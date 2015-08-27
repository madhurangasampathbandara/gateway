package org.wso2.carbon.transport.http.netty.common;


import org.apache.log4j.Logger;
import org.wso2.carbon.mediation.controller.POCController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerPool {
    private static Logger log = Logger.getLogger(WorkerPool.class);

    private static WorkerPool instance = new WorkerPool();
    private static ExecutorService executorService ;


    private WorkerPool() {
        log.info("### Executor Worker count: " + POCController.props.getProperty("workers", "300"));
        executorService = Executors.newFixedThreadPool(Integer.valueOf(
                POCController.props.getProperty("workers", "300")));
    }

    public static WorkerPool getInstance() {
        return instance;
    }

    public static void submitJob(Runnable runnable) {
        executorService.submit(runnable);
    }
}