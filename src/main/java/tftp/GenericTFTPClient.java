package tftp;

import tftp.core.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author Sam Marsh
 */
public abstract class GenericTFTPClient extends Thread {


    /**
     * The address of the TFTP server.
     */
    protected InetAddress remoteAddress;

    /**
     * The port of the TFTP server.
     */
    protected int remotePort;

    /**
     * Creates a new TFTP client.
     */
    public GenericTFTPClient(int port) {
        this.remotePort = port;
    }

    /**
     * Run when the client thread is started. Loops, taking commands from standard input, until
     * the command 'exit' is entered.
     */
    @Override
    public final void run() {
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
        //set remote address, print error if no such host
        if (args.length >= 2) {
            try {
                remoteAddress = InetAddress.getByName(args[1]);
            } catch (UnknownHostException uhe) {
                System.out.println("unknown host: " + args[1]);
            }
        }
        //set remote port if given, print error if not an integer
        if (args.length >= 3) {
            try {
                remotePort = Integer.parseInt(args[2]);
            } catch (NumberFormatException nfe) {
                System.out.println("invalid port: " + args[2]);
            }
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
        if (remoteAddress == null) {
            System.out.println("not connected to a server");
            System.out.println("use 'connect' command to connect");
            return;
        }

        remoteFile = args[1];

        //optional argument - where to save the file. if not specified by the user, this will
        // be null, which means the file is saved in the current working directory
        if (args.length >= 3) {
            localFile = args[2];
        } else {
            localFile = Paths.get(remoteFile).getFileName().toString();
        }

        get(remoteFile, localFile);
    }

    protected abstract void get(String remoteFile, String localFile);

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
        if (remoteAddress == null) {
            System.out.println("not connected to a server");
            System.out.println("use 'connect' command to connect");
            return;
        }

        localFile = args[1];

        //optional argument - where to write the file. if not specified by the user, this will
        // be null, which means the file is saved in the working directory of the server
        if (args.length >= 3) {
            remoteFile = args[2];
        } else {
            remoteFile = Paths.get(localFile).getFileName().toString();
        }

        put(localFile, remoteFile);
    }

    protected abstract void put(String localFile, String remoteFile);

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

}
