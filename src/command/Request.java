package command;

import java.util.HashMap;

/**
 * acm
 */
public class Request extends HashMap<String, Object> {

    public static final String RAW_CONTENT = "RAW_CONTENT";

    public String getURL() {
        return "www.something.com:7070/cms/user/login/";
    }

}
