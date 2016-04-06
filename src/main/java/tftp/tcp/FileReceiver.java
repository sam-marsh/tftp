package tftp.tcp;

import tftp.core.Configuration;
import tftp.core.packet.AcknowledgementPacket;

import java.io.*;

/**
 *
 */
public class FileReceiver {

    public static void receive(InputStream is, String file) {
        byte[] buffer = new byte[Configuration.MAX_DATA_LENGTH];
        try (FileOutputStream writer = new FileOutputStream(file)) {
            int num;
            try {
                while ((num = is.read(buffer)) == Configuration.MAX_DATA_LENGTH) {
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
