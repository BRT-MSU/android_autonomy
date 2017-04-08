package nasa_rmc.autonomy.network.message;

/**
 * Created by atomlinson on 4/3/17.
 */

public enum ForwardingPrefix {
    CLIENT("-c"),
    TANGO("-t"),
    MOTOR("-m"),
    DEBUG("-d"),
    STATUS("-s");

    private String forwardingPrefix;

    ForwardingPrefix(String forwardingPrefix) {
        this.forwardingPrefix = forwardingPrefix;
    }

    @Override
    public String toString() {
        return forwardingPrefix;
    }
}
