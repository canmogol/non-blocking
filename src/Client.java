import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

/**
 * acm
 */
public class Client {

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        runClient(new Random().nextInt());
    }

    private void runClient(int clientId) {
        String serverName = "127.0.0.1";
        int port = Integer.parseInt("9091");
        Socket client = null;
        try {
            client = new Socket(serverName, port);
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            out.writeUTF("Hi I'm blocking client");
            out.flush();
            DataInputStream in = new DataInputStream(client.getInputStream());
            System.out.println("server response: " + in.readUTF());
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
