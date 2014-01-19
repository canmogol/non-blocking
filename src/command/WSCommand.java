package command;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * acm
 */
public abstract class WSCommand extends ClientCommand {

    public WSCommand(URL url) {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            int port = url.getPort() == -1 ? (url.getProtocol().equals("https") ? 443 : 80) : url.getPort();
            socketChannel.connect(new InetSocketAddress(InetAddress.getByName(url.getHost()), port));
            SelectionKey selectionKey = socketChannel.register(EventLoop.getSelector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
            EventLoop.addCommand(selectionKey, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
