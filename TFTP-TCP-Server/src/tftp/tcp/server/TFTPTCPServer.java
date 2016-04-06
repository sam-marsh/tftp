package tftp.tcp.server;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.tcp.TCPFileUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main class, running a Trivial File Transfer server on TCP.
 */
public class TFTPTCPServer extends Thread {

    /**
     * The port to bind the server socket to.
     */
    private final int port;

    /**
     * Creates a new TFTP TCP server.
     *
     * @param port the port to bind the server socket to
     */
    public TFTPTCPServer(int port) {
        this.port = port;
    }

    /**
     * Run when this thread is started - loops forever, responding to WRQs/RRQs from clients.
     */
    @Override
    public void run() {
        //open the 'master' socket which receives RRQs and WRQs
        try (ServerSocket mainSocket = new ServerSocket(port)){

            //loop forever, continually accepting requests from clients
            while (true) {

                //accept a connection (blocks)
                Socket workerSocket;
                try {
                    workerSocket = mainSocket.accept();
                } catch (IOException e) {
                    System.out.println("failed to accept new connection: " + e.getMessage());
                    continue;
                }

                ExecutorService executor = Executors.newSingleThreadExecutor();

                //first, submit a job to be executed in parallel which responds to the client request
                executor.submit(() -> {

                    //get the input and output streams
                    InputStream is;
                    OutputStream os;
                    try {
                        is = workerSocket.getInputStream();
                    } catch (IOException e) {
                        System.out.println("failed to open input stream: " + e.getMessage());
                        return;
                    }
                    try {
                        os = workerSocket.getOutputStream();
                    } catch (IOException e) {
                        System.out.println("failed to open output stream: " + e.getMessage());
                        return;
                    }

                    //allocate a buffer to store the client request
                    byte[] buffer = new byte[Configuration.MAX_PACKET_LENGTH];

                    //read the client request from the input stream
                    int read;
                    try {
                        read = is.read(buffer);
                    } catch (IOException e) {
                        System.out.println("unable to read from network: " + e.getMessage());
                        return;
                    }

                    if (read == -1) {
                        return;
                    }

                    //convert the 'raw bytes' to a TFTP packet
                    TFTPPacket packet;
                    try {
                        packet = TFTPPacket.fromByteArray(buffer, read);
                    } catch (TFTPException e) {
                        System.out.println("invalid tftp packet received: " + e.getMessage());
                        return;
                    }

                    //ensure the packet is a WRQ or RRQ
                    if (!(packet instanceof RequestPacket)) {
                        System.out.println("unexpected tftp packet received, ignoring: " + packet);
                        return;
                    }

                    RequestPacket rq = (RequestPacket) packet;

                    //only octet (binary) mode is supported by the server
                    if (rq.getMode() != Mode.OCTET) {
                        System.out.println("unsupported mode: " + rq.getMode());
                        return;
                    }

                    String fileName = ((RequestPacket) packet).getFileName();
                    File file = new File(fileName);


                    if (packet instanceof WriteRequestPacket) {

                        //send an acknowledgement to the client so it will send the file through
                        AcknowledgementPacket ack = new AcknowledgementPacket((short) 0);
                        try {
                            writePadded(ack, os);
                        } catch (IOException e) {
                            System.out.println("could not send acknowledgement: " + e.getMessage());
                            return;
                        }

                        //now receive the file
                        TCPFileUtil.receive(is, fileName);

                    } else if (packet instanceof ReadRequestPacket) {

                        //ensure that the requested file exists
                        if (!file.exists()) {
                            ErrorPacket error = new ErrorPacket(
                                    ErrorType.FILE_NOT_FOUND, "file not found: " + rq.getFileName()
                            );
                            try {
                                writePadded(error, os);
                            } catch (IOException e) {
                                System.out.println("could not send error packet: " + e.getMessage());
                            }
                            return;
                        }

                        //send an acknowledgement to the client to notify it that all is going well and the file is
                        // about to be sent through
                        AcknowledgementPacket ack = new AcknowledgementPacket((short) 0);
                        try {
                            writePadded(ack, os);
                        } catch (IOException e) {
                            System.out.println("could not send acknowledgement: " + e.getMessage());
                            return;
                        }

                        //now send it to the client
                        TCPFileUtil.send(os, fileName);

                    }

                });

                //submit a job to close the worker socket after the worker has finished communicating with the client
                // the executor only has one thread, so jobs are executed in sequence
                executor.submit(() -> {
                    try {
                        workerSocket.close();
                    } catch (IOException ignore) {}
                });

            }

        } catch (IOException e) {
            System.out.println("failed to start server: " + e.getMessage());
        }
    }

    /**
     * Since the first packet could be an acknowledgement packet (4 bytes) or an error packet (arbitrarily many
     * bytes up to the max packet length), it is hard to tell where the file transfer starts. To solve this, the server
     * response is padded to {@link Configuration#MAX_PACKET_LENGTH} bytes - i.e. the file bytes start on offset 512.
     * This method writes a TFTP packet to the stream, padding it to 512 bytes long.
     *
     * @param packet the packet to send to the client
     * @param os the output stream to the client
     * @throws IOException if the packet could not be sent
     */
    private void writePadded(TFTPPacket packet, OutputStream os) throws IOException {
        byte[] padded = new byte[Configuration.MAX_PACKET_LENGTH];
        byte[] bytes = packet.getPacketBytes();
        System.arraycopy(bytes, 0, padded, 0, bytes.length);
        os.write(padded);
    }

    /**
     * The entry point of the program.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        int port = Configuration.DEFAULT_SERVER_PORT;

        //parse the arguments
        for (int i = 0; i < args.length - 1; ++i) {
            if (args[i].equals("-port")) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException nfe) {
                    System.out.println("invalid port: " + args[i + 1]);
                    return;
                }
            } else if (args[i].equals("-timeout")) {
                try {
                    Configuration.TIMEOUT = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException nfe) {
                    System.out.println("invalid timeout: " + args[i + 1]);
                    return;
                }
            }
        }

        //run the server, passing the port as an argument
        Thread server = new TFTPTCPServer(port);
        server.start();
    }

}
