package tftp.tcp;

import tftp.GenericTFTPClient;
import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.AcknowledgementPacket;
import tftp.core.packet.ReadRequestPacket;
import tftp.core.packet.TFTPPacket;
import tftp.core.packet.WriteRequestPacket;

import java.io.*;
import java.net.*;

/**
 * @author Sam Marsh
 */
public class TFTPTCPClient extends GenericTFTPClient {

    /**
     * Creates a new TFTP client.
     */
    public TFTPTCPClient(int port) {
        super(port);
    }

    @Override
    protected void get(String remoteFile, String localFile) {
        //open a socket using any free port
        try (Socket socket = new Socket()) {

            //attempt to connect to the server
            try {
                socket.connect(new InetSocketAddress(remoteAddress, remotePort));
            } catch (IOException e) {
                System.out.println("could not reach server: " + e.getMessage());
                return;
            }

            //open the input and output streams
            InputStream is;
            OutputStream os;
            try {
                is = socket.getInputStream();
            } catch (IOException e) {
                System.out.println("failed to open input stream: " + e.getMessage());
                return;
            }
            try {
                os = socket.getOutputStream();
            } catch (IOException e) {
                System.out.println("failed to open output stream: " + e.getMessage());
                return;
            }

            //send an initial RRQ
            ReadRequestPacket rrq = new ReadRequestPacket(remoteFile, Mode.OCTET);

            try {
                os.write(rrq.getPacketBytes());
            } catch (IOException e) {
                System.out.println("could not send read request: " + e.getMessage());
                return;
            }

            //receive the file in response
            FileReceiver.receive(is, localFile);

        } catch (IOException e) {
            System.out.println("could not create socket: " + e.getMessage());
        }
    }

    @Override
    protected void put(String localFile, String remoteFile) {
        File file = new File(localFile);
        if (!file.exists()) {
            System.out.println("file does not exist: " + localFile);
            return;
        }

        //open a socket using any free port
        try (Socket socket = new Socket()) {

            //attempt to connect to the server
            try {
                socket.connect(new InetSocketAddress(remoteAddress, remotePort));
            } catch (IOException e) {
                System.out.println("could not reach server: " + e.getMessage());
                return;
            }

            //open the input and output streams
            InputStream is;
            OutputStream os;
            try {
                is = socket.getInputStream();
            } catch (IOException e) {
                System.out.println("failed to open input stream: " + e.getMessage());
                return;
            }
            try {
                os = socket.getOutputStream();
            } catch (IOException e) {
                System.out.println("failed to open output stream: " + e.getMessage());
                return;
            }

            //send an initial WRQ
            WriteRequestPacket wrq = new WriteRequestPacket(remoteFile, Mode.OCTET);

            try {
                os.write(wrq.getPacketBytes());
            } catch (IOException e) {
                System.out.println("could not send write request: " + e.getMessage());
                return;
            }

            //allocate a buffer for receiving the acknowledgement
            byte[] buffer = new byte[Configuration.MAX_DATA_LENGTH];

            try {
                //read the TFTP packet from the server
                int read = is.read(buffer);

                if (read == -1) {
                    throw new IOException("end of stream reached");
                }

                //convert server response to TFTP packet
                TFTPPacket response = TFTPPacket.fromByteArray(buffer, read);

                //should acknowledge the response from the server
                if (!(response instanceof AcknowledgementPacket)) {
                    System.out.println("unexpected packet from server, aborting: " + response);
                    return;
                }

                //server accepted WRQ - send file
                FileSender.send(os, localFile);

            } catch (IOException e) {
                System.out.println("could not read server response: " + e.getMessage());
            } catch (TFTPException e) {
                System.out.println("could not parse server response: " + e.getMessage());
            }

        } catch (IOException e) {
            System.out.println("could not create socket: " + e.getMessage());
        }
    }

    /**
     * The entry point of this TFTP client program.
     *
     * @param args the user arguments
     */
    public static void main(String[] args) {
        Thread client = new TFTPTCPClient(Configuration.DEFAULT_SERVER_PORT);
        client.start();
    }


}
