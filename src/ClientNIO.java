import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class ClientNIO {

    private SocketChannel sc;

    public static void main(String args[]) {
        new ClientNIO();
    }

    public ClientNIO() {
        start();
    }

    private void start() {
        InetSocketAddress ISA = null;
        SocketChannel socketChannel = null;
        Selector selector = null;
        SelectionKey key = null;
        ByteBuffer clientBuf = ByteBuffer.allocate(1024);

        int port = 9091;

        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        try {
            InetAddress addr = InetAddress.getByName("localhost");
            ISA = new InetSocketAddress(addr, port);
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        }

        try {
            socketChannel.connect(ISA);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        try {
            /*socketChannel.validOps()*/
            SelectionKey clientKey = socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        final int clientId = new Random().nextInt();
        // UI Thread simulation
        new Thread() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    while (true) {
                        //Thread.sleep(10000);
                        System.out.println("type something:");
                        Scanner keyboard = new Scanner(System.in);
                        String line = keyboard.nextLine();
                        if (sc != null) {
                            ByteBuffer serverBuf = ByteBuffer.wrap(line.getBytes());
                            sc.write(serverBuf);
                            serverBuf.clear();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        try {
            while (true) {
                selector.select();
                Set keys = selector.selectedKeys();
                Iterator it = keys.iterator();
                while (it.hasNext()) {
                    key = (SelectionKey) it.next();
                    it.remove();

                    boolean connect = ((key.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT);
                    boolean accept = ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT);
                    boolean read = ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ);
                    boolean write = ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE);

                    sc = (SocketChannel) key.channel();
                    if (!key.isValid()) {
                        break;
                    } else if (key.isConnectable()) {
                        if (sc.isConnectionPending()) {
                            sc.finishConnect();
                        }
                    }

                    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        System.out.println("OP_ACCEPT");
                    } else if ((key.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
                        System.out.println("OP_CONNECT");
                    } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        System.out.println("OP_READ");
                        clientBuf.clear();
                        clientBuf.put(new byte[clientBuf.limit()]);
                        clientBuf.clear();
                        int numberOfBytes = sc.read(clientBuf);
                        if (numberOfBytes == 0) {
                            System.out.println("nothing to read, will break");
                            break;
                        } else if (numberOfBytes == -1) {
                            System.out.println("will close");
                            sc.close();
                            break;
                        } else if (numberOfBytes > 0) {
                            clientBuf.flip();
                            String message = new String(clientBuf.array()).trim();
                            System.out.println(">>>" + message);
                            clientBuf.clear();
                        }

                    } else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                        System.out.println("OP_WRITE");
                        ByteBuffer serverBuf = ByteBuffer.wrap(("login:user:" + clientId).getBytes());
                        sc.write(serverBuf);
                        serverBuf.clear();
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            try {
                key.channel().close();
                key.cancel();
            } catch (Exception ex) {
                System.out.println(e.getMessage());
            }
        }

    }
}