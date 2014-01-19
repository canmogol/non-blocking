package command;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * acm
 */
public class UserWebService {

    private static UserWebService instance;

    private UserWebService() {
    }

    public static UserWebService getInstance() {
        if (instance == null) {
            instance = new UserWebService();
        }
        return instance;
    }

    public URL getURL() {
        try {
            return new URL("http://172.16.14.15/");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public WSLoginResponse read(byte[] bytes) {
        return new WSLoginResponse();
    }

    public byte[] getRequestContent(Object request) {
        // here it will return a request with proper headers and XML content as byte array for this WS method call
        return ("GET / HTTP/1.1\nhost: hb.innova.com.tr\n\r\n\r").getBytes();
    }
}
