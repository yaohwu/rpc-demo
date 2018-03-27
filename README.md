# RPC

## 前言

一方面，在之前的分享会上，有人提过rpc，我对这个也感兴趣，因为我完全不知道这个技术；
另外，数据挖掘还没什么显著的成果，不好意思分享。因此就趁着这个机会，学习一下 RPC

## 什么是RPC

上次分享会也提到过，Socket 是 Client-Server 网络中的基本组成部分，它个提供了一种相对简单的机制，让一个应用程序建立一个到另外一个应用程序的连接，来回发送消息。

后来，两位大佬设计了一种新的机制。
机器 A 上的一个进程可以调用在机器 B 上一个过程，当它这样做时，A 的进程被暂停执行，继续执行 B。当 B 返回时，返回值被传递给 A，A 继续执行，这种机制被称作 RPC。这个过程非常类似与本地调用，但是和本地调用又有着明显的区别。

RPC 是指远程过程调用。

如果两台服务器 A 和 B，部署在服务器 A 上的应用希望调用部署在服务器 B 上应用程序的函数，但是由于不能共享内存空间，不能直接调用，需要通过网络表达调用的语义和传达调用的数据。

RPC 在各大互联网公司中被广泛使用，如阿里巴巴的hsf、dubbo（开源）、Facebook的thrift（开源）、Google grpc（开源）、Twitter的finagle（开源）等等。

大致介绍了 RPC，接下来我们通过写一个简单的 RPC 框架来深入理解一下。在写之前，我们需要了解一下 RPC 的调用流程和通信细节。

## RPC 的调用流程和通信细节

为了实现 RPC 的网络细节对使用者透明，我们需要对网络通信细节进行封装。先了解一下 RPC 调用的流程，以及有哪些通信细节。

![RPC flow](resources/rpc-flow.png)

1. 客户端以本地调用的方式调用一个服务；
2. client stub 接收到调用后将参数打包成一个甚至多个网络传输的消息体，打包过程需要编组和序列化数据。并将这些消息体交给给基于 socket 设计的通信接口；
3. 通信接口通过协议（无连接或者面向连接的协议）传输这些消息体；
4. 服务端或者远程接收到消息体后将消息体转交给 server stub；
5. server stub 反序列化参数，然后调用 "本地方法" server functions；
6. server functions 执行完后将结果返回给 server stub；
7. server stub 将结果打包成消息体，序列化，编组交给 server 端的通信接口；
8. 客户端收到返回的结果后，将结果交给 client stub；
9. client stub 将结果解码，返回给调用方；
10. 完成调用，得到结果。

为了实现细节对使用者透明，RPC 就要将 2-9 的过程封装起来。

封装过程中要解决这些问题：

1. 通信的问题，在客户端和服务器直接建立连接，RPC 所有数据的交换都要在这个连接里面传输。可以按需连接，调用结束后就断开；也可以是长连接，多个远程调用共享一个连接。
2. 寻址问题，A 服务器上的应用要让底层的 RPC 框架知道怎么调用到 B 服务器上的特定方法。
3. 网络传输中的数据要进行序列化或者编组，接收到的数据要进行反序列化，恢复成为内存中的表达方式。

问题1，建立连接，我们可以直接使用socket。
问题2，寻址方式，可以建立一个服务注册中心，将服务注册进来，保证可以调用。
问题3，序列化的方案更是非常多，Protobuf、Kryo、Hessian、Jackson 等，出于简单，我们使用 Java 默认的序列化。

## 封装细节

使用 Java 的 socket 来建立通信、默认的序列化方法实现序列化，服务注册中心可以先直接写死。

要让使用者像以本地调用方式调用远程服务，可以使用 java 的动态代理可以做到这一点。
关于动态代理的知识，可以看[mock 从动态代理到单元测试](https://yaohwu.xyz/#/posts/4);
动态代理可以有反射或者生成字节码来实现。

借助反射，实现动态代理

```java
/**
 * @author yaoh.wu
 */
public class AddInvocationHandler implements InvocationHandler {
    private Add delegate;
    public AddInvocationHandler(Add delegate) {
        this.delegate = delegate;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable, InvocationTargetException {
        if ("add".equals(method.getName())) {
            Integer x = (Integer) args[0];
            Integer y = (Integer) args[1];
            System.out.print("x=" + x + " y=" + y + " result=");
            Integer result = delegate.add(x, y);
            System.out.println(result);
            return result;
        }
        return method.invoke(delegate, args);
    }
}
```

```java
/**
 * @author yaoh.wu
 */
public class AdderProxyFactory {

    public static Add createAdderProxy(Add delegate) {
        return (Add) Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                delegate.getClass().getInterfaces(),
                new AddInvocationHandler(delegate));
    }
}
```

或者借用其他类库生成字节码，来实现动态代理。

出于简单，使用反射。

### 编写服务接口

类似于动态代理要求被代理的对象和代理类都实现同一个接口，远程调用和本地调用都应该实现一个共同的接口，例子中我们这样写这个接口：

```java
package xyz.yaohwu.provider.service;

/**
 * @author yaoh.wu
 */
public interface HelloService {

    /**
     * say
     *
     * @param word something
     * @return String
     */
    String say(String word);
}
```

### 编写服务接口的实现类


