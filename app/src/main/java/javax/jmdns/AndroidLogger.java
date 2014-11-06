package javax.jmdns;

/**
 * Created by homer on 03/10/14.
 */
public class AndroidLogger {
    private static final boolean DEBUG_LOG = true;

    public static int d(String tag, String message) {
        if(DEBUG_LOG) {
            return android.util.Log.d(tag, message);
        }

        return 0;
    }

    public static int e(String tag, String msg) {
        return android.util.Log.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable t) {
        return android.util.Log.e(tag, msg, t);
    }

    public static int w(String tag, String msg) {
        return android.util.Log.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable t) {
        return android.util.Log.w(tag, msg, t);
    }
}
