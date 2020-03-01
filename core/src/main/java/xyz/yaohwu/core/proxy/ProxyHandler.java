package xyz.yaohwu.core.proxy;


import xyz.yaohwu.core.RpcContext;
import xyz.yaohwu.core.RpcThreadFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 动态代理处理程序
 *
 * @author yaoh.wu
 */
public class ProxyHandler implements InvocationHandler {

    /**
     * 超时等待时间
     */
    private static final long TIMEOUT = 10000L;
    private Class<?> service;
    /**
     * 远程调用地址
     */
    private InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 8989);

    public ProxyHandler(Class<?> service) {
        this.service = service;
    }

    @Override
    public Object invoke(Object object, Method method, Object[] args) throws Throwable {
        //准备传输的对象
        RpcContext rpcContext = new RpcContext();
        rpcContext.setService(service.getName());
        rpcContext.setMethod(method.getName());
        rpcContext.setArguments(args);
        rpcContext.setArgumentTypes(method.getParameterTypes());

        return this.request(rpcContext);
    }

    private Object request(RpcContext rpcContext) {
        //使用线程池，主要是为了下面使用Future，异步得到结果，来做超时放弃处理
        ExecutorService executor = new ThreadPoolExecutor(5, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(5),
                new RpcThreadFactory("handler"));
        Object result = null;


        Future<?> future = executor.submit((Callable<?>) () -> {
            //执行并返回远程调用结果
            return req(rpcContext);
        });

        try {
            result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println("请求响应超时放弃..");
            future.cancel(true);
        }
        executor.shutdown();
        return result;
    }

    /**
     * 远程调用请求
     *
     * @param rpcContext rpc 请求上下文
     * @return 调用结果
     * @throws ClassNotFoundException e
     */
    private Object req(RpcContext rpcContext) throws ClassNotFoundException {
        Object result = null;
        Socket socket = null;
        ObjectOutputStream os = null;
        ObjectInputStream is = null;
        try {
            socket = new Socket(remoteAddress.getAddress(), remoteAddress.getPort());
            os = new ObjectOutputStream(socket.getOutputStream());
            os.writeObject(rpcContext);
            // 传输完毕
            socket.shutdownOutput();
            // 阻塞等待服务器响应
            is = new ObjectInputStream(socket.getInputStream());
            result = is.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            release(socket, os, is);
        }
        return result;
    }

    private void release(Socket socket, ObjectOutputStream os, ObjectInputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
