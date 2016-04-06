package tftp.tcp;

import tftp.GenericTFTPClient;
import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;

import java.io.*;
import java.net.*;

/**
 * A client for sending/receiving files from a server using the Trivial File Transfer Protocol over TCP.
 */
public class TFTPTCPClient extends GenericTFTPClient {

    /**
     * Creates a new TFTP client.
     */
    public TFTPTCPClient(int port) {
        super(port);
    }

    /**
     * Receives a file from the server using TFTP over UDP.
     *
     * @param remoteFile the path of the file on the server
     * @param localFile the path of the file on the local machine
     */
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

            try {
                //read the TFTP packet from the server
                TFTPPacket response = readPadded(is);

                if (response instanceof ErrorPacket) {
                    System.out.println("error: " + ((ErrorPacket) response).getMessage());
                    return;
                }

                //should acknowledge the response from the server
                if (!(response instanceof AcknowledgementPacket)) {
                    System.out.println("unexpected packet from server, aborting: " + response);
                    return;
                }

            } catch (IOException e) {
                System.out.println("could not read server response: " + e.getMessage());
            } catch (TFTPException e) {
                System.out.println("could not parse server response: " + e.getMessage());
            }

            //receive the file now that ACK from server has been received
            TCPFileUtil.receive(is, localFile);

        } catch (IOException e) {
            System.out.println("could not create socket: " + e.getMessage());
        }
    }

    /**
     * Sends a file to the server using the TFTP protocol over UDP.
     *
     * @param localFile the path of the file on the local machine
     * @param remoteFile the path of the file on the server
     */
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

            try {
                //read the TFTP packet from the server
                TFTPPacket response = readPadded(is);

                if (response instanceof ErrorPacket) {
                    System.out.println("error: " + ((ErrorPacket) response).getMessage());
                    return;
                }

                //should acknowledge the response from the server
                if (!(response instanceof AcknowledgementPacket)) {
                    System.out.println("unexpected packet from server, aborting: " + response);
                    return;
                }

                //server accepted WRQ - send file
                TCPFileUtil.send(os, localFile);

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
     * Since the first packet could be an acknowledgement packet (4 bytes) or an error packet (arbitrarily many
     * bytes up to the max packet length), it is hard to tell where the file transfer starts. To solve this, the server
     * response is padded to {@link Configuration#MAX_PACKET_LENGTH} bytes - i.e. the file bytes start on offset 512.
     * This method reads the TFTP packet from the first 512 bytes read from the input stream.
     *
     * @param is the input stream from the server
     * @return the TFTP packet parsed from the input stream
     * @throws IOException if failed to read from the input stream
     * @throws TFTPException if failed to parse the TFTP packet
     */
    private TFTPPacket readPadded(InputStream is) throws IOException, TFTPException {
        byte[] padded = new byte[Configuration.MAX_PACKET_LENGTH];
        int read = is.read(padded, 0, padded.length);
        if (read != Configuration.MAX_PACKET_LENGTH)
            throw new TFTPException("packet not padded properly");
        return TFTPPacket.fromByteArray(padded, Configuration.MAX_PACKET_LENGTH);
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
