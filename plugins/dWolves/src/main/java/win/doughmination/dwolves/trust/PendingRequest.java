package win.doughmination.dwolves.trust;

import java.util.UUID;

public class PendingRequest {

    private final UUID ownerUuid;
    private final String ownerName;
    private final UUID targetUuid;
    private final TrustLevel level;
    private final long createdAt;

    /** Requests expire after 60 seconds. */
    public static final long EXPIRY_MS = 60_000L;

    public PendingRequest(UUID ownerUuid, String ownerName, UUID targetUuid, TrustLevel level) {
        this.ownerUuid  = ownerUuid;
        this.ownerName  = ownerName;
        this.targetUuid = targetUuid;
        this.level      = level;
        this.createdAt  = System.currentTimeMillis();
    }

    public UUID getOwnerUuid()   { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public UUID getTargetUuid()  { return targetUuid; }
    public TrustLevel getLevel() { return level; }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > EXPIRY_MS;
    }
}