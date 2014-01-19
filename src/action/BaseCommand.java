package action;

/**
 * acm
 */
public abstract class BaseCommand implements Command {

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
