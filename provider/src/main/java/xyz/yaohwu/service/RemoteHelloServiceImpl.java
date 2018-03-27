package xyz.yaohwu.service;


/**
 * @author yaoh.wu
 */
public class RemoteHelloServiceImpl implements HelloService {
    @Override
    public String say(String words) {
        String result = "hello, this is remote hello: " + words;
        System.out.println(result);
        return result;
    }
}
