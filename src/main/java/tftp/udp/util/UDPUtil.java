package tftp.udp.util;

import tftp.core.TFTPException;
import tftp.core.packet.TFTPPacket;

import java.net.DatagramPacket;

/**
 * @author Sam Marsh
 */
public class UDPUtil {

    public static TFTPPacket fromDatagram(DatagramPacket datagram) throws TFTPException {
        return TFTPPacket.fromByteArray(datagram.getData(), datagram.getLength());
    }

    public static DatagramPacket toDatagram(TFTPPacket packet) {
        byte[] data = packet.getPacketBytes();
        return new DatagramPacket(data, 0, data.length);
    }

}
