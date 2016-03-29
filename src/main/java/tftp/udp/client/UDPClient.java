package tftp.udp.client;

import tftp.core.Mode;
import tftp.core.packet.ReadRequestPacket;

import java.net.DatagramSocket;

/**
 * @author Sam Marsh
 */
public class UDPClient implements Runnable {

    @Override
    public void run() {
        ReadRequestPacket packet = new ReadRequestPacket("~/.bash_profile", Mode.OCTET);
    }
}
