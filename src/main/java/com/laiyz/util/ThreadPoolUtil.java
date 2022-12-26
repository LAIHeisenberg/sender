package com.laiyz.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {

    private static ExecutorService executorService = new ThreadPoolExecutor(10,20,120, TimeUnit.SECONDS,new LinkedBlockingDeque(20));

    public static void submitTask(Runnable task){
        executorService.submit(task);
    }
}
