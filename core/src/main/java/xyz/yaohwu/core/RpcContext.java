package xyz.yaohwu.core;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * @author yaoh.wu
 */
public class RpcContext implements Serializable {
    /**
     * 接口名
     */
    private String service;

    /**
     * 方法名
     */
    private String method;

    /**
     * 参数类型
     */
    private Class<?>[] argumentTypes;

    /**
     * 参数
     */
    private Object[] arguments;

    /**
     * 本地地址
     */
    private InetSocketAddress localAddress;

    /**
     * 远程地址
     */
    private InetSocketAddress remoteAddress;
    /**
     * 超时 默认10秒
     */
    private long timeout = 10000L;


    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Class<?>[] getArgumentTypes() {
        return argumentTypes;
    }

    public void setArgumentTypes(Class<?>[] argumentTypes) {
        this.argumentTypes = argumentTypes;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public String toString() {
        return "RpcContext{" +
                "service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", argumentTypes=" + Arrays.toString(argumentTypes) +
                ", arguments=" + Arrays.toString(arguments) +
                ", localAddress=" + localAddress +
                ", remoteAddress=" + remoteAddress +
                ", timeout=" + timeout +
                '}';
    }
}
