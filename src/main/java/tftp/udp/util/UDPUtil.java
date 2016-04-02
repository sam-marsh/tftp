package tftp.udp.util;

import tftp.core.TFTPException;
import tftp.core.packet.TFTPPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * @author Sam Marsh
 */
public class UDPUtil {

    public static TFTPPacket fromDatagram(DatagramPacket datagram) throws TFTPException {
        return TFTPPacket.fromByteArray(datagram.getData(), datagram.getLength());
    }

    public static DatagramPacket toDatagram(TFTPPacket packet, InetAddress address, int port) {
        byte[] data = packet.getPacketBytes();
        DatagramPacket datagram = new DatagramPacket(data, 0, data.length);
        datagram.setAddress(address);
        datagram.setPort(port);
        return datagram;
    }

}
