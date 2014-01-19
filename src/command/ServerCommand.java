package command;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * acm
 */
public abstract class ServerCommand extends BaseCommand {

    protected ServerCommand(int listenPort) {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            InetSocketAddress address = new InetSocketAddress(listenPort);
            ServerSocket ss = ssc.socket();
            ss.bind(address);
            SelectionKey key = ssc.register(EventLoop.getSelector(), SelectionKey.OP_ACCEPT);
            EventLoop.addCommand(key, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract void doWrite(Map<String, Object> map, SocketChannel socketChannel);

    public abstract void onRead(byte[] bytes, Map<String, Object> map);

}
