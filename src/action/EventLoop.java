package action;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * acm
 */
public class EventLoop {

    private Application application;
    private Selector selector;
    private Map<SelectionKey, Command> selectionKeyCommandMap = new ConcurrentHashMap<SelectionKey, Command>();
    private Map<SelectionKey, Map<String, Object>> selectionKeyMap = new ConcurrentHashMap<SelectionKey, Map<String, Object>>();

    public EventLoop(Application application) {
        try {
            selector = Selector.open();
            this.application = application;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runLoop() {
        application.runApplication();
        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        while (true) {
            Command command = null;
            SelectionKey selectionKey = null;
            try {
                // .select() will block
                selector.select();
                Set keys = selector.selectedKeys();
                Iterator it = keys.iterator();
                while (it.hasNext()) {
                    // do a little cleaning
                    command = null;
                    selectionKey = null;
                    selectionKey = (SelectionKey) it.next();
                    if (!selectionKeyMap.containsKey(selectionKey)) {
                        selectionKeyMap.put(selectionKey, new HashMap<String, Object>());
                    }
                    boolean connect = ((selectionKey.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT);
                    boolean accept = ((selectionKey.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT);
                    boolean read = ((selectionKey.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ);
                    boolean write = ((selectionKey.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE);
                    it.remove();
                    if (!selectionKey.isValid()) {
                        break;
                    }

                    // find command
                    if (selectionKeyCommandMap.containsKey(selectionKey)) {
                        command = selectionKeyCommandMap.get(selectionKey);
                    }
                    // if no command found and this is an read event
                    // it may be a client connected, write something and waiting for a response
                    if (command == null && (read || write) && selectionKey.channel() instanceof SocketChannel &&
                            (((SocketChannel) selectionKey.channel()).getLocalAddress()) instanceof InetSocketAddress) {
                        for (SelectionKey key : selectionKeyCommandMap.keySet()) {
                            if (key.channel() instanceof ServerSocketChannel &&
                                    (((ServerSocketChannel) key.channel()).getLocalAddress()) instanceof InetSocketAddress) {
                                int commandPort = ((InetSocketAddress) ((ServerSocketChannel) key.channel()).getLocalAddress()).getPort();
                                int requestedPort = ((InetSocketAddress) (((SocketChannel) selectionKey.channel()).getLocalAddress())).getPort();
                                if (commandPort == requestedPort) {
                                    command = selectionKeyCommandMap.get(key);
                                    selectionKeyCommandMap.put(selectionKey, command);
                                    break;
                                }
                            }
                        }
                    }

                    // if no command found, simply ignore this request
                    if (command == null) {
                        continue;
                    }

                    // below are the events
                    if ((selectionKey.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        ServerSocketChannel ssc = (ServerSocketChannel) selectionKey.channel();
                        SocketChannel sc = ssc.accept();
                        sc.configureBlocking(false);
                        sc.register(selector, SelectionKey.OP_READ);
                        command.onAccept();

                    } else if ((selectionKey.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
                        command.onConnect();
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        if (selectionKey.isConnectable()) {
                            if (socketChannel.isConnectionPending()) {
                                socketChannel.finishConnect();
                            }
                        }
                    } else if ((selectionKey.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        byteBuffer.clear();
                        byteBuffer.put(new byte[byteBuffer.limit()]);
                        byteBuffer.clear();
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        if (selectionKey.isConnectable()) {
                            if (socketChannel.isConnectionPending()) {
                                socketChannel.finishConnect();
                            }
                        }
                        int numberOfBytes = socketChannel.read(byteBuffer);
                        if (numberOfBytes == 0) {
                            command.onEmptyRead();
                            break;
                        } else if (numberOfBytes == -1) {
                            socketChannel.close();
                            command.onClose();
                            Command removedCommand = selectionKeyCommandMap.remove(selectionKey);
                            Map<String, Object> map = selectionKeyMap.remove(selectionKey);
                            //System.out.println("---- execution done, socket closed, command removed: " + removedCommand + " #commands: " + selectionKeyCommandMap.size());
                            //System.out.println("---- execution done, socket closed, map removed: " + map + " #maps: " + selectionKeyMap.size());
                            if (selectionKeyCommandMap.size() == 0) {
                                System.exit(0);
                            }
                            break;
                        } else if (numberOfBytes > 0) {
                            byteBuffer.flip();
                            command.onRead(byteBuffer.array(), selectionKeyMap.get(selectionKey));
                            byteBuffer.clear();
                            if (command instanceof ServerCommand) {
                                socketChannel.register(selector, SelectionKey.OP_WRITE);
                            }
                        }
                        //socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                    } else if ((selectionKey.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                        /*
                        // if command is a ServerCommand
                                 Map<String, Object> map = selectionKeyMap.get(selectionKey);
                                command.doWrite(map);
                         */
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        command.doWrite(selectionKey.attachment(), socketChannel);

                        selectionKey.interestOps(SelectionKey.OP_READ);
                    }
                }
            } catch (IOException e) {
                try {
                    if (command != null) {
                        command.onError(e);
                    }
                    if (selectionKey != null) {
                        selectionKey.channel().close();
                        selectionKey.cancel();
                    }
                } catch (Exception ex) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Selector getSelector() {
        return selector;
    }

    public void addCommand(SelectionKey selectionKey, Command command) {
        selectionKeyCommandMap.put(selectionKey, command);
    }
}
