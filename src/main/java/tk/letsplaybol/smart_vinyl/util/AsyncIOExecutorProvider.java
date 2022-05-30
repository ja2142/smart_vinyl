package tk.letsplaybol.smart_vinyl.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// most async actions here are io bound with lots of waiting and not much cpuing
// using commonPool for them not only runs just couple of them in parallel, it also can clog up
// all threads for any other mod or whatever uses it
// also that has to be one of the most java names I've came up with (shame it's not a factory of providers of managers though)
public class AsyncIOExecutorProvider {
    private static ExecutorService ioExecutor;

    public static synchronized ExecutorService getExecutor() {
        if (ioExecutor == null) {
            ioExecutor = Executors.newCachedThreadPool();
        }
        return ioExecutor;
    }
}
