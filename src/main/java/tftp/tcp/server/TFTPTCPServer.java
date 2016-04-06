package tftp.tcp.server;

import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.packet.*;
import tftp.tcp.FileReceiver;
import tftp.tcp.FileSender;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Sam Marsh
 */
public class TFTPTCPServer extends Thread {

    private final int port;

    public TFTPTCPServer(int port) {
        this.port = port;
    }

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
                            os.write(ack.getPacketBytes());
                        } catch (IOException e) {
                            System.out.println("could not send acknowledgement: " + e.getMessage());
                            return;
                        }

                        //now receive the file
                        FileReceiver.receive(is, fileName);

                    } else if (packet instanceof ReadRequestPacket) {

                        //ensure that the requested file exists
                        if (!file.exists()) {
                            System.out.println("file does not exist: " + rq.getFileName());
                            return;
                        }

                        //if so, send it to the client
                        FileSender.send(os, fileName);

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
     * The entry point of the program.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        Thread server = new TFTPTCPServer(Configuration.DEFAULT_SERVER_PORT);
        server.start();
    }

}
