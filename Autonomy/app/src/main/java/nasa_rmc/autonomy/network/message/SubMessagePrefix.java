package nasa_rmc.autonomy.network.message;

/**
 * Created by atomlinson on 4/3/17.
 */

public enum SubMessagePrefix {
    LEFT_MOTOR("l"),
    RIGHT_MOTOR("r"),
    ACTUATOR("a"),
    BUCKET("b"),
    SERVO("s");

    private String subMessagePrefix;

    SubMessagePrefix(String subMessagePrefix) {
        this.subMessagePrefix = subMessagePrefix;
    }

    @Override
    public String toString() {
        return subMessagePrefix;
    }
}
