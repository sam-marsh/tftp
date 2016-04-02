package tftp.udp.server;

import tftp.core.Configuration;
import tftp.core.TFTPException;
import tftp.core.packet.TFTPPacket;
import tftp.udp.util.UDPUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * @author Sam Marsh
 */
public class TFTPServer extends Thread {

    private static final Logger LOG = Logger.getLogger(TFTPServer.class.getSimpleName());

    private final int port;

    public TFTPServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(port);
            socket.bind(new InetSocketAddress("127.0.0.1", port));

            byte[] buffer = new byte[Configuration.MAX_PACKET_LENGTH];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            while (true) {
                try {
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    LOG.warning("error receiving packet: " + e);
                    continue;
                }

                try {
                    TFTPPacket packet = UDPUtil.fromDatagram(receivePacket);

                    switch (packet.getPacketType()) {
                        case READ_REQUEST:
                            break;
                        case WRITE_REQUEST:
                            break;
                        default:
                            System.err.println();
                    }

                } catch (TFTPException e) {
                    LOG.warning("exception parsing received packet: " + e);
                }
            }

        } catch (SocketException e) {
            LOG.severe("failed to start server: " + e);
        }
    }

    public static void main(String[] args) {
        TFTPServer server = new TFTPServer(Configuration.DEFAULT_SERVER_PORT);
        server.start();
    }

}
