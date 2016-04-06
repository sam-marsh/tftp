package tftp.tcp;

import tftp.core.Configuration;
import tftp.core.packet.AcknowledgementPacket;

import java.io.*;

/**
 * Utility class for writing and reading files over TCP.
 */
public class TCPFileUtil {

    /**
     * Sends a file over an output stream.
     *
     * @param os the output stream to write the file to
     * @param fileName the path of the file to write to the output stream
     */
    public static void send(OutputStream os, String fileName) {
        //allocate a buffer for sending data - might as well make this 512 bytes, like the data packets in TFTP
        byte[] buffer = new byte[Configuration.MAX_DATA_LENGTH];

        //open an input stream to the file
        try (FileInputStream reader = new FileInputStream(fileName)) {
            int num;
            try {
                //keep on writing to the output stream until the end of the file is reached
                while ((num = reader.read(buffer)) != -1) {
                    os.write(buffer, 0, num);
                }
            } catch (IOException e) {
                System.out.println("error sending file: " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("error reading from file: " + e.getMessage());
        }
    }

    /**
     * Receives a file from an input stream and writes it to file.
     *
     * @param is the input stream to read the file bytes from
     * @param file the path where the file will be written
     */
    public static void receive(InputStream is, String file) {
        //allocate a buffer for sending data - might as well make this 512 bytes, like the data packets in TFTP
        byte[] buffer = new byte[Configuration.MAX_DATA_LENGTH];

        //open an output stream to the file
        try (FileOutputStream writer = new FileOutputStream(file)) {
            int num;
            try {
                //keep on reading from the input stream until the remote client/server finishes sending and closes the
                // connection
                while ((num = is.read(buffer)) != -1) {
                    writer.write(buffer, 0, num);
                }
            } catch (IOException e) {
                System.out.println("error receiving file: " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("error writing to file: " + e.getMessage());
        }
    }

}
