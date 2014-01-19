package command;

/**
 * acm
 */
public abstract class ClientCommand extends BaseCommand {

    public abstract byte[] doWrite();

    public abstract void onRead(byte[] bytes);

}
