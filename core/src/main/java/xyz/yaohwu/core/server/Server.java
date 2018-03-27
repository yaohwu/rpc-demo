package xyz.yaohwu.core.server;

/**
 * @author yaoh.wu
 */
public interface Server {
    /**
     * 启动rpc服务
     */
    void start();

    /**
     * 停止rpc服务
     */
    void stop();

    /**
     * 把服务注册进rpc
     *
     * @param name  name
     * @param clazz class
     * @throws Exception e
     */
    void register(String name, Class clazz) throws Exception;

    /**
     * rpc 服务是否存活
     *
     * @return isAlive
     */
    boolean isAlive();
}
