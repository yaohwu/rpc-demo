package xyz.yaohwu.service;


/**
 * @author yaoh.wu
 */
public class RemoteHelloServiceImpl implements HelloService {
    @Override
    public String say(String words) {
        System.out.println("hello, this is remote hello: " + words);
        return words;
    }
}
