package command;

/**
 * acm
 */
public class UserAction {


    public void doLogin(Request request, Response response) {
        try {
            String username = request.get("username").toString();
            String password = request.get("password").toString();
            validateUsernameAndPassword(username, password);
            doWSLogin(username, password, response);
        } catch (Exception e) {
            response.setStatus(400);
            response.setMessage(e.getMessage());
            response.write();
        }
    }

    private void doWSLogin(final String username, final String password, final Response response) {

        WSCommand wsCommand = new WSCommand(UserWebService.getInstance().getURL()) {
            @Override
            public byte[] doWrite() {
                WSLoginRequest wsLoginRequest = new WSLoginRequest(username, password);
                //System.out.println("will do a WSLoginRequest request");
                return UserWebService.getInstance().getRequestContent(wsLoginRequest);
            }

            @Override
            public void onRead(byte[] bytes) {
                //System.out.println("got content from WS, length: " + bytes.length);
                WSLoginResponse wsLoginResponse = UserWebService.getInstance().read(bytes);
                if (wsLoginResponse.isLogged()) {
                    response.setStatus(200);
                    response.setMessage("this is just a message to client");
                    response.setContent("{\"loginResult\":\"success\"}".getBytes());
                } else {
                    response.setStatus(401);
                    response.setMessage("not logged in");
                    response.setContent("{\"loginResult\":\"failed\"}".getBytes());
                }
                response.write();
            }
        };

    }

    private void validateUsernameAndPassword(String username, String password) throws Exception {
        if (username.isEmpty() || password.isEmpty()) {
            throw new Exception("username and/or password cannot be empty!");
        }
    }
}
