package tftp.udp.client;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.FileSender;
import tftp.udp.util.UDPUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Handles responses to read requests from clients.
 */
public class WRQHandler implements Runnable {

    private final Logger log;
    private InetAddress clientAddress;
    private int clientPort;
    private String localFile;
    private String remoteFile;
    private Mode mode;

    public WRQHandler(InetAddress clientAddress, int clientPort, String localFile, Mode mode) {
        this(clientAddress, clientPort, localFile, null, mode);
    }

    public WRQHandler(InetAddress clientAddress, int clientPort, String localFile, String remoteFile, Mode mode) {
        this.log = Logger.getLogger(
                String.format("%s[%s:%d]", WRQHandler.class.getSimpleName(), clientAddress, clientPort)
        );
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.localFile = localFile;
        this.remoteFile = (remoteFile == null ? Paths.get(localFile).getFileName().toString() : remoteFile);
        this.mode = mode;
    }

    @Override
    public void run() {
        log.info("connecting to client: " + clientAddress + ":" + clientPort);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            if (mode != Mode.OCTET) {
                log.severe("unsupported mode: " + mode);
                return;
            }

            try (FileInputStream fis = new FileInputStream(localFile)) {

                FileSender.send(
                        socket,
                        new WriteRequestPacket(remoteFile, Mode.OCTET),
                        clientAddress,
                        clientPort,
                        fis
                );

            } catch (FileNotFoundException e) {
                System.err.println("file not found: " + localFile);
            }

        } catch (IOException e) {
            log.severe("failed to transfer: " + e);
        }
    }

}
