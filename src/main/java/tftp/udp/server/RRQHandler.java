package tftp.udp.server;

import jdk.nashorn.internal.runtime.regexp.joni.Config;
import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.util.UDPUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

/**
 * @author Sam Marsh
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
                            ++invalids;
                        }
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
