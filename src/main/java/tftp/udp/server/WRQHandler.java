package tftp.udp.server;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.FileReceiver;
import tftp.udp.UDPUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * A 'handler' for reading a file from a client (WRQ) and writing it to a disk. That is, this class is the
 * responder to write requests from the client.
 */
public class WRQHandler implements Runnable {

    /**
     * The address of the client.
     */
    private InetAddress clientAddress;

    /**
     * The port used for communication with the client.
     */
    private int clientPort;

    /**
     * The write request packet received initially from the client, which initiated this transfer.
     */
    private final WriteRequestPacket wrq;

    /**
     * Creates a new handler for responding to a write request from a given client.
     *
     * @param clientAddress the address of the client which sent the WRQ
     * @param clientPort the port of the client which sent the WRQ
     * @param wrq the write request received from the client
     */
    public WRQHandler(InetAddress clientAddress, int clientPort, WriteRequestPacket wrq) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.wrq = wrq;
    }

    /**
     * Starts the response and transfer. This is executed asynchronously by the server.
     */
    @Override
    public void run() {
        System.out.println("responding to request: " + wrq + " from client: " + clientAddress + ":" + clientPort);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            //open output stream to the file specified in the write request
            try (FileOutputStream fos = new FileOutputStream(wrq.getFileName())) {

                //receive the file from the client, specifying the first packet to be
                // acknowledging packet 0 as specified in the RFC
                FileReceiver.receive(
                        socket,
                        new AcknowledgementPacket((short) 0),
                        clientAddress,
                        clientPort,
                        fos
                );

            } catch (FileNotFoundException fnfe) {
                //some sort of error occurred in writing to the file, print a message and send that
                // same message to the client in an error packet
                System.out.println("unable to write to: " + wrq.getFileName());
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "unable to write to: " + wrq.getFileName()
                );
                DatagramPacket datagram = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                socket.send(datagram);
            } catch (TFTPException e) {
                //an error occurred in receiving the file, just print an error and end this handler
                System.out.println(e.getMessage());
            }

        } catch (IOException e) {
            //couldn't even open a socket - give up
            // also could happen if the output stream failed to close, but that doesn't really matter
            System.out.println("failed to receive: " + e.getMessage());
        }
    }

}
