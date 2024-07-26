import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RequestGenerator {

    private static final Random random = new Random();
    private static final String[] REQ_TYPES = {"WF", "NWF", "UF"};
    private static final String[] REQ_SYSTEMS = {"StratsRecallGUI", "TradeFlow", "RiskManager"};
    private static final String[] REQ_STATES = {"NEW", "PROCESSING", "COMPLETED", "FAILED"};
    private static final String[] TARGET_SYSTEMS = {"MLClear", "Merlin"};
    private static final String[] FTP_TYPES = {"SOD", "EOD", "INTRADAY"};
    private static final String[] FUNDS = {"JUMPTP_SWAP3", "HEDGE_FUND_A", "EQUITY_FUND_B"};
    private static final String[] TICKERS = {"AAPL", "GOOGL", "MSFT", "AMZN", "FB"};

    public static List<Request> generateRequests(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> generateRequest())
                .collect(Collectors.toList());
    }

    public static Request generateRequest() {
        Request request = new Request();
        request.setReqId(UUID.randomUUID().toString());
        request.setReqType(getRandomElement(REQ_TYPES));
        request.setReqSystem(getRandomElement(REQ_SYSTEMS));
        request.setReqTimeStamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        request.setReqState(getRandomElement(REQ_STATES));
        request.setDestinations(generateDestinations());
        return request;
    }

    private static List<Destination> generateDestinations() {
        List<Destination> destinations = new ArrayList<>();
        int destCount = random.nextInt(3) + 1; // 1 to 3 destinations
        for (int i = 0; i < destCount; i++) {
            destinations.add(generateDestination());
        }
        return destinations;
    }

    private static Destination generateDestination() {
        String targetSystem = getRandomElement(TARGET_SYSTEMS);
        if ("MLClear".equals(targetSystem)) {
            return generateMLClearDestination();
        } else {
            return generateMerlinDestination();
        }
    }

    private static MLClearDestination generateMLClearDestination() {
        MLClearDestination dest = new MLClearDestination();
        dest.setTargetSystem("MLClear");
        dest.setType("File");
        dest.setFtpType(getRandomElement(FTP_TYPES));
        dest.setFileName("File_" + UUID.randomUUID().toString().substring(0, 8) + ".csv");
        dest.setFtpPath("/jumpftp/outgoing");
        dest.setContent(generateContent());
        dest.setTicketIds(generateTicketIds());
        return dest;
    }

    private static MerlinDestination generateMerlinDestination() {
        MerlinDestination dest = new MerlinDestination();
        dest.setTargetSystem("Merlin");
        dest.setType("Ticket");
        dest.setTicketIds(generateTicketIds());
        return dest;
    }

    private static List<Content> generateContent() {
        List<Content> contentList = new ArrayList<>();
        int contentCount = random.nextInt(3) + 1; // 1 to 3 content items
        for (int i = 0; i < contentCount; i++) {
            Content content = new Content();
            content.add("Fund", getRandomElement(FUNDS));
            content.add("Ticker", getRandomElement(TICKERS));
            content.add("Quantity", random.nextInt(10000) + 1);
            content.add("Price", random.nextDouble() * 1000);
            contentList.add(content);
        }
        return contentList;
    }

    private static List<String> generateTicketIds() {
        return IntStream.range(0, random.nextInt(3) + 1)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
    }

    private static <T> T getRandomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }

    public static void main(String[] args) {
        List<Request> requests = generateRequests(3);
        requests.forEach(request -> {
            System.out.println("Request ID: " + request.getReqId());
            System.out.println("Request Type: " + request.getReqType());
            System.out.println("Request System: " + request.getReqSystem());
            System.out.println("Request Timestamp: " + request.getReqTimeStamp());
            System.out.println("Request State: " + request.getReqState());
            System.out.println("Destinations: ");
            request.getDestinations().forEach(destination -> {
                System.out.println("  Target System: " + destination.getTargetSystem());
                if (destination instanceof MLClearDestination) {
                    MLClearDestination mlClear = (MLClearDestination) destination;
                    System.out.println("  FTP Type: " + mlClear.getFtpType());
                    System.out.println("  File Name: " + mlClear.getFileName());
                    System.out.println("  Content: " + mlClear.getContent());
                }
                System.out.println("  Ticket IDs: " + destination.getTicketIds());
            });
            System.out.println();
        });
    }
}