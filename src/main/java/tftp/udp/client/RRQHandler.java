package tftp.udp.client;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.FileReceiver;
import tftp.udp.util.UDPUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * @author Sam Marsh
 */
public class RRQHandler implements Runnable {

    private final Logger log;
    private InetAddress clientAddress;
    private int clientPort;
    private final String remotePath;
    private final String localPath;

    public RRQHandler(InetAddress address, int port, String remotePath) {
        this(address, port, remotePath, null);
    }

    public RRQHandler(InetAddress clientAddress, int clientPort, String remotePath, String localPath) {
        this.log = Logger.getLogger(
                String.format("%s[%s:%d]", RRQHandler.class.getSimpleName(), clientAddress, clientPort)
        );
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.remotePath = remotePath;
        this.localPath = (localPath == null ? Paths.get(remotePath).getFileName().toString() : localPath);
    }

    @Override
    public void run() {
        log.info("connecting to server: " + clientAddress + ":" + clientPort);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            try (FileOutputStream fos = new FileOutputStream(localPath)) {

                FileReceiver.receive(
                        socket,
                        new ReadRequestPacket(remotePath, Mode.OCTET),
                        clientAddress,
                        clientPort,
                        fos
                );

            } catch (FileNotFoundException fnfe) {
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "unable to write to: " + localPath
                );
                DatagramPacket datagram = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                socket.send(datagram);
            }

        } catch (IOException e) {
            log.severe("failed to receive: " + e);
        }
    }
}
