package tftp.udp.client;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.FileReceiver;
import tftp.udp.UDPUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Paths;

/**
 * Initiates a read request (RRQ) to the specified TFTP server and then, if accepted, reads a file
 * from the server and writes it to disk.
 */
public class ClientReceiver implements Runnable {

    /**
     * The address of the TFTP server.
     */
    private InetAddress serverAddress;

    /**
     * The port of the TFTP server (will change after the first request).
     */
    private int serverPort;

    /**
     * The path of the file on the TFTP server to read from.
     */
    private final String remotePath;

    /**
     * The location on disk to write the received file to.
     */
    private final String localPath;

    /**
     * Creates a new handler for reading a file from a TFTP server.
     *
     * @param serverAddress the address of the TFTP server
     * @param serverPort the port of the TFTP server
     * @param remotePath the path of the file on the TFTP server to read from
     * @param localPath the location on disk to write the received file to (if null, then the file will be
     *                  named the same as the filename on the remote server, and saved in the current
     *                  working directory)
     */
    public ClientReceiver(InetAddress serverAddress, int serverPort, String remotePath, String localPath) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.remotePath = remotePath;
        this.localPath = (localPath == null ? Paths.get(remotePath).getFileName().toString() : localPath);
    }

    /**
     * Initiates the transfer from the server.
     */
    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            //open an output stream to the local file
            try (FileOutputStream fos = new FileOutputStream(localPath)) {

                //receive the file from the server, specifying the first packet in the 'communication' to be
                // a read request packet
                FileReceiver.receive(
                        socket,
                        new ReadRequestPacket(remotePath, Mode.OCTET),
                        serverAddress,
                        serverPort,
                        fos
                );

            } catch (FileNotFoundException fnfe) {
                //file not found exception occurs "if the file exists but is a directory rather than a regular file,
                // does not exist but cannot be created, or cannot be opened for any other reason"
                System.out.println("unable to write to: " + localPath);
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "unable to write to: " + localPath
                );
                DatagramPacket datagram = UDPUtil.toDatagram(errorPacket, serverAddress, serverPort);
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

}
