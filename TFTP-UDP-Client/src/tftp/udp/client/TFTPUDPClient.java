package tftp.udp.client;

import tftp.GenericTFTPClient;
import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.ErrorPacket;
import tftp.core.packet.ReadRequestPacket;
import tftp.core.packet.WriteRequestPacket;
import tftp.udp.FileReceiver;
import tftp.udp.FileSender;
import tftp.udp.UDPUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

/**
 * A client for sending/receiving files from a server using the Trivial File Transfer Protocol.
 * Uses octet mode for all transfers.
 */
public class TFTPUDPClient extends GenericTFTPClient {

    /**
     * Creates a new TFTP client.
     */
    public TFTPUDPClient(int port) {
        super(port);
    }

    /**
     * Receives a file from the server using TFTP over UDP.
     *
     * @param remoteFile the path of the file on the server
     * @param localFile the path of the file on the local machine
     */
    @Override
    public void get(String remoteFile, String localFile) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            //open an output stream to the local file
            try (FileOutputStream fos = new FileOutputStream(localFile)) {

                //receive the file from the server, specifying the first packet in the 'communication' to be
                // a read request packet
                FileReceiver.receive(
                        socket,
                        new ReadRequestPacket(remoteFile, Mode.OCTET),
                        remoteAddress,
                        remotePort,
                        fos
                );

            } catch (FileNotFoundException fnfe) {
                //file not found exception occurs "if the file exists but is a directory rather than a regular file,
                // does not exist but cannot be created, or cannot be opened for any other reason"
                System.out.println("unable to write to: " + localFile);
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "unable to write to: " + localFile
                );
                DatagramPacket datagram = UDPUtil.toDatagram(errorPacket, remoteAddress, remotePort);
                //send an error packet to the server if this happens
                socket.send(datagram);
            }

        } catch (SocketException e) {
            System.out.println("error: socket could not be opened");
        } catch (TFTPException e) {
            System.out.println(e.getMessage());
        } catch (IOException ignore) {
            //only reaches here if unable to send error packet, but already printed error message by this time
        }
    }

    /**
     * Sends a file to the server using the TFTP protocol over UDP.
     *
     * @param localFile the path of the file on the local machine
     * @param remoteFile the path of the file on the server
     */
    @Override
    public void put(String localFile, String remoteFile) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            //open an input stream to read from the given file
            try (FileInputStream fis = new FileInputStream(localFile)) {

                //send the file to the server, specifying the first packet in the 'communication' to be
                // a write request packet
                FileSender.send(
                        socket,
                        new WriteRequestPacket(remoteFile, Mode.OCTET),
                        remoteAddress,
                        remotePort,
                        fis,
                        (short) 0
                );

            } catch (FileNotFoundException e) {
                System.out.println("file not found: " + localFile);
            } catch (TFTPException e) {
                System.out.println(e.getMessage());
            }

        } catch (SocketException e) {
            System.out.println("error: socket could not be opened");
        } catch (IOException e) {
            System.out.println("error closing file input stream");
        }
    }

    /**
     * The entry point of this TFTP client program.
     *
     * @param args the user arguments
     */
    public static void main(String[] args) {
        Thread client = new TFTPUDPClient(Configuration.DEFAULT_SERVER_PORT);
        client.start();
    }

}
