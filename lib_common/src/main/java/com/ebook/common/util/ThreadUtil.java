package com.ebook.common.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {

    private static final String TAG = "ThreadUtil";
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    public static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }



    public static void executeTask(Runnable task) {
        getSingleExecutorService("单个任务").execute(task);
    }
    public static void executeTaskOnUiThread(Runnable task) {
        uiHandler.post(task);
    }

    public static void executeTaskOnUiThread(Runnable task,int delay) {
        uiHandler.postDelayed(task,delay);
    }

    public static ExecutorService getThreadPoolExecutorService(String executorName) {
        return new ThreadPoolExecutor(2, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, executorName + "-" + System.currentTimeMillis());
            }
        }, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                executor.submit(r);
            }
        });
    }

    public static ExecutorService getNewThreadExecutorService(String executorName) {
        return Executors.newScheduledThreadPool(3,new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, executorName + "-" + System.currentTimeMillis());
            }
        });
    }

    public static ExecutorService getSingleExecutorService(String executorName) {
       return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, executorName + "-" + System.currentTimeMillis());
            }
        });
    }

}