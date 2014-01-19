import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * acm
 */
public class NBClient {

    public static void main(String[] args) {
        new NBClient();
    }

    public NBClient() {
        runClient(new Random().nextInt());
    }

    private void runClient(int clientId) {
        String serverName = "127.0.0.1";
        int port = Integer.parseInt("9091");
        try {

            SocketChannel mySocket = SocketChannel.open();

            // non blocking
            mySocket.configureBlocking(false);

            // connect to a running server
            mySocket.connect(new InetSocketAddress(serverName, port));

            // get a selector
            Selector selector = Selector.open();

            // register the client socket with "connect operation" to the selector
            mySocket.register(selector, SelectionKey.OP_CONNECT);

            // select() blocks until something happens on the underlying socket
            while (selector.select() > 0) {

                Set keys = selector.selectedKeys();
                Iterator it = keys.iterator();

                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    SocketChannel myChannel = (SocketChannel) key.channel();
                    it.remove();

                    if (key.isConnectable()) {
                        /*if (myChannel.isConnectionPending()) {
                            myChannel.finishConnect();
                            System.out.println("pending connection finished!");
                        } else {
                            System.out.println("connection not pending");
                        } */
                        if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                            System.out.println("--- accept");
                        } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                            System.out.println("--- read");
                        } else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                            System.out.println("--- write");
                        }
                        ByteBuffer bb = ByteBuffer.wrap(("clientId: " + clientId).getBytes());
                        myChannel.write(bb);
                        bb.clear();
                    } else {
                        System.out.println("not connectible");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
