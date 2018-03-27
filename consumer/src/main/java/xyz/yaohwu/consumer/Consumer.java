package xyz.yaohwu.consumer;

import xyz.yaohwu.core.proxy.RemoteServiceFactory;
import xyz.yaohwu.provider.service.HelloService;

/**
 * @author yaoh.wu
 */
public class Consumer {


    public static void main(String[] args) {
        //获取动态代理的HelloService的“真实对象（其实内部不是真实的，被换成了调用远程方法）”
        final HelloService helloService = RemoteServiceFactory.newRemoteProxyObject(HelloService.class);

        String result = helloService.say("demo");
        System.out.println("rpc result: " + result);
    }
}
