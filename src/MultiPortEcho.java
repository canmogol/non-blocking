import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MultiPortEcho {
    private int ports[];
    private ByteBuffer echoBuffer = ByteBuffer.allocate(1024);
    private List<SocketChannel> socketChannels = new ArrayList<SocketChannel>();

    class Request {
    }

    public MultiPortEcho(int ports[]) throws IOException {
        this.ports = ports;
        configureSelector();
        try {
            onFinish(
                    MultiPortEcho.class,
                    MultiPortEcho.class.getMethod("doSomething", Request.class)
            );
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void onFinish(Class<?> clazz, Method method) {

    }

    public static void doSomething(Request request) {

    }

    public void configureSelector() throws IOException {
        // Create a new selector
        Selector selector = Selector.open();

        // Open a listener on each port, and register each one
        // with the selector
        for (int i = 0; i < ports.length; ++i) {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ServerSocket ss = ssc.socket();
            InetSocketAddress address = new InetSocketAddress(ports[i]);
            ss.bind(address);

            SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Going to listen on " + ports[i]);
        }

        while (true) {
            int num = selector.select();

            Set selectedKeys = selector.selectedKeys();
            Iterator it = selectedKeys.iterator();

            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                if (!key.isValid()) {
                    it.remove();
                    break;
                }

                if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                    // Accept the new connection
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);

                    // Add the new connection to the selector
                    SelectionKey newKey = sc.register(selector, SelectionKey.OP_READ);
                    it.remove();
                    socketChannels.add(sc);
                    System.out.println("Got connection from " + sc);
                } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                    // Read the data
                    SocketChannel sc = (SocketChannel) key.channel();

                    while (true) {
                        echoBuffer.clear();
                        echoBuffer.put(new byte[echoBuffer.limit()]);
                        echoBuffer.clear();

                        int numberOfBytes = sc.read(echoBuffer);

                        if (numberOfBytes == 0) {
                            //System.out.println("read done, will break");
                            break;
                        } else if (numberOfBytes == -1) {
                            System.out.println("will close");
                            socketChannels.remove(sc);
                            sc.close();
                            break;
                        } else if (numberOfBytes > 0) {
                            echoBuffer.flip();
                            //sc.write(ByteBuffer.wrap("OK".getBytes()));
                            String message = new String(echoBuffer.array());
                            if (!message.startsWith("login")) {
                                for (SocketChannel socketChannel : socketChannels) {
                                    echoBuffer.rewind();
                                        socketChannel.write(echoBuffer);
                                    if (!socketChannel.equals(sc)) {
                                    }
                                }
                            }
                        }
                    }
                    it.remove();
                } else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                    System.out.println("---------- write now!");
                }
            }
        }
    }

    static public void main(String args[]) throws Exception {
        args = new String[]{"9091", "9092", "9093"};
        if (args.length <= 0) {
            System.err.println("Usage: java MultiPortEcho port [port port ...]");
            System.exit(1);
        }

        int ports[] = new int[args.length];

        for (int i = 0; i < args.length; ++i) {
            ports[i] = Integer.parseInt(args[i]);
        }

        new MultiPortEcho(ports);
    }
}