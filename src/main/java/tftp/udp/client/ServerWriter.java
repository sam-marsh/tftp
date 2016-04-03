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
 * Handles responses to read requests from clients.
 */
public class ServerWriter implements Runnable {

    private InetAddress clientAddress;
    private int clientPort;
    private String localFile;
    private String remoteFile;
    private Mode mode;

    public ServerWriter(InetAddress clientAddress, int clientPort, String localFile, Mode mode) {
        this(clientAddress, clientPort, localFile, null, mode);
    }

    public ServerWriter(InetAddress clientAddress, int clientPort, String localFile, String remoteFile, Mode mode) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.localFile = localFile;
        this.remoteFile = (remoteFile == null ? Paths.get(localFile).getFileName().toString() : remoteFile);
        this.mode = mode;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            if (mode != Mode.OCTET) {
                System.out.println("unsupported mode: " + mode);
                return;
            }

            try (FileInputStream fis = new FileInputStream(localFile)) {

                FileSender.send(
                        socket,
                        new WriteRequestPacket(remoteFile, Mode.OCTET),
                        clientAddress,
                        clientPort,
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
