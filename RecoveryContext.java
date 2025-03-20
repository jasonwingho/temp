package com.a.g.recall.recovery;

import com.a.g.recall.cache.TransactionLogEntry;
import com.a.g.recall.entity.RecallTicket;
import com.a.g.recall.objects.ExecutionReport;
import com.a.g.recall.objects.Order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Context object containing all data needed for recovery decisions.
 * <p>
 * This class encapsulates the state information from various message topics
 * that is used during the recovery process to determine the appropriate
 * recovery action for an order.
 */
public class RecoveryContext {
    /** The ID of the order being recovered */
    private final String orderId;
    
    /** The recall ticket from RECALL/TICKET/HISTORY */
    private final RecallTicket ticket;
    
    /** The state from RECALL/TICKET/HISTORY */
    private final String historyState;
    
    /** The state from RECALL/TO/OMS */
    private final String omsRequestState;
    
    /** The state from OMS/TO/RECALL */
    private final String omsResponseState;
    
    /** The timestamp from RECALL/TICKET/HISTORY */
    private final Instant historyTimestamp;
    
    /** The timestamp from RECALL/TO/OMS */
    private final Instant requestTimestamp;
    
    /** The timestamp from OMS/TO/RECALL */
    private final Instant responseTimestamp;
    
    /** The latest transaction log entry from RECALL/TICKET/HISTORY */
    private final TransactionLogEntry ticketEntry;
    
    /** The latest transaction log entry from RECALL/TO/OMS */
    private final TransactionLogEntry requestEntry;
    
    /** The latest transaction log entry from OMS/TO/RECALL */
    private final TransactionLogEntry responseEntry;
    
    /** Complete list of transaction log entries for this order */
    private final List<TransactionLogEntry> allLogEntries;
    
    /**
     * Creates a new RecoveryContext from transaction log entries.
     *
     * @param orderId The order ID
     * @param ticketEntry The transaction log entry from RECALL/TICKET/HISTORY
     * @param requestEntry The transaction log entry from RECALL/TO/OMS
     * @param responseEntry The transaction log entry from OMS/TO/RECALL
     * @param allLogEntries The complete list of transaction log entries for this order
     */
    public RecoveryContext(String orderId, 
                          TransactionLogEntry ticketEntry,
                          TransactionLogEntry requestEntry, 
                          TransactionLogEntry responseEntry,
                          List<TransactionLogEntry> allLogEntries) {
        this.orderId = orderId;
        this.ticketEntry = ticketEntry;
        this.requestEntry = requestEntry;
        this.responseEntry = responseEntry;
        this.allLogEntries = allLogEntries != null ? 
            new ArrayList<>(allLogEntries) : new ArrayList<>();

        // Process ticket history entry (required)
        this.ticket = ticketEntry != null ? ticketEntry.getMessageAs(RecallTicket.class) : null;
        this.historyState = ticketEntry != null ? ticketEntry.getState() : null;
        this.historyTimestamp = ticketEntry != null ? ticketEntry.getTimestamp() : null;
        
        // Process OMS request entry (optional)
        if (requestEntry != null) {
            this.omsRequestState = requestEntry.getState();
            this.requestTimestamp = requestEntry.getTimestamp();
        } else {
            this.omsRequestState = null;
            this.requestTimestamp = null;
        }
        
        // Process OMS response entry (optional)
        if (responseEntry != null) {
            this.omsResponseState = responseEntry.getState();
            this.responseTimestamp = responseEntry.getTimestamp();
        } else {
            this.omsResponseState = null;
            this.responseTimestamp = null;
        }
    }
    
    /**
     * Gets the order ID.
     *
     * @return The order ID
     */
    public String getOrderId() { 
        return orderId; 
    }
    
    /**
     * Gets the recall ticket.
     *
     * @return The recall ticket
     */
    public RecallTicket getTicket() { 
        return ticket; 
    }
    
    /**
     * Gets the state from RECALL/TICKET/HISTORY.
     *
     * @return The history state
     */
    public String getHistoryState() { 
        return historyState; 
    }
    
    /**
     * Gets the state from RECALL/TO/OMS.
     *
     * @return The OMS request state, or null if not available
     */
    public String getOmsRequestState() { 
        return omsRequestState; 
    }
    
    /**
     * Gets the state from OMS/TO/RECALL.
     *
     * @return The OMS response state, or null if not available
     */
    public String getOmsResponseState() { 
        return omsResponseState; 
    }
    
