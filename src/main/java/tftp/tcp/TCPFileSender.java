package tftp.tcp;

import tftp.core.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A utility class for sending a file over TCP.
 */
public class TCPFileSender {

    public static void send(OutputStream os, String fileName) {
        byte[] buffer = new byte[Configuration.MAX_DATA_LENGTH];
        try (FileInputStream reader = new FileInputStream(fileName)) {
            int num;
            try {
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

}
