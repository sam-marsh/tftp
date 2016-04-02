package tftp.udp.server;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.util.UDPUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

/**
 * @author Sam Marsh
 */
public class WRQHandler implements Runnable {

    private final Logger log;
    private final InetAddress clientAddress;
    private final int clientPort;
    private final WriteRequestPacket wrq;

    public WRQHandler(InetAddress clientAddress, int clientPort, WriteRequestPacket wrq) {
        this.log = Logger.getLogger(
                String.format("%s[%s:%d]", WRQHandler.class.getSimpleName(), clientAddress, clientPort)
        );
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.wrq = wrq;
    }

    @Override
    public void run() {
        log.info("connecting to client: " + clientAddress + ":" + clientPort);
        log.info("responding to request: " + wrq);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            byte[] rcvBuffer = new byte[Configuration.MAX_PACKET_LENGTH];
            DatagramPacket rcvDatagram = new DatagramPacket(rcvBuffer, rcvBuffer.length);

            try (FileOutputStream fos = new FileOutputStream(wrq.getFileName())) {

                short ackNumber = 0;

                while (true) {

                    AcknowledgementPacket ack = new AcknowledgementPacket(ackNumber);
                    DatagramPacket datagram = UDPUtil.toDatagram(ack, clientAddress, clientPort);

                    int timeouts = 0;
                    int invalids = 0;

                    while (timeouts < Configuration.MAX_TIMEOUTS && invalids < Configuration.MAX_INVALIDS) {
                        socket.send(datagram);

                        try {
                            socket.receive(rcvDatagram);
                        } catch (SocketTimeoutException timeout) {
                            ++timeouts;
                            continue;
                        }

                        try {
                            TFTPPacket packet = UDPUtil.fromDatagram(rcvDatagram);

                            System.out.println("received " + packet);
                            if (packet instanceof DataPacket) {
                                DataPacket data = (DataPacket) packet;

                                if (data.getBlockNumber() == ackNumber + 1) {
                                    fos.write(data.getPacketBytes(), DataPacket.DATA_OFFSET, data.getDataLength());
                                    ++ackNumber;

                                    if (data.isFinalPacket()) {
                                        ack = new AcknowledgementPacket(ackNumber);
                                        datagram = UDPUtil.toDatagram(ack, clientAddress, clientPort);
                                        socket.send(datagram);
                                        return;
                                    }

                                    break;
                                }

                            } else if (packet instanceof ErrorPacket) {
                                log.severe("error packet received: " + packet);
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

            } catch (FileNotFoundException fnfe) {
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "unable to write to: " + wrq.getFileName()
                );
                DatagramPacket sendPacket = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                socket.send(sendPacket);
            }

        } catch (IOException e) {
            log.severe("failed to receive: " + e);
        }
    }
}
