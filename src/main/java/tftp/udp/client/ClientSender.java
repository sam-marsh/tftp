package tftp.udp.client;

import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.FileSender;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.Paths;

/**
 * Initiates a write request (WRQ) to the specified TFTP server and then, if accepted, sends a file
 * to the server.
 */
public class ClientSender implements Runnable {

    /**
     * The address of the TFTP server.
     */
    private InetAddress serverAddress;

    /**
     * The port of the TFTP server (will change after the first request).
     */
    private int serverPort;

    /**
     * The location on disk to read the file from.
     */
    private String localFile;

    /**
     * The path on the remote server to write to.
     */
    private String remoteFile;

    /**
     * Creates a new handler for writing a file to a TFTP server.
     *
     * @param serverAddress the address of the TFTP server
     * @param serverPort the port of the TFTP server
     * @param localFile the path of the file to read from
     * @param remoteFile the remote path to send to the server - if null, this is simply the filename of the
     *                   localFile path.
     */
    public ClientSender(InetAddress serverAddress, int serverPort, String localFile, String remoteFile) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.localFile = localFile;
        this.remoteFile = (remoteFile == null ? Paths.get(localFile).getFileName().toString() : remoteFile);
    }

    /**
     * Initiates the transfer to the server.
     */
    @Override
    public void run() {
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
                        serverAddress,
                        serverPort,
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

}
