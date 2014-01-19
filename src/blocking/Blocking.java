package blocking;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * acm
 */
public class Blocking {

    public void start() {
        runInboundTest();
        //runOutboundTest();
    }

    private void runInboundTest() {
        try {
            ServerSocket serverSocket = new ServerSocket(9091);
            while (true) {
                final Socket socket = serverSocket.accept(); // will block and wait for a client
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            System.out.println(Thread.currentThread().getId() + ": ON_ACCEPT");
                            // read request
                            InputStream inputStream = socket.getInputStream();
                            StringBuilder stringBuilder = new StringBuilder();
                            int ch;
                            while ((ch = inputStream.read()) != '\r') {
                                char currentChar = (char) ch;
                                stringBuilder.append(currentChar);
                            }
                            // same logic with non-blocking
                            String message = stringBuilder.toString();
                            System.out.println("SERVER COMMAND DO_READ: " + message);
                            String response;
                            if (message.equals("acm:123")) {
                                response = "true\r";
                            } else {
                                response = "false\r";
                            }
                            // write response
                            OutputStream outputStream = socket.getOutputStream();
                            outputStream.write(response.getBytes());
                            outputStream.flush();
                            System.out.println("SERVER COMMAND DO_WRITE");
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                socket.close();
                                System.out.println("ON_CLOSE");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runOutboundTest() {
        for (int i = 0; i < 100; i++) {
            new Thread() {
                @Override
                public void run() {
                    Socket socket = null;
                    String message;
                    try {
                        socket = new Socket("172.16.14.15", 80);
                        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                        OutputStreamWriter osw = new OutputStreamWriter(bos);
                        osw.write("GET / HTTP/1.1\nhost: hb.innova.com.tr\n\n");
                        osw.flush();

                        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                        InputStreamReader isr = new InputStreamReader(bis);
                        StringBuilder instr = new StringBuilder();
                        int c;
                        while ((c = isr.read()) != -1) {
                            instr.append((char) c);
                        }
                        System.out.println("response: " + instr);
                        socket.close();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

}
