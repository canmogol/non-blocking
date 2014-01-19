package blocking;

/**
 * acm
 */
public class BlockingTest {

    public static void main(String[] args) {
        new BlockingTest();
    }

    public BlockingTest() {
        Blocking blocking = new Blocking();
        blocking.start();
    }
}
