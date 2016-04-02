package tftp.udp.server;

import tftp.core.Configuration;
import tftp.core.TFTPException;
import tftp.core.packet.ReadRequestPacket;
import tftp.core.packet.TFTPPacket;
import tftp.core.packet.WriteRequestPacket;
import tftp.udp.util.UDPUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * The main class, running a Trivial File Transfer server.
 */
public class TFTPServer extends Thread {

    /**
     * The logging mechanism for this class.
     */
    private static final Logger LOG = Logger.getLogger(TFTPServer.class.getSimpleName());

    /**
     * The port to run this TFTP server on.
     */
    private final int port;

    /**
     * An executor for supporting multiple TFTP clients. When a RRQ or WRQ
     * is received, a job is submitted to this executor to respond to the request
     * asynchronously.
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Creates a new TFTP server thread, to run on the given port.
     *
     * @param port the port to run the server on
     */
    public TFTPServer(int port) {
        this.port = port;
    }

    /**
     * Called when the thread is started.
     */
    @Override
    public void run() {
        try {
            //create a new datagram socket and bind to the given port
            DatagramSocket socket = new DatagramSocket(port);

            //allocate a buffer for holding received datagrams
            byte[] buffer = new byte[Configuration.MAX_PACKET_LENGTH];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            //loop forever until forcibly stopped
            while (true) {

                try {
                    //receive a datagram packet from the network - this method blocks
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    LOG.warning("error receiving packet: " + e);
                    continue;
                }

                try {
                    //extract the TFTP packet from the datagram
                    TFTPPacket packet = UDPUtil.fromDatagram(receivePacket);

                    //if the packet is a RRQ or WRQ, submit a job to the executor
                    // to respond to the client, otherwise ignore.
                    switch (packet.getPacketType()) {
                        case READ_REQUEST:
                            executor.submit(new RRQHandler(
                                    receivePacket.getAddress(),
                                    receivePacket.getPort(),
                                    (ReadRequestPacket) packet
                            ));
                            break;
                        case WRITE_REQUEST:
                            executor.submit(new WRQHandler(
                                    receivePacket.getAddress(),
                                    receivePacket.getPort(),
                                    (WriteRequestPacket) packet
                            ));
                            break;
                        default:
                            LOG.warning("received packet " + packet + ", ignoring");
                            break;
                    }

                } catch (TFTPException e) {
                    LOG.warning("error parsing received packet: " + e);
                }
            }

        } catch (SocketException e) {
            LOG.severe("failed to start server: " + e);
        }
    }

    /**
     * The entry point of the program.
     *
     * @param args the program arguments, separated by spaces
     */
    public static void main(String[] args) {
        TFTPServer server = new TFTPServer(Configuration.DEFAULT_SERVER_PORT);
        server.start();
    }

}
