package tftp.udp.client;

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
import java.net.*;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * @author Sam Marsh
 */
public class RRQHandler implements Runnable {

    private InetAddress clientAddress;
    private int clientPort;
    private final String remotePath;
    private final String localPath;

    public RRQHandler(InetAddress address, int port, String remotePath) {
        this(address, port, remotePath, null);
    }

    public RRQHandler(InetAddress clientAddress, int clientPort, String remotePath, String localPath) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.remotePath = remotePath;
        this.localPath = (localPath == null ? Paths.get(remotePath).getFileName().toString() : localPath);
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(Configuration.TIMEOUT);

            try (FileOutputStream fos = new FileOutputStream(localPath)) {

                FileReceiver.receive(
                        socket,
                        new ReadRequestPacket(remotePath, Mode.OCTET),
                        clientAddress,
                        clientPort,
                        fos
                );

            } catch (FileNotFoundException fnfe) {
                System.out.println("unable to write to: " + localPath);
                ErrorPacket errorPacket = new ErrorPacket(
                        ErrorType.FILE_NOT_FOUND,
                        "unable to write to: " + localPath
                );
                DatagramPacket datagram = UDPUtil.toDatagram(errorPacket, clientAddress, clientPort);
                socket.send(datagram);
            } catch (IOException e) {
                System.out.println("error: " + e.getMessage());
            }

        } catch (SocketException e) {
            System.out.println("error: socket could not be opened");
        } catch (TFTPException e) {
            System.out.println(e.getMessage());
        } catch (IOException ignore) {
            //only reaches here if unable to send error packet, but already printed error message by this time
        }
    }
}
