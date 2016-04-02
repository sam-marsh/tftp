package tftp.core;

/**
 * @author Sam Marsh
 */
public class Configuration {

    public static final int MAX_DATA_LENGTH = 512;
    public static final int MAX_PACKET_LENGTH = MAX_DATA_LENGTH + 4;

    public static final int TIMEOUT = 3000;

    public static final int DEFAULT_SERVER_PORT = 69;

}
