package tftp.udp.server;

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
import java.util.logging.Logger;

/**
 * Handles responses to read requests from clients.
 */
public class RRQHandler implements Runnable {

    private final Logger log;
    private final InetAddress clientAddress;
    private final int clientPort;
    private final ReadRequestPacket rrq;

    public RRQHandler(InetAddress clientAddress, int clientPort, ReadRequestPacket rrq) {
        this.log = Logger.getLogger(
                String.format("%s[%s:%d]", RRQHandler.class.getSimpleName(), clientAddress, clientPort)
        );
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.rrq = rrq;
    }

    @Override
    public void run() {
        log.info("connecting to client: " + clientAddress + ":" + clientPort);
        log.info("responding to request: " + rrq);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            if (rrq.getMode() != Mode.OCTET) {
                ErrorPacket error = new ErrorPacket(ErrorType.UNDEFINED, "unsupported mode: " + rrq.getMode());
                socket.send(UDPUtil.toDatagram(error, clientAddress, clientPort));
                log.severe("unsupported mode: " + rrq.getMode());
                return;
            }

            try (FileInputStream fis = new FileInputStream(rrq.getFileName())) {

                FileSender.send(socket, new AcknowledgementPacket((short) 0), clientAddress, clientPort, fis);

            } catch (FileNotFoundException e) {
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "file not found: " + rrq.getFileName()
                );
                DatagramPacket sendPacket = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                socket.send(sendPacket);
            }

        } catch (IOException e) {
            log.severe("failed to transfer: " + e);
        }
    }

}
