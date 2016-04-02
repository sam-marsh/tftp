package tftp.udp.server;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
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

            byte[] receiveBuffer = new byte[Configuration.MAX_PACKET_LENGTH];

            byte[] fileBuffer = new byte[Configuration.MAX_DATA_LENGTH];

            DataPacket dataPacket = null;

            try (FileInputStream fis = new FileInputStream(rrq.getFileName())) {

                short blockNumber = 1;

                int read;
                while ((read = fis.read(fileBuffer)) != -1 || dataPacket == null) {

                    if (read == -1) {
                        dataPacket = new DataPacket(blockNumber, fileBuffer, 0);
                    } else {
                        dataPacket = new DataPacket(blockNumber, fileBuffer, read);
                    }

                    DatagramPacket datagram = UDPUtil.toDatagram(dataPacket, clientAddress, clientPort);

                    DatagramPacket rcvDatagram = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                    int timeouts = 0;
                    int invalids = 0;

                    while (timeouts < Configuration.MAX_TIMEOUTS && invalids < Configuration.MAX_INVALIDS) {

                        socket.send(datagram);
                        try {
                            socket.receive(rcvDatagram);
                        } catch (SocketTimeoutException timeout) {
                            log.warning("timed out, resending " + dataPacket);
                            ++timeouts;
                            continue;
                        }

                        try {
                            TFTPPacket received = UDPUtil.fromDatagram(rcvDatagram);

                            if (received instanceof AcknowledgementPacket) {
                                AcknowledgementPacket ack = (AcknowledgementPacket) received;
                                log.info("received " + ack);

                                if (ack.getBlockNumber() == blockNumber) {
                                    ++blockNumber;
                                    break;
                                }

                            } else if (received instanceof ErrorPacket) {
                                log.severe("error packet received: " + received);
                                return;
                            }

                        } catch (TFTPException e) {
                            log.warning("invalid packet received, ignoring");
                            ++invalids;
                        }
                    }

                    if (timeouts == Configuration.MAX_TIMEOUTS) {
                        throw new IOException("timeout");
                    } else if (invalids == Configuration.MAX_INVALIDS) {
                        throw new IOException("invalid");
                    }

                }

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
