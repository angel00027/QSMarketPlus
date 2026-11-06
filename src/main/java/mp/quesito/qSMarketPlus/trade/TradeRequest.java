package mp.quesito.qSMarketPlus.trade;

import java.util.UUID;

public class TradeRequest {

    private final UUID from;
    private final UUID to;
    private final long createdAt;

    public TradeRequest(UUID from, UUID to) {
        this.from = from;
        this.to = to;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getFrom() {
        return from;
    }

    public UUID getTo() {
        return to;
    }

    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - createdAt > timeoutMs;
    }
}