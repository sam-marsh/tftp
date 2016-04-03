package tftp.udp;

import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.util.UDPUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * @author Sam Marsh
 */
public class FileReceiver {

    public static void receive(
            DatagramSocket socket, TFTPPacket firstPacket, InetAddress remoteAddress,
            int remotePort, FileOutputStream fos) throws TFTPException {

        long startTime = System.currentTimeMillis();
        int bytesReceived = 0;

        TFTPPacket sendPacket;

        byte[] rcvBuffer = new byte[Configuration.MAX_PACKET_LENGTH];
        DatagramPacket rcvDatagram = new DatagramPacket(rcvBuffer, rcvBuffer.length);

        short ackNumber = 0;

        while (true) {

            if (ackNumber == 0) {
                sendPacket = firstPacket;
            } else {
                sendPacket = new AcknowledgementPacket(ackNumber);
            }

            DatagramPacket datagram = UDPUtil.toDatagram(sendPacket, remoteAddress, remotePort);

            int timeouts = 0;
            int invalids = 0;

            while (timeouts < Configuration.MAX_TIMEOUTS && invalids < Configuration.MAX_INVALIDS) {
                try {

                    socket.send(datagram);

                    try {
                        socket.receive(rcvDatagram);
                    } catch (SocketTimeoutException timeout) {
                        ++timeouts;
                        continue;
                    }

                    remoteAddress = rcvDatagram.getAddress();
                    remotePort = rcvDatagram.getPort();

                    TFTPPacket packet;
                    try {
                        packet = UDPUtil.fromDatagram(rcvDatagram);
                    } catch (TFTPException e) {
                        ++invalids;
                        continue;
                    }

                    if (packet instanceof DataPacket) {
                        DataPacket data = (DataPacket) packet;

                        if (data.getBlockNumber() == ackNumber + 1) {
                            fos.write(data.getPacketBytes(), DataPacket.DATA_OFFSET, data.getDataLength());
                            bytesReceived += data.getDataLength();
                            ++ackNumber;

                            if (data.isFinalPacket()) {
                                sendPacket = new AcknowledgementPacket(ackNumber);
                                datagram = UDPUtil.toDatagram(sendPacket, remoteAddress, remotePort);
                                socket.send(datagram);


                                long time = System.currentTimeMillis() - startTime;
                                double seconds = (double) time / 1000.0;
                                BigDecimal bigDecimal = new BigDecimal(seconds);
                                bigDecimal = bigDecimal.setScale(1, BigDecimal.ROUND_UP);
                                System.out.printf("received %d bytes in %s seconds%n", bytesReceived, bigDecimal
                                        .toPlainString());
                                return;
                            }

                            break;
                        }

                    } else if (packet instanceof ErrorPacket) {
                        System.out.println("error packet received: " + packet);
                        return;
                    }

                } catch (IOException e) {
                    ++invalids;
                }
            }

            if (timeouts == Configuration.MAX_TIMEOUTS) {
                throw new TFTPException("error: transfer timed out");
            } else if (invalids == Configuration.MAX_INVALIDS) {
                throw new TFTPException("error: too many invalid packets received");
            }
        }
    }

}
