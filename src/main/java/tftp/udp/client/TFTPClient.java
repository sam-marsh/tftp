package tftp.udp.client;

import tftp.core.Mode;

import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Sam Marsh
 */
public class TFTPClient {

    public static void main(String[] args) throws IOException {
        InetAddress server = InetAddress.getByName("127.0.0.1");
        int port = 60009;

        Runnable handler = new WRQHandler(server, port, "/Users/Sam/.bash_profile", "bash_profile", Mode.OCTET);
        handler.run();
    }

}
