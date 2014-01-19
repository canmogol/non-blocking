package command;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * acm
 */
public class EventLoop {

    private static EventLoop instance;
    private Selector selector;
    private Application application;
    private Map<SelectionKey, Command> selectionKeyCommandMap = new ConcurrentHashMap<SelectionKey, Command>();
    private Map<SelectionKey, Map<String, Object>> selectionKeyMap = new ConcurrentHashMap<SelectionKey, Map<String, Object>>();

    public EventLoop(Application application) {
        try {
            selector = Selector.open();
            this.application = application;
        } catch (IOException e) {
            e.printStackTrace();
        }
        instance = this;
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
                        try {
                            ServerSocketChannel ssc = (ServerSocketChannel) selectionKey.channel();
                            SocketChannel sc = ssc.accept();
                            sc.configureBlocking(false);
                            sc.register(selector, SelectionKey.OP_READ);
                            command.onAccept();
                        } catch (Exception e) {
                            log("Exception while accepting, e: " + e.getMessage());
                        }

                    } else if ((selectionKey.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
                        try {
                            command.onConnect();
                            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                            if (selectionKey.isConnectable()) {
                                if (socketChannel.isConnectionPending()) {
                                    socketChannel.finishConnect();
                                }
                            }
                        } catch (Exception e) {
                            log("Exception while connecting, e: " + e.getMessage());
                        }
                    } else if ((selectionKey.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        SocketChannel socketChannel = null;
                        try {
                            byteBuffer.clear();
                            byteBuffer.put(new byte[byteBuffer.limit()]);
                            byteBuffer.clear();
                            socketChannel = (SocketChannel) selectionKey.channel();
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
                                try {
                                    if (command instanceof ServerCommand) {
                                        ServerCommand serverCommand = (ServerCommand) command;
                                        serverCommand.onRead(byteBuffer.array(), selectionKeyMap.get(selectionKey));
                                    } else if (command instanceof ClientCommand) {
                                        ClientCommand clientCommand = (ClientCommand) command;
                                        clientCommand.onRead(byteBuffer.array());
                                    } else {
                                        log("What? there is an unknown onRead() command: " + command);
                                    }
                                    byteBuffer.clear();
                                } catch (Exception e) {
                                    log("Exception while reading, numberOfBytes: " + numberOfBytes + " e: " + e.getMessage());
                                    try {
                                        socketChannel.write(ByteBuffer.wrap("\n\r\n\r".getBytes()));
                                    } catch (IOException ioe) {
                                        log("IOException at socketChannel.write(), numberOfBytes: " + numberOfBytes + " e: " + ioe.getMessage());
                                    }
                                }
                                if (command instanceof ServerCommand) {
                                    socketChannel.register(selector, SelectionKey.OP_WRITE);
                                }
                            }
                        } catch (Exception e) {
                            log("Exception while reading, e: " + e.getMessage());
                            if (socketChannel != null) {
                                try {
                                    socketChannel.write(ByteBuffer.wrap("\n\r\n\r".getBytes()));
                                } catch (IOException ioe) {
                                    log("IOException at socketChannel.write(), general read event, e: " + ioe.getMessage());
                                }
                            }
                        }
                    } else if ((selectionKey.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        try {
                            if (command instanceof ServerCommand) {
                                ServerCommand serverCommand = (ServerCommand) command;
                                serverCommand.doWrite(selectionKeyMap.get(selectionKey), socketChannel);
                            } else if (command instanceof ClientCommand) {
                                ClientCommand clientCommand = (ClientCommand) command;
                                byte[] bytes = clientCommand.doWrite();
                                byte[] bytesToWrite = new byte[bytes.length + 4];
                                System.arraycopy(bytes, 0, bytesToWrite, 0, bytes.length);
                                bytesToWrite[bytes.length] = '\n';
                                bytesToWrite[bytes.length + 1] = '\r';
                                bytesToWrite[bytes.length + 2] = '\n';
                                bytesToWrite[bytes.length + 3] = '\r';
                                ByteBuffer writeByteBuffer = ByteBuffer.wrap(bytesToWrite);
                                try {
                                    socketChannel.write(writeByteBuffer);
                                } catch (IOException ioe) {
                                    log("IOException at socketChannel.write(), clientCommand write, e: " + ioe.getMessage());
                                }
                            } else {
                                log("What? there is an unknown doWrite() command: " + command);
                            }
                        } catch (Exception e) {
                            log("Exception while writing, e: " + e.getMessage());
                            try {
                                socketChannel.write(ByteBuffer.wrap("\n\r\n\r".getBytes()));
                            } catch (IOException ioe) {
                                log("IOException at socketChannel.write(), general write event, e: " + ioe.getMessage());
                            }
                        }
                        selectionKey.interestOps(SelectionKey.OP_READ);
                    }
                }
            } catch (IOException e) {
                try {
                    if (command != null) {
                        command.onError(e);
                    }
                    if (selectionKey != null) {
                        if (selectionKey.channel() != null) {
                            selectionKey.channel().close();
                        }
                        selectionKey.cancel();
                    }
                } catch (Exception ex) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static Selector getSelector() {
        return instance.selector;
    }

    public static void addCommand(SelectionKey selectionKey, Command command) {
        instance.selectionKeyCommandMap.put(selectionKey, command);
    }

    private void log(String log) {
        System.out.println("[" + new Date() + "] " + log);
    }
}
