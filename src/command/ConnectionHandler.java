package command;

/**
 * acm
 */
public class ConnectionHandler {

    public Request parseRequest(byte[] bytes) {
        Request request = new Request();
        request.put(Request.RAW_CONTENT, bytes);
        String clientRequest = new String(bytes).trim();
        String[] usernameAndPassword = clientRequest.split(":");
        if (usernameAndPassword.length == 2) {
            request.put("username", usernameAndPassword[0]);
            request.put("password", usernameAndPassword[1]);
        }
        return request;
    }

}
