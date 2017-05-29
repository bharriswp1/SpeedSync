package info.brandonharris.speedsync;

/**
 * Created by brandon on 5/17/17.
 */

public class Constants {
    public static String COMPUTER_ADDRESS = "address";
    public static String PREFERENCES = "settings";
    public static String BACKUP_DIRECTORY = "backupDirectory";
    public static String FUNCTION_EXTRA = "function";

    public static int SERVICE_PORT = 8523;
    public static int BUFFER_SIZE = 1024*8;

    public static class Functions {
        public final static int SEND_FILE = 1;
        public final static int LIST_FILES = 2;
        public final static int RECEIVE_FILE = 3;
        public final static int GET_SIZE = 4;
        public final static int GET_ACTION_LIST = 5;
        public final static int TEST = 127;
    }
}