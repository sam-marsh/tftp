package tftp.udp.server;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.udp.FileReceiver;
import tftp.udp.util.UDPUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

/**
 * @author Sam Marsh
 */
public class ReadFromClient implements Runnable {

    private InetAddress clientAddress;
    private int clientPort;
    private final WriteRequestPacket wrq;

    public ReadFromClient(InetAddress clientAddress, int clientPort, WriteRequestPacket wrq) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.wrq = wrq;
    }

    @Override
    public void run() {
        System.out.println("responding to request: " + wrq + " from client: " + clientAddress + ":" + clientPort);

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            try (FileOutputStream fos = new FileOutputStream(wrq.getFileName())) {

                FileReceiver.receive(socket, new AcknowledgementPacket((short) 0), clientAddress, clientPort, fos);

            } catch (FileNotFoundException fnfe) {
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "unable to write to: " + wrq.getFileName()
                );
                DatagramPacket datagram = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                socket.send(datagram);
            } catch (TFTPException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println("failed to receive: " + e.getMessage());
        }
    }
}