    /**
     * Gets the timestamp from RECALL/TICKET/HISTORY.
     *
     * @return The history timestamp
     */
    public Instant getHistoryTimestamp() { 
        return historyTimestamp; 
    }
    
    /**
     * Gets the timestamp from RECALL/TO/OMS.
     *
     * @return The request timestamp, or null if not available
     */
    public Instant getRequestTimestamp() { 
        return requestTimestamp; 
    }
    
    /**
     * Gets the timestamp from OMS/TO/RECALL.
     *
     * @return The response timestamp, or null if not available
     */
    public Instant getResponseTimestamp() { 
        return responseTimestamp; 
    }

    /**
     * Gets the latest transaction log entry from RECALL/TICKET/HISTORY.
     *
     * @return The ticket entry
     */
    public TransactionLogEntry getTicketEntry() {
        return ticketEntry;
    }

    /**
     * Gets the latest transaction log entry from RECALL/TO/OMS.
     *
     * @return The request entry
     */
    public TransactionLogEntry getRequestEntry() {
        return requestEntry;
    }

    /**
     * Gets the latest transaction log entry from OMS/TO/RECALL.
     *
     * @return The response entry
     */
    public TransactionLogEntry getResponseEntry() {
        return responseEntry;
    }
    
    /**
     * Gets all transaction log entries for this order.
     *
     * @return The complete list of transaction log entries
     */
    public List<TransactionLogEntry> getAllLogEntries() {
        return new ArrayList<>(allLogEntries);
    }
    
    /**
     * Gets all transaction log entries from a specific source.
     *
     * @param source The source to filter by
     * @return The list of entries from the specified source
     */
    public List<TransactionLogEntry> getLogEntriesFromSource(String source) {
        return allLogEntries.stream()
            .filter(entry -> entry.isFromSource(source))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all transaction log entries ordered by timestamp.
     *
     * @return The list of entries ordered by timestamp
     */
    public List<TransactionLogEntry> getLogEntriesOrderedByTimestamp() {
        List<TransactionLogEntry> orderedEntries = new ArrayList<>(allLogEntries);
        orderedEntries.sort(Comparator.comparing(TransactionLogEntry::getTimestamp));
        return orderedEntries;
    }
    
    /**
     * Checks if the OMS request state matches the history state.
     *
     * @return true if the states match, false otherwise
     */
    public boolean omsRequestStateMatchesHistory() {
        return omsRequestState != null && omsRequestState.equals(historyState);
    }
    
    /**
     * Checks if the OMS response state matches the history state.
     *
     * @return true if the states match, false otherwise
     */
    public boolean omsResponseStateMatchesHistory() {
        return omsResponseState != null && omsResponseState.equals(historyState);
    }
    
    /**
     * Checks if the OMS response is newer than the OMS request.
     *
     * @return true if the response is newer, false otherwise
     */
    public boolean isResponseNewerThanRequest() {
        return responseTimestamp != null && requestTimestamp != null &&
               responseTimestamp.isAfter(requestTimestamp);
    }

    /**
     * Gets the order from the request entry.
     *
     * @return The order or null if not available
     */
    public Order getRequestOrder() {
        if (requestEntry != null) {
            return requestEntry.getMessageAs(Order.class);
        }
        return null;
    }

    /**
     * Gets the execution report from the request entry.
     *
     * @return The execution report or null if not available
     */
    public ExecutionReport getRequestExecutionReport() {
        Order order = getRequestOrder();
        if (order != null) {
            return order.getFillRequest();
        } 
        return null;
    }
    
    /**
     * Gets the execution report from the response entry.
     *
     * @return The execution report or null if not available
     */
    public ExecutionReport getResponseExecutionReport() {
        if (responseEntry != null) {
            return responseEntry.getMessageAs(ExecutionReport.class);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "RecoveryContext{" +
               "orderId='" + orderId + '\'' +
               ", historyState='" + historyState + '\'' +
               ", omsRequestState='" + omsRequestState + '\'' +
               ", omsResponseState='" + omsResponseState + '\'' +
               ", historyTimestamp=" + historyTimestamp +
               ", requestTimestamp=" + requestTimestamp +
               ", responseTimestamp=" + responseTimestamp +
               ", logEntryCount=" + allLogEntries.size() +
               '}';
    }
}
