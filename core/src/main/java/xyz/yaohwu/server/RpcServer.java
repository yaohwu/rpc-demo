package xyz.yaohwu.server;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import xyz.yaohwu.exception.RpcException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yaoh.wu
 */
public class RpcServer implements Server {
    private int nThreads = 10;
    private boolean isAlive = false;
    private int port = 8989;
    private ExecutorService executor;


    public RpcServer(int port, int nThreads) {
        this.port = port;
        this.nThreads = nThreads;
        init();
    }

    private void init() {
        executor = new ThreadPoolExecutor(5, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(5),
                new ThreadFactoryBuilder().setNameFormat("server-thread-%s").build());
    }

    @Override
    public void start() {
        isAlive = true;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("RPC服务启动成功...");
            //noinspection InfiniteLoopStatement
            while (true) {
                executor.execute(new RpcRequestHandlerTask(serverSocket.accept()));
                System.out.println("执行一次响应.." + new Date());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void stop() {
        isAlive = false;
        executor.shutdown();
    }

    @Override
    public void register(String name, Class clazz) throws Exception {
        if (RegisterServicesCenter.getRegisterServices() != null) {
            RegisterServicesCenter.getRegisterServices().put(name, clazz);
        } else {
            throw new RpcException("RPC服务未初始化");
        }
    }

    @Override
    public boolean isAlive() {
        String status = (this.isAlive) ? "RPC服务已经启动" : "RPC服务已经关闭";
        System.out.println(status);
        return this.isAlive;
    }
}
