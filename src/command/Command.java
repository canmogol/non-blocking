package command;

/**
 * acm
 */
public interface Command {

    /* events */

    public void onConnect();

    public void onAccept();

    public void onEmptyRead();

    public void onClose();

    public void onError(Exception e);

}
