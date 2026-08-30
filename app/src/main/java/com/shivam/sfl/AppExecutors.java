package com.shivam.sfl;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small shared executor so database writes/reads and reverse-geocoding can run off the
 * main thread. The app is tiny enough that a single background thread plus a main-thread
 * handler is enough - no need to pull in WorkManager/coroutines/RxJava for this.
 */
public final class AppExecutors {

    private static final AppExecutors INSTANCE = new AppExecutors();

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static AppExecutors getInstance() {
        return INSTANCE;
    }

    public void runOnBackground(Runnable runnable) {
        backgroundExecutor.execute(runnable);
    }

    public void runOnMainThread(Runnable runnable) {
        mainThreadHandler.post(runnable);
    }
}
