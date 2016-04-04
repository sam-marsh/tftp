package tftp.tcp.client;

import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.packet.WriteRequestPacket;
import tftp.tcp.server.TFTPTCPServer;
import tftp.udp.client.ClientReceiver;
import tftp.udp.client.ClientSender;
import tftp.udp.client.TFTPUDPClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * @author Sam Marsh
 */
public class TFTPTCPClient extends Thread {

    private AsynchronousSocketChannel channel;

    /**
     * Creates a new TFTP client.
     */
    public TFTPTCPClient() {
        //nothing
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("tftp client");
        System.out.println("enter '?' to print a list of commands");

        while (true) {
            //print out prompt
            System.out.print("tftp> ");

            String line = scanner.nextLine();

            //read user input, split by whitespace
            String[] args = line.split(" ");

            //ignore and continue looping if no arguments, otherwise parse input
            if (args.length >= 1) {
                switch (args[0]) {
                    case "connect":
                        //set a hostname/ip address/port
                        handleConnect(args);
                        break;
                    case "get":
                        //read a file from the server
                        handleGet(args);
                        break;
                    case "put":
                        //write a file to the server
                        handlePut(args);
                        break;
                    case "timeout":
                        //set the timeout length
                        handleTimeout(args);
                        break;
                    case "exit":
                        //stop the client
                        return;
                    case "?":
                        //print a list of available commands
                        printHelp();
                        break;
                    default:
                        System.out.println("unrecognised command: " + args[0]);
                        System.out.println("enter '?' to print a list of commands");
                        break;
                }
            }
        }
    }

    /**
     * Sets the server address, and possibly port, based on the given user input.
     *
     * @param args the user input, split by whitespace
     */
    private void handleConnect(String[] args) {
        //if no arguments to command, print correct usage
        if (args.length == 1) {
            System.out.println("usage: connect host-name [port]");
            return;
        }

        InetAddress remoteAddress;
        int remotePort = Configuration.DEFAULT_SERVER_PORT;

        //set remote address, print error if no such host
        try {
            remoteAddress = InetAddress.getByName(args[1]);
        } catch (UnknownHostException uhe) {
            System.out.println("unknown host: " + args[1]);
            return;
        }

        //set remote port if given, print error if not an integer
        if (args.length >= 3) {
            try {
                remotePort = Integer.parseInt(args[2]);
            } catch (NumberFormatException nfe) {
                System.out.println("invalid port: " + args[2]);
                return;
            }
        }

        try {
            channel = AsynchronousSocketChannel.open();
            channel.connect(new InetSocketAddress(remoteAddress, remotePort)).get();
        } catch (IOException | InterruptedException | ExecutionException  e) {
            System.out.println("failed to connect to server: " + e.getMessage());
        }

    }

    /**
     * Attempts to retrieve a file from the TFTP server with address set previously using
     * the 'connect' command.
     *
     * @param args the user input, split by whitespace
     */
    private void handleGet(String[] args) {
        String remoteFile;
        String localFile = null;

        //if no arguments to command, print correct usage
        if (args.length == 1) {
            System.out.println("usage: get remote-path [local-path]");
            return;
        }

        //if no tftp server specified, print error
        if (channel == null || !channel.isOpen()) {
            System.out.println("not connected to a server");
            System.out.println("use 'connect' command to connect");
            return;
        }

        remoteFile = args[1];

        //optional argument - where to save the file. if not specified by the user, this will
        // be null, which means the file is saved in the current working directory
        if (args.length >= 3) {
            localFile = args[2];
        }

        //start the read request handler with the user's specified parameters

        //ClientReceiver handler = new ClientReceiver(remoteAddress, remotePort, remoteFile, localFile);
        //handler.run();
    }

    /**
     * Attempts to write a file to the TFTP server with address set previously using
     * the 'connect' command.
     *
     * @param args the user input, split by whitespace
     */
    private void handlePut(String[] args) {
        String localFile;
        String remoteFile = null;

        //if no arguments to command, print correct usage
        if (args.length == 1) {
            System.out.println("usage: put local-path [remote-path]");
            return;
        }

        //if no tftp server specified, print error
        if (channel == null || !channel.isOpen()) {
            System.out.println("not connected to a server");
            System.out.println("use 'connect' command to connect");
            return;
        }

        localFile = args[1];

        //optional argument - where to write the file. if not specified by the user, this will
        // be null, which means the file is saved in the working directory of the server
        if (args.length >= 3) {
            remoteFile = args[2];
        }

        ByteBuffer buffer = ByteBuffer.wrap(new WriteRequestPacket(remoteFile, Mode.OCTET).getPacketBytes());
        try {
            AsynchronousFileChannel in = AsynchronousFileChannel.open(Paths.get(localFile));
            new TFTPTCPServer.AsynchronousFileWriter(in, out)
        } catch (IOException e) {
            e.printStackTrace();
        }
        //start the write request handler with the user's specified parameters
        //ClientSender handler = new ClientSender(remoteAddress, remotePort, localFile, remoteFile);
        //handler.run();
    }

    /**
     * Sets the timeout timer length in milliseconds, used when reading/writing to the TFTP server.
     *
     * @param args the user input, split by whitespace
     */
    private void handleTimeout(String[] args) {
        //if no arguments to command, print correct usage
        if (args.length == 1) {
            System.out.println("usage: timeout time-in-ms");
            return;
        }

        //set timeout, print error if not an integer
        try {
            Configuration.TIMEOUT = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("invalid timeout: " + args[1]);
        }
    }

    /**
     * Prints the available commands, along with their required and optional arguments.
     */
    private void printHelp() {
        System.out.println("connect host-name [port]");
        System.out.println("get remote-path [local-path]");
        System.out.println("put local-path [remote-path]");
        System.out.println("timeout time-in-ms");
        System.out.println("exit");
    }

    /**
     * The entry point of this TFTP client program.
     *
     * @param args the user arguments
     */
    public static void main(String[] args) {
        Thread client = new TFTPTCPClient();
        client.start();
    }


}