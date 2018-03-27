package xyz.yaohwu.center;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yaoh.wu
 */
public class RegisterServicesCenter {
    /**
     * 暴露接口的实现类存放容器
     */
    private static ConcurrentHashMap<String, Class> registerServices = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, Class> getRegisterServices() {
        return registerServices;
    }

    public static void setRegisterServices(ConcurrentHashMap<String, Class> registerServices) {
        RegisterServicesCenter.registerServices = registerServices;
    }
}
