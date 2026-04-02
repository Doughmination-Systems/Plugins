package win.doughmination.plural;

// Static plugin configuration — replaces config.yml, as data does not need to be changed

public final class PluralConfig {

    private PluralConfig() {}

    public static final String API_URL = System.getProperty(
            "PLURAL_API_URL",
            "https://plural.doughmination.win"
    );

    public static final boolean LUCKPERMS_PREFIX = Boolean.parseBoolean(
            System.getProperty("PLURAL_LUCKPERMS_PREFIX", "true")
    );
}
