package xyz.yaohwu;


import xyz.yaohwu.server.RpcServer;
import xyz.yaohwu.server.Server;
import xyz.yaohwu.service.HelloService;
import xyz.yaohwu.service.RemoteHelloServiceImpl;

/**
 * @author yaoh.wu
 */
public class Provider {
    public static void main(String[] args) throws Exception {

        Server rpcServer = new RpcServer(8989, 5);
        //暴露HelloService接口，具体实现为HelloServiceImpl
        rpcServer.register(HelloService.class.getName(), RemoteHelloServiceImpl.class);
        //启动rpc服务
        rpcServer.start();
    }
}
