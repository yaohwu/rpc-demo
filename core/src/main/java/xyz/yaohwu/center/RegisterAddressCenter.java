package xyz.yaohwu.center;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yaoh.wu
 */
public class RegisterAddressCenter {

    /**
     * 暴露接口的实现类存放容器
     */
    private static ConcurrentHashMap<String, String> addresses = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, String> getRegisterAddresses() {
        return addresses;
    }

    public static void setRegisterAddresses(ConcurrentHashMap<String, String> addresses) {
        RegisterAddressCenter.addresses = addresses;
    }

    /**
     * 按照一定的分配策略获取服务地址
     *
     * @return ip address
     */
    public static String getAddress() {
        return addresses.get("Cloud");
    }
}
