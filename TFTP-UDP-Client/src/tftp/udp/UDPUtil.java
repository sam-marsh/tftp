package tftp.udp;

import tftp.core.TFTPException;
import tftp.core.packet.TFTPPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * A utility class providing services related to sending/receiving TFTP packets in datagrams.
 */
public class UDPUtil {

    /**
     * Parses a TFTP packet from a datagram packet.
     *
     * @param datagram the datagram to read from
     * @return the TFTP packet contained in the datagram
     * @throws TFTPException if the datagram contains an invalid TFTP packet
     */
    public static TFTPPacket fromDatagram(DatagramPacket datagram) throws TFTPException {
        return TFTPPacket.fromByteArray(datagram.getData(), datagram.getLength());
    }

    /**
     * Given a TFTP packet and remote host information, composes a datagram packet containing this information.
     *
     * @param packet the TFTP packet to contain in the datagram
     * @param address the address of the remote host
     * @param port the port of the remote host
     * @return a datagram containing the TFTP packet and the remote address and port
     */
    public static DatagramPacket toDatagram(TFTPPacket packet, InetAddress address, int port) {
        byte[] data = packet.getPacketBytes();
        DatagramPacket datagram = new DatagramPacket(data, 0, data.length);
        datagram.setAddress(address);
        datagram.setPort(port);
        return datagram;
    }

}
