package tftp.udp.server;

import tftp.core.Configuration;
import tftp.core.TFTPException;
import tftp.core.packet.ReadRequestPacket;
import tftp.core.packet.TFTPPacket;
import tftp.core.packet.WriteRequestPacket;
import tftp.udp.UDPUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main class, running a Trivial File Transfer server.
 */
public class TFTPUDPServer extends Thread {

    /**
     * The port to run this TFTP server on.
     */
    private final int port;

    /**
     * An executor for supporting multiple TFTP clients. When a RRQ or WRQ
     * is received, a job is submitted to this executor to respond to the request
     * asynchronously.
     */
    private final ExecutorService executor;

    /**
     * Creates a new TFTP server thread, to run on the given port.
     *
     * @param port the port to run the server on
     */
    public TFTPUDPServer(int port) {
        this.port = port;
        this.executor = Executors.newCachedThreadPool();
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
                    System.out.println("error receiving packet: " + e);
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
                            System.out.println("received packet " + packet + ", ignoring");
                            break;
                    }

                } catch (TFTPException e) {
                    System.out.println("error parsing received packet: " + e);
                }
            }

        } catch (SocketException e) {
            System.out.println("failed to start server: " + e);
        }
    }

    /**
     * The entry point of the program.
     *
     * @param args the program arguments, separated by spaces
     */
    public static void main(String[] args) {
        int port = Configuration.DEFAULT_SERVER_PORT;

        //parse the optional arguments
        for (int i = 0; i < args.length - 1; ++i) {
            if (args[i].equals("-port")) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException nfe) {
                    System.out.println("invalid port: " + args[i + 1]);
                    return;
                }
            } else if (args[i].equals("-timeout")) {
                try {
                    Configuration.TIMEOUT = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException nfe) {
                    System.out.println("invalid timeout: " + args[i + 1]);
                    return;
                }
            }
        }

        //run the server, passing the port as an argument
        TFTPUDPServer server = new TFTPUDPServer(port);
        server.start();
    }

}
