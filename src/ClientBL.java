import java.io.*;
import java.net.Socket;

/**
 * acm
 */
public class ClientBL {


    public static void main(String[] args) {
        new ClientBL();
    }

    public ClientBL() {
        for (int i = 0; i < 100; i++) {
            final int finalI = i;
            new Thread() {
                @Override
                public void run() {
                    doRequest(finalI);
                }
            }.start();
        }
    }

    private void doRequest(int i) {
        Socket socket = null;
        String message;
        try {
            socket = new Socket("localhost", 9091);
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            OutputStreamWriter osw = new OutputStreamWriter(bos);
            osw.write("acm:123\r");
            osw.flush();

            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            InputStreamReader isr = new InputStreamReader(bis);
            StringBuilder instr = new StringBuilder();
            int c;
            while ((c = isr.read()) != 13) {
                instr.append((char) c);
            }
            System.out.println(i + ": response size: " + instr.toString().length());
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
