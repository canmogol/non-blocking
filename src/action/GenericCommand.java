package action;

/**
 * acm
 */
public abstract class GenericCommand extends BaseCommand {

    protected GenericCommand() {
    }

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

}
