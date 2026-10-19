package com.ruoyi.framework.manager;

import java.util.TimerTask;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * 异步任务管理器
 *
 * 使用有界队列的专用线程池执行异步任务，避免 ScheduledThreadPoolExecutor
 * 的无界延迟队列在高峰期无限堆积。
 *
 * @author liuhulu
 */
public class AsyncManager
{
    /**
     * 异步操作任务调度线程池
     */
    private ThreadPoolTaskExecutor executor = SpringUtils.getBean("asyncLogExecutor");

    /**
     * 单例模式
     */
    private AsyncManager(){}

    private static AsyncManager me = new AsyncManager();

    public static AsyncManager me()
    {
        return me;
    }

    /**
     * 执行任务
     * 
     * @param task 任务
     */
    public void execute(TimerTask task)
    {
        executor.execute(task);
    }

    /**
     * 停止任务线程池
     */
    public void shutdown()
    {
        executor.shutdown();
    }
}
