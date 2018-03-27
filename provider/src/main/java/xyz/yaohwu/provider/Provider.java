package xyz.yaohwu.provider;


import xyz.yaohwu.core.server.RpcServer;
import xyz.yaohwu.core.server.Server;
import xyz.yaohwu.provider.service.HelloService;
import xyz.yaohwu.provider.service.RemoteHelloServiceImpl;

/**
 * @author yaoh.wu
 */
public class Provider {
    public static void main(String[] args) throws Exception {

        Server rpcServer = new RpcServer(8989, "127.0.0.1", "Cloud", 5);
        //暴露HelloService接口，具体实现为HelloServiceImpl
        rpcServer.register(HelloService.class.getName(), RemoteHelloServiceImpl.class);
        //启动rpc服务
        rpcServer.start();
    }
}
