package tftp.udp;

import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.util.UDPUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * @author Sam Marsh
 */
public class FileSender {

    public static void send(DatagramSocket socket, TFTPPacket firstPacket, InetAddress remoteAddress,
                            int remotePort, FileInputStream fis) throws IOException {

        byte[] receiveBuffer = new byte[Configuration.MAX_PACKET_LENGTH];
        byte[] fileBuffer = new byte[Configuration.MAX_DATA_LENGTH];

        TFTPPacket sendPacket;

        short blockNumber = 0;

        int read;
        int lastLength = Configuration.MAX_DATA_LENGTH;

        while (true) {

            if (blockNumber == 0) {
                sendPacket = firstPacket;
            } else {
                read = fis.read(fileBuffer);
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

                socket.send(datagram);
                try {
                    socket.receive(rcvDatagram);
                } catch (SocketTimeoutException timeout) {
                    System.err.println("timed out, resending " + sendPacket);
                    ++timeouts;
                    continue;
                }

                remoteAddress = rcvDatagram.getAddress();
                remotePort = rcvDatagram.getPort();

                try {
                    TFTPPacket received = UDPUtil.fromDatagram(rcvDatagram);

                    if (received instanceof AcknowledgementPacket) {
                        AcknowledgementPacket ack = (AcknowledgementPacket) received;
                        System.out.println("received " + ack);

                        if (ack.getBlockNumber() == blockNumber) {
                            ++blockNumber;
                            break;
                        }

                    } else if (received instanceof ErrorPacket) {
                        System.err.println("error packet received: " + received);
                        return;
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
