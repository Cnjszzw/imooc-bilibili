package com.imooc.bilibili.service.config;


import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    private static ThreadFactory threadFactory ;

    private static ThreadPoolExecutor threadPool ;

    public static ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public static void setThreadFactory(ThreadFactory threadFactory) {
        ThreadPoolConfig.threadFactory = threadFactory;
    }

    public static ThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    public static void setThreadPool(ThreadPoolExecutor threadPool) {
        ThreadPoolConfig.threadPool = threadPool;
    }

    static{
        //创建工厂
         threadFactory = new ThreadFactory() {

            AtomicInteger atomicInteger = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                //创建线程把任务传递进去
                Thread thread = new Thread(r);
                //设置线程名称
                thread.setName("MyThread: "+atomicInteger.getAndIncrement());
//                System.out.println("-------------");
//                System.out.println(thread.getName());
//                System.out.println("-------------");
                return thread;
            }
        };

        //创建线程池
         threadPool = new ThreadPoolExecutor(
                5,
                6,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }



}
