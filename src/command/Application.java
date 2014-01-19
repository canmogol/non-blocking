package command;

import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * acm
 */
public class Application {

    public void start() {
        // start event loop
        new EventLoop(this).runLoop();
    }

    public void runApplication() {
        try {

            // this will listen to the connections that are inbound which means this will handle the connections from clients to server
            ServerCommand loginCommand = new ServerCommand(9091) {
                private ConnectionHandler connectionHandler = new ConnectionHandler();
                private ApplicationHandler applicationHandler = new ApplicationHandler();

                @Override
                @SuppressWarnings("unchecked")
                public void doWrite(Map<String, Object> map, SocketChannel socketChannel) {
                    Request request = (Request) map.get(Request.class.getName());
                    Response response = new Response(socketChannel);

                    // first get a hold on the App,
                    // this is a singleton instance of the app that is responsible for this request
                    App app = applicationHandler.findApplication(request.getURL());// app is singleton
                    // run the corresponding action method, something like below example;
                    // (void)UserAction::doLogin(Request request, Response response)
                    app.runAction(request, response);
                    // event loop will never block
                }

                @Override
                public void onRead(byte[] bytes, Map<String, Object> map) {
                    map.put(Request.class.getName(), connectionHandler.parseRequest(bytes));
                }

            };

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
