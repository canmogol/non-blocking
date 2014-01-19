package action;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * acm
 */
public class Application {
    private EventLoop eventLoop;

    public void start() {
        // start event loop
        eventLoop = new EventLoop(this);
        eventLoop.runLoop();
    }

    public void runApplication() {
        runInboundTest();
        //runOutboundTest();
    }

    private void runInboundTest() {
        try {
            class Request {
                public String getURL() {
                    return "www.something.com:7070/cms/user/login/";
                }
            }
            class Response {
                public byte[] toBytes() {
                    return new byte[0];
                }
            }
            class ConnectionHandler {
                public Request parseRequest(byte[] bytes) {
                    return new Request();
                }
            }
            class App {
                public Response runAction(Request req) {
                    return new Response();
                }
            }
            class ApplicationHandler {
                public App findApplication(String url) {
                    return new App();
                }
            }
            // this will listen to the connections that are inbound which means this will handle the connections from clients to server
            ServerCommand loginCommand = new ServerCommand() {

                private ConcurrentHashMap<String, Application> applicationsMap = new ConcurrentHashMap<String, Application>();
                private ConcurrentHashMap<String, ClassLoader> classLoaderMap = new ConcurrentHashMap<String, ClassLoader>();
                private ConcurrentHashMap<String, String> applicationPathMap = new ConcurrentHashMap<String, String>();
                private ConnectionHandler connectionHandler = new ConnectionHandler();
                private ApplicationHandler applicationHandler = new ApplicationHandler();
                ExecutorService pool = Executors.newCachedThreadPool();

                public void doWrite(Object attachment, final SocketChannel socketChannel) {
                    // the attachment is the Request object
                    final Request request = (Request) attachment;
                    // first get a hold on the App,
                    // this is a singleton instance of the app that is responsible for this request
                    final App app = applicationHandler.findApplication(request.getURL());// app is singleton

                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // run the corresponding action method, something like below example;
                                // (Response)UserAction::doLogin(Request request)
                                Response response = app.runAction(request);
                                // return the bytes to client
                                byte[] bytes = response.toBytes();
                                byte[] bytesToWrite = new byte[bytes.length + 4];
                                System.arraycopy(bytes, 0, bytesToWrite, 0, bytes.length);
                                bytesToWrite[bytes.length] = '\n';
                                bytesToWrite[bytes.length + 1] = '\r';
                                bytesToWrite[bytes.length + 2] = '\n';
                                bytesToWrite[bytes.length + 3] = '\r';
                                ByteBuffer writeByteBuffer = ByteBuffer.wrap(bytesToWrite);
                                socketChannel.write(writeByteBuffer);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    // since this is a request->response server operation
                                    // try to close the socket channel
                                    socketChannel.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    // event loop will never block
                }

                public void onRead(byte[] bytes, Object attachment) {
                    // Request object will be the attachment of this selectionKey
                    attachment = connectionHandler.parseRequest(bytes);
                }

            };
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            InetSocketAddress address = new InetSocketAddress(9091);
            ServerSocket ss = ssc.socket();
            ss.bind(address);
            SelectionKey key = ssc.register(eventLoop.getSelector(), SelectionKey.OP_ACCEPT);
            eventLoop.addCommand(key, loginCommand);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runOutboundTest() {
        // this will get the contents of two web pages, simulating two web service requests
        try {
            /*
            //
            // add a request
            //
            ClientCommand redminePageRequest = new ClientCommand() {
                @Override
                public byte[] doWrite(Map<String, Object> map) {
                    System.out.println("CLIENT COMMAND DO_WRITE");
                    return ("GET / HTTP/1.1\nhost: www.hostname.com\n\n").getBytes();
                }

                @Override
                public void onRead(byte[] bytes, Map<String, Object> map) {
                    System.out.println("CLIENT COMMAND DO_READ");
                    String response = new String(bytes);
                    System.out.println(response);
                }
            };
            SocketChannel socketChannel1 = SocketChannel.open();
            socketChannel1.configureBlocking(false);
            socketChannel1.connect(new InetSocketAddress(InetAddress.getByName("10.10.10.61"), 80));
            SelectionKey selectionKey1 = socketChannel1.register(eventLoop.getSelector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
            eventLoop.addCommand(selectionKey1, redminePageRequest);
             */
            /*
            for (int i = 0; i < 100; i++) {
                ClientCommand clientCommand = new ClientCommand() {
                    @Override
                    public void doWrite(Object attachment, SocketChannel socketChannel) {
                        byte[] bytes = ("GET / HTTP/1.1\nhost: hb.innova.com.tr\n\r\n\r").getBytes();
                        byte[] bytesToWrite = new byte[bytes.length + 4];
                        System.arraycopy(bytes, 0, bytesToWrite, 0, bytes.length);
                        bytesToWrite[bytes.length] = '\n';
                        bytesToWrite[bytes.length + 1] = '\r';
                        bytesToWrite[bytes.length + 2] = '\n';
                        bytesToWrite[bytes.length + 3] = '\r';
                        ByteBuffer writeByteBuffer = ByteBuffer.wrap(bytesToWrite);
                        socketChannel.write(writeByteBuffer);
                    }

                    @Override
                    public void onRead(byte[] bytes, Object o) {
                        String response = new String(bytes);
                        System.out.println(response);
                    }
                };
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(new InetSocketAddress(InetAddress.getByName("172.16.14.15"), 80));
                SelectionKey selectionKey = socketChannel.register(eventLoop.getSelector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
                eventLoop.addCommand(selectionKey, clientCommand);
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runAppTest() {
        /*
        try {
            // this will listen to the connections that are inbound which means this will handle the connections from clients to server
            ServerCommand command = new ServerCommand() {


                @Override
                public byte[] doWrite(Object o) {
                    Map<String, Object> map = (Map<String, Object>) o;
                    System.out.println("SERVER COMMAND DO_WRITE");
                    return (map.get("login").toString()).getBytes();
                }

                @Override
                public void onRead(byte[] bytes, Object o) {
                // read bytes and create request object
                    Request req = createRequestFromBuffer(byteBuffer)
                // find the app
                    App app = findApplication(req.getUri()); // singleton APP
                //find the action.method for this request
                    app.runApplication(req, map);
                // and will continue
                }
            };
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ServerSocket ss = ssc.socket();
            InetSocketAddress address = new InetSocketAddress(9090);
            ss.bind(address);
            SelectionKey key = ssc.register(eventLoop.getSelector(), SelectionKey.OP_ACCEPT);
            eventLoop.addCommand(key, command);

        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }



                                                /*
                                                // implementation of runApplication method
                                                App.runApplication(Request req){
                                                        Action action = app.getAction(req.getUri());
                                                        Method method = action.getMethod(req.getUri());
                                                        ResponseType rt = method.getResponseType();
                                                        if(rt instanceof Command){
                                                            Command command = (Command)method.invoke(action, req);
                                                            selectionKeyCommandMap.put(selectionKey, command);
                                                            command.onRead(req);
                                                            byteBuffer.clear();
                                                        }else{
                                                            //ExecutorService pool = Executors.newFixedThreadPool(maximumThreadCount);
                                                            pool.execute(new MethodHandler(){
                                                                @Override
                                                                public void run() {
                                                                    Response resp = method.invoke(action, req);
                                                                    writeResponseHeadersAndSendResponseBack(resp);
                                                                }
                                                            });

                                                        }
                                                }
                                                 */
}
