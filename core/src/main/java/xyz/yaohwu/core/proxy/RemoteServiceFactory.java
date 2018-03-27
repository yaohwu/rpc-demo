package xyz.yaohwu.core.proxy;

import xyz.yaohwu.provider.service.HelloService;

import java.lang.reflect.Proxy;

/**
 * @author yaoh.wu
 */
public class RemoteServiceFactory {
    /**
     * 动态代理的真实对象的实现
     *
     * @param service service
     * @return HelloService
     */
    public static HelloService newRemoteProxyObject(final Class<? extends HelloService> service) {
        return (HelloService) Proxy.newProxyInstance(service.getClassLoader(), new Class[]{service}, new ProxyHandler(service));
    }
}
