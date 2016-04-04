package tftp.tcp.server;

import tftp.core.Configuration;
import tftp.core.TFTPException;
import tftp.core.packet.ReadRequestPacket;
import tftp.core.packet.TFTPPacket;
import tftp.core.packet.WriteRequestPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
        AsynchronousServerSocketChannel mainChannel;

        try {
            mainChannel = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));
            mainChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        } catch (IOException e) {
            System.err.println("unable to start server: " + e.getMessage());
            return;
        }

        while (true) {

            Future<AsynchronousSocketChannel> future = mainChannel.accept();

            try (AsynchronousSocketChannel workerChannel = future.get()) {

                byte[] array = new byte[Configuration.MAX_PACKET_LENGTH];
                ByteBuffer buffer = ByteBuffer.wrap(array);

                workerChannel.read(buffer, null, new CompletionHandler<Integer, Object>() {

                    @Override
                    public void completed(Integer result, Object attachment) {
                        if (result != -1) {
                            try {
                                TFTPPacket packet = TFTPPacket.fromByteArray(array, result);

                                if (packet instanceof ReadRequestPacket) {

                                    ByteBuffer out = ByteBuffer.allocate(Configuration.MAX_DATA_LENGTH);

                                    try {
                                        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(Paths.get(
                                                ((ReadRequestPacket) packet).getFileName()
                                        ));
                                        new AsynchronousFileWriter(fileChannel, workerChannel, out).run();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else if (packet instanceof WriteRequestPacket) {

                                    ByteBuffer in = ByteBuffer.allocate(Configuration.MAX_DATA_LENGTH);

                                    try {
                                        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(Paths.get(
                                                ((WriteRequestPacket) packet).getFileName()
                                        ));
                                        new AsynchronousFileReader(fileChannel, workerChannel, in).run();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else {
                                    System.out.println("unexpected packet received: " + packet + ", ignoring");
                                }

                            } catch (TFTPException e) {
                                System.out.println("error parsing packet: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        System.out.println("failed to read from channel: " + exc.getMessage());
                    }

                });

            } catch (InterruptedException | IOException | ExecutionException e) {
                System.out.println("error opening/closing channel: " + e);
            }

        }

    }

    public static class AsynchronousFileWriter implements Runnable {

        private final AsynchronousFileChannel in;
        private final AsynchronousSocketChannel out;
        private final ByteBuffer buffer;

        AsynchronousFileWriter(AsynchronousFileChannel in, AsynchronousSocketChannel out, ByteBuffer buffer) {
            this.in = in;
            this.out = out;
            this.buffer = buffer;
        }

        @Override
        public void run() {
            in.read(buffer, 0, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    buffer.flip();
                    if (result != -1) {
                        out.write(buffer, null, new CompletionHandler<Integer, Object>() {
                            @Override
                            public void completed(Integer result, Object attachment) {
                                if (result == Configuration.MAX_DATA_LENGTH) {
                                    new AsynchronousFileWriter(in, out, buffer).run();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, Object attachment) {
                                System.out.println("failed to send file: " + exc.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    System.out.println("failed to read from file: " + exc.getMessage());
                }

            });
        }
    }

    public static class AsynchronousFileReader implements Runnable {

        private final AsynchronousFileChannel out;
        private final AsynchronousSocketChannel in;
        private final ByteBuffer buffer;

        AsynchronousFileReader(AsynchronousFileChannel out, AsynchronousSocketChannel in, ByteBuffer buffer) {
            this.out = out;
            this.in = in;
            this.buffer = buffer;
        }

        @Override
        public void run() {
            in.read(buffer, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    buffer.flip();

                    if (result != -1) {
                        out.write(buffer, 0, null, new CompletionHandler<Integer, Object>() {
                            @Override
                            public void completed(Integer result, Object attachment) {
                                if (result == Configuration.MAX_DATA_LENGTH) {
                                    new AsynchronousFileReader(out, in, buffer).run();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, Object attachment) {
                                System.out.println("failed to send file: " + exc.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    System.out.println("failed to read from channel: " + exc.getMessage());
                }
            });
        }
    }

    public static void main(String[] args) {
        Thread server = new TFTPTCPServer(Configuration.DEFAULT_SERVER_PORT);
        server.start();
    }

}
