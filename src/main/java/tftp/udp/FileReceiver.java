package tftp.udp;

import tftp.core.Configuration;
import tftp.core.TFTPException;
import tftp.core.packet.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * A generic class for receiving a file over UDP using the trivial file transfer protocol.
 * This class is used by both client and server, since the actions of each are almost identical - when a
 * server receives a WRQ it needs to receive a file, and when a client sends a RRQ it needs to receive
 * a file.
 */
public class FileReceiver {

    /**
     * Receives a file from a TFTP host.
     *
     * @param socket the socket used to send and receive datagrams
     * @param firstPacket the first packet to send - this is usually a RRQ or an ACK0
     * @param remoteAddress the address of the remote host to send datagrams to
     * @param remotePort the port on the remote host to send datagrams to
     * @param fos the file output stream to write the received data to
     * @throws TFTPException if an 'unfixable' error occured during transfer
     */
    public static void receive(
            DatagramSocket socket, TFTPPacket firstPacket, InetAddress remoteAddress,
            int remotePort, FileOutputStream fos) throws TFTPException {

        //track the time taken and the number of bytes received to print at the end if all goes well
        long startTime = System.currentTimeMillis();
        int bytesReceived = 0;

        //the packet currently being sent into the network
        TFTPPacket sendPacket;

        //a buffer for holding the data contained in received datagrams
        byte[] rcvBuffer = new byte[Configuration.MAX_PACKET_LENGTH];

        //a datagram object to hold received datagrams
        DatagramPacket rcvDatagram = new DatagramPacket(rcvBuffer, rcvBuffer.length);

        //the acknowledgement number - currently acknowlegding the data packet with this block number
        short ackNumber = 0;

        //loop until all file is received, then break out
        while (true) {

            //generally will be sending acks, but the first packet is different (could be ACK0 or RRQ)
            // so check which ack we're up to and set the packet to send accordingly
            if (ackNumber == 0) {
                sendPacket = firstPacket;
            } else {
                sendPacket = new AcknowledgementPacket(ackNumber);
            }

            //convert the TFTP packet to a datagram
            DatagramPacket datagram = UDPUtil.toDatagram(sendPacket, remoteAddress, remotePort);

            //keep track of the number of consecutive timeouts, and the number of nonsense packets received
            int timeouts = 0;
            int invalids = 0;

            //continue looping until we reach the max number of timeouts/invalids
            while (timeouts < Configuration.MAX_TIMEOUTS && invalids < Configuration.MAX_INVALIDS) {
                try {
                    //send the current datagram to the remote host
                    socket.send(datagram);

                    try {
                        //block until we receive a response, if this throws a timeout exception then increment
                        // the number of timeouts and 're-enter' the loop - thus sending the datagram again
                        socket.receive(rcvDatagram);
                    } catch (SocketTimeoutException timeout) {
                        ++timeouts;
                        continue;
                    }

                    if (ackNumber == 0) {
                        //server can respond from a different port, so re-set the remote address and port based on
                        // the received datagram
                        remotePort = rcvDatagram.getPort();
                    }

                    //convert the received datagram to a TFTP packet - if this throws an exception, it means the packet
                    // is 'nonsensical' in terms of the protocol - so re-send the request for the next packet and
                    // increment the number of these invalid packets received
                    TFTPPacket packet;
                    try {
                        packet = UDPUtil.fromDatagram(rcvDatagram);
                    } catch (TFTPException e) {
                        ++invalids;
                        continue;
                    }

                    if (packet instanceof DataPacket) {
                        DataPacket data = (DataPacket) packet;

                        //packet has correct block number, we are waiting on this pcaket
                        if (data.getBlockNumber() == ackNumber + 1) {
                            //write the data received in the data packet to the file
                            fos.write(data.getPacketBytes(), DataPacket.DATA_OFFSET, data.getDataLength());
                            //increment the number of bytes successfully received
                            bytesReceived += data.getDataLength();
                            //now we are waiting on the packet with block number (ackNumber + 1)
                            ++ackNumber;

                            //if this is the final packet, send an acknowledgement, print information about the
                            // transfer, and finish
                            if (data.isFinalPacket()) {
                                sendPacket = new AcknowledgementPacket(ackNumber);
                                datagram = UDPUtil.toDatagram(sendPacket, remoteAddress, remotePort);
                                socket.send(datagram);

                                long time = System.currentTimeMillis() - startTime;
                                double seconds = (double) time / 1000.0;
                                BigDecimal bigDecimal = new BigDecimal(seconds);
                                bigDecimal = bigDecimal.setScale(1, BigDecimal.ROUND_UP);
                                System.out.printf(
                                        "received %d bytes in %s seconds%n",
                                        bytesReceived, bigDecimal.toPlainString()
                                );
                                return;
                            }

                            break;
                        }

                    } else if (packet instanceof ErrorPacket) {
                        //received error packet from remote host, so print the message and terminate
                        System.out.println("error packet received: " + packet);
                        return;
                    }

                } catch (IOException e) {
                    //failed to write to file for whatever reason - can still try again, but only up to MAX_INVALIDS
                    // times in a row
                    ++invalids;
                }
            }

            if (timeouts == Configuration.MAX_TIMEOUTS) {
                //too many timeouts - give up
                throw new TFTPException("error: transfer timed out");
            } else if (invalids == Configuration.MAX_INVALIDS) {
                //too many odd packets received or too many failed attempts to write to output stream
                throw new TFTPException("error: too many invalid packets received " +
                        "or failed to write to file too many times");
            }
        }
    }

}
