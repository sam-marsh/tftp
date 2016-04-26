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
 * A generic class for sending a file over UDP using the trivial file transfer protocol.
 * This class is used by both client and server, since the actions of each are almost identical - when a
 * server receives a RRQ it needs to send a file, and when a client sends a WRQ it needs to send
 * a file.
 */
public class FileSender {

    /**
     * Sends a file to a TFTP host.
     *
     * @param socket the socket used to send and receive datagrams
     * @param firstPacket the first packet to send - this is usually a WRQ or a DATA1
     * @param remoteAddress the address of the remote host to send datagrams to
     * @param remotePort the port on the remote host to send datagrams to
     * @param fis the file input stream to read from, which is sent to the remote host
     * @param firstBlockNumber the initial block number - this differs depending on whether it is a client or server
     * @throws TFTPException if an 'unfixable' error occurred during transfer
     */
    public static void send(DatagramSocket socket, TFTPPacket firstPacket, InetAddress remoteAddress,
                            int remotePort, FileInputStream fis, short firstBlockNumber) throws TFTPException {

        //track the time taken and the number of bytes sent to print at the end if all goes well
        long startTime = System.currentTimeMillis();
        int bytesSent = 0;

        //the packet currently being sent into the network
        TFTPPacket sendPacket;

        //a buffer for holding the data contained in received datagrams
        byte[] receiveBuffer = new byte[Configuration.MAX_PACKET_LENGTH];
        //a buffer for holding the data read from the file
        byte[] fileBuffer = new byte[Configuration.MAX_DATA_LENGTH];
        
        //to check if we're sending the initial packet since this differs between server and client
        boolean first = true;

        //the current block number being sent - waiting for acknowledgement of this block
        short blockNumber = firstBlockNumber;

        //a variable to hold the number of bytes read from the file input stream (see below)
        int read;

        //the length of the last file-read (will be 512 unless it is the final read)
        int lastLength = Configuration.MAX_DATA_LENGTH;

        //loop until all file is sent, then break out
        while (true) {

            //generally will be sending data, but the first packet is different (could be WRQ or DATA1)
            // so check which ack we're up to and set the packet to send accordingly
            if (first) {
                sendPacket = firstPacket;
                if (firstPacket instanceof DataPacket) {
                    lastLength = ((DataPacket) firstPacket).getDataLength();
                }
            } else {
                try {
                    //read a chunk of the file into the file buffer (usually 512 bytes)
                    read = fis.read(fileBuffer);
                } catch (IOException e) {
                    System.out.println("error reading from file");
                    return;
                }
                if (read == -1) {
                    //if the file-read returned -1, then we have reached the end of the file. as per the TFTP
                    // RFC, need to check if the file size is a multiple of 512 bytes. if so, a zero-byte data
                    // packet must be sent.
                    if (lastLength == Configuration.MAX_DATA_LENGTH) {
                        //if last length sent was 512, then need to send a 0-byte data packet - so set read to 0
                        read = 0;
                    } else {
                        break;
                    }
                }
                //create a new data packet containing the file's chunk of data
                sendPacket = new DataPacket(blockNumber, fileBuffer, read);
                lastLength = read;
            }

            //convert the TFTP packet into a datagram
            DatagramPacket datagram = UDPUtil.toDatagram(sendPacket, remoteAddress, remotePort);

            //create a datagram 'shell' to hold the datagram received from the network
            DatagramPacket rcvDatagram = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            //keep track of the number of consecutive timeouts, and the number of nonsense packets received
            int timeouts = 0;
            int invalids = 0;

            //continue looping until we reach the max number of timeouts/invalids or until the packet is acknowledged
            // the hopeful alternative is that the relevant ACK is received, in which case will break out of the loop
            while (timeouts < Configuration.MAX_TIMEOUTS && invalids < Configuration.MAX_INVALIDS) {
                try {
                    //send the current datagram to the remote host
                    socket.send(datagram);

                    try {
                        //block until we receive a response, if this throws a timeout exception then increment
                        // the number of timeouts and 're-enter' the loop - thus sending the datagram again
                        socket.receive(rcvDatagram);
                    } catch (SocketTimeoutException timeout) {
                        System.out.println("timed out, resending " + sendPacket);
                        ++timeouts;
                        continue;
                    }

                    if (blockNumber == firstBlockNumber) {
                        //server can respond from a different port, so re-set the remote port based on
                        // the received datagram
                        remotePort = rcvDatagram.getPort();
                    }

                    //convert the received datagram to a TFTP packet - if this throws an exception, it means the packet
                    // is 'nonsensical' in terms of the protocol - so re-send the request for the next packet and
                    // increment the number of these invalid packets received
                    TFTPPacket received;
                    try {
                        received = UDPUtil.fromDatagram(rcvDatagram);
                    } catch (TFTPException e) {
                        ++invalids;
                        continue;
                    }

                    if (received instanceof AcknowledgementPacket) {
                        AcknowledgementPacket ack = (AcknowledgementPacket) received;

                        //packet has correct ack number, we are waiting on this packet
                        if (ack.getBlockNumber() == blockNumber) {
                            if (sendPacket.getPacketType() == PacketType.DATA) {
                                //update with the number of bytes successfully sent
                                bytesSent += ((DataPacket) sendPacket).getDataLength();
                            }
                            //ready to send the next packet - break out of the loop
                            ++blockNumber;
                            first = false;
                            break;
                        }

                    } else if (received instanceof ErrorPacket) {
                        //received error packet from remote host, so print the message and terminate
                        System.out.println("error: " + ((ErrorPacket) received).getMessage());
                        return;
                    }

                } catch (IOException e) {
                    //failed to send/receive datagram - just try again, up to the limit specified by the while loop
                    ++invalids;
                }
            }

            if (timeouts == Configuration.MAX_TIMEOUTS) {
                //too many timeouts - give up
                throw new TFTPException("error: transfer timed out");
            } else if (invalids == Configuration.MAX_INVALIDS) {
                //too many odd packets received or too many failed attempts to write to output stream
                throw new TFTPException(
                        "error: too many invalid packets received " +
                        "or error writing to/reading from socket"
                );
            }

        }

        //print information about the transfer, and finish
        long time = System.currentTimeMillis() - startTime;
        double seconds = (double) time / 1000.0;
        BigDecimal bigDecimal = new BigDecimal(seconds);
        bigDecimal = bigDecimal.setScale(1, BigDecimal.ROUND_UP);
        System.out.printf("sent %d bytes in %s seconds%n", bytesSent, bigDecimal.toPlainString());
    }

}
