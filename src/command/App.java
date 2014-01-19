package command;

/**
 * acm
 */
public class App {

    public void runAction(Request request, Response response) {
        // lets say this particular request is UserAction:doLogin
        new UserAction().doLogin(request, response);
    }

}
