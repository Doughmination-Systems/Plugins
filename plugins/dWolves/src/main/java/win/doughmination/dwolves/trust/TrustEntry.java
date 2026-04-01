package win.doughmination.dwolves.trust;

import java.util.UUID;

public class TrustEntry {

    private final UUID ownerUuid;
    private final UUID trustedUuid;
    private TrustLevel level;

    public TrustEntry(UUID ownerUuid, UUID trustedUuid, TrustLevel level) {
        this.ownerUuid  = ownerUuid;
        this.trustedUuid = trustedUuid;
        this.level      = level;
    }

    public UUID getOwnerUuid()   { return ownerUuid; }
    public UUID getTrustedUuid() { return trustedUuid; }
    public TrustLevel getLevel() { return level; }

    public void setLevel(TrustLevel level) { this.level = level; }
}