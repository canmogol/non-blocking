package command;

import java.util.HashMap;
import java.util.Map;

/**
 * acm
 */
public class ApplicationHandler {

    private Map<String, App> urlAppMap = new HashMap<String, App>();

    public App findApplication(String url) {
        if (!urlAppMap.containsKey(url)) {
            urlAppMap.put(url, new App());
        }
        return urlAppMap.get(url);
    }
}
