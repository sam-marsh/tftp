package tftp.udp.server;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.FileSender;
import tftp.udp.util.UDPUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

/**
 * Handles responses to read requests from clients.
 */
public class RRQHandler implements Runnable {

    private final InetAddress clientAddress;
    private final int clientPort;
    private final ReadRequestPacket rrq;

    public RRQHandler(InetAddress clientAddress, int clientPort, ReadRequestPacket rrq) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.rrq = rrq;
    }

    @Override
    public void run() {
        System.out.println("responding to request: " + rrq + " from client: " + clientAddress + ":" + clientPort);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            if (rrq.getMode() != Mode.OCTET) {
                ErrorPacket error = new ErrorPacket(ErrorType.UNDEFINED, "unsupported mode: " + rrq.getMode());
                socket.send(UDPUtil.toDatagram(error, clientAddress, clientPort));
                System.out.println("unsupported mode: " + rrq.getMode());
                return;
            }

            try (FileInputStream fis = new FileInputStream(rrq.getFileName())) {

                byte[] first = new byte[Configuration.MAX_DATA_LENGTH];
                int read = fis.read(first);
                if (read == -1) read = 0;
                DataPacket data = new DataPacket((short) 1, first, read);

                FileSender.send(socket, data, clientAddress, clientPort, fis, (short) 1);

            } catch (FileNotFoundException e) {
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "file not found: " + rrq.getFileName()
                );
                DatagramPacket sendPacket = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                socket.send(sendPacket);
            } catch (TFTPException e) {
                System.out.println(e.getMessage());
            }

        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }

}
