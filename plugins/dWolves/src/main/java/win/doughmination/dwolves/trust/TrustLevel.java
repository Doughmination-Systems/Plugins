package win.doughmination.dwolves.trust;

/**
 * Trust levels for wolf access.
 *
 * BASIC — sit, stand, follow, teleport
 * FULL  — all of the above + rename
 */
public enum TrustLevel {
    BASIC,
    FULL;

    public static TrustLevel fromString(String s) {
        return switch (s.toLowerCase()) {
            case "full" -> FULL;
            default -> BASIC;
        };
    }

    public String displayName() {
        return switch (this) {
            case BASIC -> "Basic";
            case FULL  -> "Full";
        };
    }

    /** Returns true if this level includes rename permission. */
    public boolean canRename() {
        return this == FULL;
    }
}