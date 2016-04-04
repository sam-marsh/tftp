package tftp.udp;

import tftp.core.Configuration;
import tftp.core.TFTPException;
import tftp.core.packet.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * @author Sam Marsh
 */
public class FileSender {

    public static void send(DatagramSocket socket, TFTPPacket firstPacket, InetAddress remoteAddress,
                            int remotePort, FileInputStream fis, short firstBlockNumber) throws TFTPException {

        long startTime = System.currentTimeMillis();
        int bytesSent = 0;

        byte[] receiveBuffer = new byte[Configuration.MAX_PACKET_LENGTH];
        byte[] fileBuffer = new byte[Configuration.MAX_DATA_LENGTH];

        TFTPPacket sendPacket;

        short blockNumber = firstBlockNumber;

        int read;
        int lastLength = Configuration.MAX_DATA_LENGTH;

        while (true) {

            if (blockNumber == firstBlockNumber) {
                sendPacket = firstPacket;
                if (firstPacket instanceof DataPacket) {
                    lastLength = ((DataPacket) firstPacket).getDataLength();
                }
            } else {
                try {
                    read = fis.read(fileBuffer);
                } catch (IOException e) {
                    System.out.println("error reading from file");
                    return;
                }
                if (read == -1) {
                    if (lastLength == Configuration.MAX_DATA_LENGTH) {
                        read = 0;
                    } else {
                        break;
                    }
                }
                sendPacket = new DataPacket(blockNumber, fileBuffer, read);
                lastLength = read;
            }

            DatagramPacket datagram = UDPUtil.toDatagram(sendPacket, remoteAddress, remotePort);

            DatagramPacket rcvDatagram = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            int timeouts = 0;
            int invalids = 0;

            while (timeouts < Configuration.MAX_TIMEOUTS && invalids < Configuration.MAX_INVALIDS) {

                try {
                    socket.send(datagram);
                    try {
                        socket.receive(rcvDatagram);
                    } catch (SocketTimeoutException timeout) {
                        System.out.println("timed out, resending " + sendPacket);
                        ++timeouts;
                        continue;
                    }

                    remoteAddress = rcvDatagram.getAddress();
                    remotePort = rcvDatagram.getPort();

                    TFTPPacket received = UDPUtil.fromDatagram(rcvDatagram);

                    if (received instanceof AcknowledgementPacket) {
                        AcknowledgementPacket ack = (AcknowledgementPacket) received;

                        if (ack.getBlockNumber() == blockNumber) {
                            if (sendPacket.getPacketType() == PacketType.DATA) {
                                bytesSent += ((DataPacket) sendPacket).getDataLength();
                            }
                            ++blockNumber;
                            break;
                        }

                    } else if (received instanceof ErrorPacket) {
                        System.out.println("error packet received: " + received);
                        return;
                    }

                } catch (TFTPException | IOException e) {
                    ++invalids;
                }
            }

            if (timeouts == Configuration.MAX_TIMEOUTS) {
                throw new TFTPException("error: transfer timed out");
            } else if (invalids == Configuration.MAX_INVALIDS) {
                throw new TFTPException("error: too many invalid packets received");
            }

        }

        long time = System.currentTimeMillis() - startTime;
        double seconds = (double) time / 1000.0;
        BigDecimal bigDecimal = new BigDecimal(seconds);
        bigDecimal = bigDecimal.setScale(1, BigDecimal.ROUND_UP);
        System.out.printf("sent %d bytes in %s seconds%n", bytesSent, bigDecimal.toPlainString());
    }

}
