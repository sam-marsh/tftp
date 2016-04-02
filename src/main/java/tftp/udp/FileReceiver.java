package tftp.udp;

import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.util.UDPUtil;

import java.io.FileOutputStream;
import java.io.IOException;
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
            int remotePort, FileOutputStream fos) throws IOException {

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
                socket.send(datagram);

                try {
                    socket.receive(rcvDatagram);
                } catch (SocketTimeoutException timeout) {
                    ++timeouts;
                    continue;
                }

                remoteAddress = rcvDatagram.getAddress();
                remotePort = rcvDatagram.getPort();

                try {
                    TFTPPacket packet = UDPUtil.fromDatagram(rcvDatagram);

                    System.out.println("received " + packet);
                    if (packet instanceof DataPacket) {
                        DataPacket data = (DataPacket) packet;

                        if (data.getBlockNumber() == ackNumber + 1) {
                            fos.write(data.getPacketBytes(), DataPacket.DATA_OFFSET, data.getDataLength());
                            ++ackNumber;

                            if (data.isFinalPacket()) {
                                sendPacket = new AcknowledgementPacket(ackNumber);
                                datagram = UDPUtil.toDatagram(sendPacket, remoteAddress, remotePort);
                                socket.send(datagram);
                                return;
                            }

                            break;
                        }

                    } else if (packet instanceof ErrorPacket) {
                        throw new RuntimeException("error packet received: " + packet);
                    }

                } catch (TFTPException e) {
                    System.err.println("invalid packet received, ignoring");
                    ++invalids;
                }
            }

            if (timeouts == Configuration.MAX_TIMEOUTS) {
                throw new IOException("timeout");
            } else if (invalids == Configuration.MAX_INVALIDS) {
                throw new IOException("invalid");
            }
        }
    }

}
