package tftp.core;

/**
 * Provides the default configuration of variables for this implementation of the trivial file transfer protocol.
 */
public class Configuration {

    /**
     * The max data length of a data packet is 512 as specified in the RFC.
     */
    public static final int MAX_DATA_LENGTH = 512;

    /**
     * This constant is used for allocating buffer space. It is assumed that the largest packet that could be received
     * is of length 516 - a data packet holding 512 bytes of data, plus the 4-byte header.
     */
    public static final int MAX_PACKET_LENGTH = MAX_DATA_LENGTH + 4;

    /**
     * The default (initial) server port. In the client, if no port is specified, this will be used as the assumed
     * port of the TFTP server. In the server, if no port as specified, the server will bind to this port.
     */
    public static final int DEFAULT_SERVER_PORT = 6009;

    /**
     * The maximum number of timeouts (in a row) to occur before the transfer 'gives up'.
     */
    public static final int MAX_TIMEOUTS = 5;

    /**
     * The maximum number of 'nonsensical' packets to receive before giving up.
     */
    public static final int MAX_INVALIDS = 5;

    /**
     * The initial default timeout length before retransmitting the previous packet.
     */
    public static int TIMEOUT = 3000;

}
