package command;

/**
 * acm
 */
public abstract class BaseCommand implements Command {

    @Override
    public void onAccept() {
        System.out.println("ON_ACCEPT");
    }

    @Override
    public void onConnect() {
        System.out.println("ON_CONNECT");
    }

    @Override
    public void onEmptyRead() {
        System.out.println("ON_EMPTY_READ");
    }

    @Override
    public void onError(Exception e) {
        System.out.println("Exception e: " + e);
        e.printStackTrace();
    }

    @Override
    public void onClose() {
        System.out.println("ON_CLOSE");
    }

}
