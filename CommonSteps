package com.a.gdot.recall.recovery.steps;

import com.a.gdot.recall.ThreadSafeCache;
import com.a.gdot.recall.amps.HAAmpsClient;
import com.a.gdot.recall.cache.TransactionLogEntry;
import com.a.gdot.recall.entity.RecallTicket;
import com.a.gdot.recall.objects.ExecutionReport;
import com.a.gdot.recall.objects.Order;
import com.a.gdot.recall.recovery.RecoveryContext;
import com.a.gdot.recall.recovery.RecoveryActionType;
import com.a.gdot.recall.service.RecoveryService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CommonSteps {

    @Autowired
    private ThreadSafeCache mockCache;
    
    @Autowired
    private HAAmpsClient mockAmpsClient;
    
    @Autowired
    private RecoveryService recoveryService;
    
    private Map<String, TransactionLogEntry> ticketHistoryEntries = new HashMap<>();
    private Map<String, TransactionLogEntry> omsRequestEntries = new HashMap<>();
    private Map<String, TransactionLogEntry> omsResponseEntries = new HashMap<>();
    
    private Map<String, RecoveryContext> recoveryContexts = new HashMap<>();
    private Map<String, RecoveryActionType> recoveryActions = new HashMap<>();
    
    @Before
    public void setUp() {
        reset(mockCache, mockAmpsClient);
    }
    
    @Given("the system is in recovery mode")
    public void theSystemIsInRecoveryMode() {
        // Nothing needed here, just establishes scenario context
    }
    
    @Given("the transaction logs are available")
    public void theTransactionLogsAreAvailable() {
        // Clear test data
        ticketHistoryEntries.clear();
        omsRequestEntries.clear();
        omsResponseEntries.clear();
        recoveryContexts.clear();
        recoveryActions.clear();
    }
    
    @Given("an order with ID {string} exists in transaction logs")
    public void anOrderWithIDExistsInTransactionLogs(String orderId) {
        // Create a basic RecallTicket for this order
        RecallTicket ticket = new RecallTicket();
        ticket.setId(orderId);
        ticket.setReqId(UUID.randomUUID().toString());
        ticket.setEntryTime(ZonedDateTime.now(ZoneId.of("UTC")));
        
        // Create a basic transaction log entry
        TransactionLogEntry entry = createTransactionLogEntry(
            orderId, 
            "RECALL/TICKET/HISTORY", 
            "Created", 
            Instant.now(), 
            ticket,
            100L,  // recallQty
            0L,    // fillQty
            0.0    // fillPrice
        );
        
        ticketHistoryEntries.put(orderId, entry);
    }
    
    @Given("the order has state {string} in RECALL\\/TICKET\\/HISTORY topic")
    public void theOrderHasStateInHistoryTopic(String state) {
        // Update the state in the existing entry
        for (Map.Entry<String, TransactionLogEntry> entry : ticketHistoryEntries.entrySet()) {
            String orderId = entry.getKey();
            TransactionLogEntry oldEntry = entry.getValue();
            RecallTicket ticket = oldEntry.getMessageAs(RecallTicket.class);
            ticket.setCurrentState(state);
            
            TransactionLogEntry newEntry = createTransactionLogEntry(
                orderId,
                "RECALL/TICKET/HISTORY",
                state,
                Instant.now(),
                ticket,
                ticket.getRecallQty(),
                ticket.getFillQty(),
                ticket.getFillPrice()
            );
            
            ticketHistoryEntries.put(orderId, newEntry);
        }
    }
    
    @Given("the order has state {string} in RECALL\\/TO\\/OMS topic")
    public void theOrderHasStateInOmsRequestTopic(String state) {
        for (Map.Entry<String, TransactionLogEntry> entry : ticketHistoryEntries.entrySet()) {
            String orderId = entry.getKey();
            RecallTicket ticket = entry.getValue().getMessageAs(RecallTicket.class);
            
            // Create a basic Order object
            Order order = Order.createFromRecallTicket(ticket);
            order.setCurrentState(OrderState.valueOf(state));
            
            TransactionLogEntry omsEntry = createTransactionLogEntry(
                orderId,
                "RECALL/TO/OMS",
                state,
                Instant.now(),
                order,
                ticket.getRecallQty(),
                ticket.getFillQty(),
                ticket.getFillPrice()
            );
            
            omsRequestEntries.put(orderId, omsEntry);
        }
    }
    
    @Given("the order has state {string} in OMS\\/TO\\/RECALL topic")
    public void theOrderHasStateInOmsResponseTopic(String state) {
        for (Map.Entry<String, TransactionLogEntry> entry : ticketHistoryEntries.entrySet()) {
            String orderId = entry.getKey();
            RecallTicket ticket = entry.getValue().getMessageAs(RecallTicket.class);
            
            // Create a basic ExecutionReport
            ExecutionReport report = new ExecutionReport();
            report.setOrderState(state);
            report.setOrigClOrdID(orderId);
            report.setClOrdID(orderId);
            report.setExecType('0'); // Default to New
            
            TransactionLogEntry omsEntry = createTransactionLogEntry(
                orderId,
                "OMS/TO/RECALL",
                state,
                Instant.now(),
                report,
                ticket.getRecallQty(),
                ticket.getFillQty(),
                ticket.getFillPrice(),
                "exec-" + UUID.randomUUID().toString(),
                '0'
            );
            
            omsResponseEntries.put(orderId, omsEntry);
        }
    }
    
    @When("the recovery process runs for this order")
    public void theRecoveryProcessRunsForThisOrder() {
        for (Map.Entry<String, TransactionLogEntry> entry : ticketHistoryEntries.entrySet()) {
            String orderId = entry.getKey();
            TransactionLogEntry historyEntry = entry.getValue();
            TransactionLogEntry requestEntry = omsRequestEntries.get(orderId);
            TransactionLogEntry responseEntry = omsResponseEntries.get(orderId);
            
            // Create a list of all log entries for this order
            List<TransactionLogEntry> allLogEntries = new ArrayList<>();
            allLogEntries.add(historyEntry);
            if (requestEntry != null) allLogEntries.add(requestEntry);
            if (responseEntry != null) allLogEntries.add(responseEntry);
            
            RecoveryContext context = new RecoveryContext(
                orderId,
                historyEntry,
                requestEntry,
                responseEntry,
                allLogEntries
            );
            
            recoveryContexts.put(orderId, context);
            RecoveryActionType action = recoveryService.determineRecoveryAction(context);
            recoveryActions.put(orderId, action);
        }
    }
    
    @Then("the recovery action should be {word}")
    public void theRecoveryActionShouldBe(String actionType) {
        for (Map.Entry<String, RecoveryActionType> entry : recoveryActions.entrySet()) {
            assertThat(entry.getValue().name())
                .isEqualToIgnoringCase(actionType);
        }
    }
    
    // Helper method to create TransactionLogEntry with the updated constructor
    private TransactionLogEntry createTransactionLogEntry(
            String orderId, String source, String state, Instant timestamp, 
            Object message, long recallQty, long fillQty, double fillPrice) {
        return createTransactionLogEntry(orderId, source, state, timestamp, message, 
                recallQty, fillQty, fillPrice, null, null);
    }
    
    private TransactionLogEntry createTransactionLogEntry(
            String orderId, String source, String state, Instant timestamp, 
            Object message, long recallQty, long fillQty, double fillPrice,
            String executionId, Character execType) {
        
        return TransactionLogEntry.Builder.builder()
                .orderId(orderId)
                .source(source)
                .state(state)
                .timestamp(timestamp)
                .message(message)
                .recallQty(recallQty)
                .fillQty(fillQty)
                .fillPrice(fillPrice)
                .executionId(executionId)
                .execType(execType)
                .build();
    }
    
    // Getter methods for use in other steps classes
    public Map<String, RecoveryContext> getRecoveryContexts() {
        return recoveryContexts;
    }
    
    public ThreadSafeCache getMockCache() {
        return mockCache;
    }
    
    public HAAmpsClient getMockAmpsClient() {
        return mockAmpsClient;
    }
}
