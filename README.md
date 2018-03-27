# 如何实现一个 RPC 框架

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

编写一个服务端针对接口的实现类，供客户端调用。

```java
package xyz.yaohwu.provider.service;


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
```

### 实现一个 RPC 服务器

大致接口如下：

```java
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
```

具体实现可以看代码。

### 组装传输的消息实体

由于我们要使用 Java 默认提供的序列化，因此需要实现 Serializable 接口。

```java
package xyz.yaohwu.core;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * @author yaoh.wu
 */
public class RpcContext implements Serializable {
    /**
     * 接口名
     */
    private String service;

    /**
     * 方法名
     */
    private String method;

    /**
     * 参数类型
     */
    private Class<?>[] argumentTypes;

    /**
     * 参数
     */
    private Object[] arguments;
```

### 实现 RPC 代理

```java
package xyz.yaohwu.core.proxy;

public class ProxyHandler implements InvocationHandler {

    /**
     * 超时等待时间
     */
    private static final long TIMEOUT = 10000L;
    private Class<?> service;
    /**
     * 远程调用地址
     */
    private InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 8989);

    public ProxyHandler(Class<?> service) {
        this.service = service;
    }

    @Override
    public Object invoke(Object object, Method method, Object[] args) throws Throwable {
        //准备传输的对象
        RpcContext rpcContext = new RpcContext();
        rpcContext.setService(service.getName());
        rpcContext.setMethod(method.getName());
        rpcContext.setArguments(args);
        rpcContext.setArgumentTypes(method.getParameterTypes());

        return this.request(rpcContext);
    }

    /**
     * 远程调用请求
     *
     * @param rpcContext rpc 请求上下文
     * @return 调用结果
     * @throws ClassNotFoundException e
     */
    private Object req(RpcContext rpcContext) throws ClassNotFoundException {
        Object result = null;
        Socket socket = null;
        ObjectOutputStream os = null;
        ObjectInputStream is = null;
        try {
            socket = new Socket(remoteAddress.getAddress(), remoteAddress.getPort());
            os = new ObjectOutputStream(socket.getOutputStream());
            // 直接使用 java 默认的对象序列化方式
            os.writeObject(rpcContext);
            // 传输完毕
            socket.shutdownOutput();
            // 阻塞等待服务器响应
            is = new ObjectInputStream(socket.getInputStream());
            result = is.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            release(socket, os, is);
        }
        return result;
    }

}
```

### 服务注册和服务发现

处于简单，就直接写死了（代码中有一个假的服务注册方案）。
主要要实现服务地址，端口，接口和方法名的注册。

```java
// todo
```

### 运行demo

启动 provider:

```java
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

```

运行 consumer:

```java
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
```

provider 日志：

```shell
RPC服务启动成功...
执行一次响应..Tue Mar 27 15:16:21 CST 2018
执行任务需要消耗：400 RpcContext{service='xyz.yaohwu.provider.service.HelloService', method='say', argumentTypes=[class java.lang.String], arguments=[demo], localAddress=null, remoteAddress=null, timeout=10000}
hello, this is remote hello: demo
```

consumer 日志：

```shell
rpc result: hello, this is remote hello: demo
```
![rpc demo](resources/RPC-module-dependencies.png)


### 总结

一个非常简单的 RPC 框架就完成了。
太过简单了，有很多的部分可以补充和完善。
例如，服务注册和发现，现在是完全没有实现，我们可以引入zookeeper来提供服务注册和发现，并且提供分布式支持。
在 demo 中，我们使用的是传统的阻塞式的 IO,可以替换成 NIO 去做高并发需求，或者进一步引入Netty。
序列化方面，可以替换更优秀的序列化框架，Protobuf、Kryo、Hessian、Jackson等，提供更优秀的性能。

## RPC 和 HTTP 的比较

首先，RPC 和 HTTP 不是一个并行的概念。

RPC 远程过程调用，它的调用协议通常包含编码协议和传输协议。

### 编码协议

编码协议规定的是如何完成消息体对象和二进制之间的转换，也就是序列化和反序列化。
大多数 rpc 框架的编码协议都是可以选择的，甚至支持定制序列化方案。
http 的编码协议在一定程度上也是可以更换的，比如 http 也可以使用 protobuf 这种二进制编码协议对内容进行编码。

### 传输协议

传输协议指的是被转换成的二进制数据怎么在网络上传输，demo 中我们直接使用的 Socket，那么对应的协议应该是 TCP。
我们也可以使用 http 作为 RPC 的传输协议，[gRPC](https://grpc.io/)使用传输协议是基于 http2 协议的，甚至也可以自定义 tcp 协议，dubbo 默认使用的就是自定义 tcp 协议。

### 比较

所以说，两个不是一个并行的概念。
从 rpc 希望达到的目的上来讲，可以说 http 是 rpc 的一种实现，然而 rpc 有着更强的语义， rpc 也可以使用 http 作为传输协议。

其实只要理解 rpc 的概念就可以，可定制性更强，可以更精简，保密。

## 几种主流的 java RPC 框架

凑字数。

| name    | com              | code address                                       |
| ------- | ---------------- | -------------------------------------------------- |
| dubbo   | alibaba,apache   | [dubbo](https://github.com/apache/incubator-dubbo) |
| motan   | weibocom         | [motan](https://github.com/weibocom/motan)         |
| thrift  | facebook, apache | [thrift](https://thrift.apache.org/download)       |
| finagle | twitter          | [finagle](https://github.com/twitter/finagle)      |
| Avro    | apache           | [Avro](https://github.com/apache/avro)             |
| grpc    | google           | [grpc](https://github.com/grpc/)                   |
| hessian | caucho           | [hessian](http://hessian.caucho.com/#Java)         |
等等

### dubbo

Alibaba开源的分布式服务框架，功能强大，属于服务治理框架。支持多种注册中心，支持多种网络通信框架，支持多种传输协议，支持多种序列化方式。

![dubbo-relation](resources/dubbo-relation.png)
![dubbo-extension](resources/dubbo-extension.png)

### motan

weibo开源的分布式服务框架，功能也很强，属于服务治理框架。主要模块都提供了多种不同的实现，例如支持多种注册中心，支持多种rpc协议等。[user guide](https://github.com/weibocom/motan/wiki/zh_userguide)

### thrift

由Fackbook开发的可伸缩、跨语言的服务开发框架，该框架已经开源并且加入的Apache项目。Thrift主要功能是：通过自定义的Interface Definition Language(IDL)，可以创建基于RPC的客户端和服务端的服务代码。数据和服务代码的生成是通过Thrift内置的代码生成器来实现的。Thrift 的跨语言性体现在，它可以生成C++, Java, Python, PHP, Ruby, Erlang, Perl, Haskell, C#, Cocoa, JavaScript, Node.js, Smalltalk, OCaml , Delphi等语言的代码，且它们之间可以进行透明的通信。

### Finagle

Finagle是Twitter基于Netty开发的支持容错的、协议无关的RPC框架。也支持多种协议。Finagle  is written in Scala, but provides both Scala and Java idiomatic APIs.

### Avro

Avro是Hadoop中的一个子项目，也是Apache中一个独立的项目。主要用在Hadoop中。类似于Thrift，支持跨编程语言实现（C, C++, C#，Java, Python, Ruby, PHP）。用来支持数据密集型应用，适合于远程或本地大规模数据的存储和交换。

### grpc

由Google主导开发的RPC框架，使用HTTP/2协议并用ProtoBuf作为序列化工具。

### hessian

## 产品中的应用场景

也不好说，RPC 是建立在服务调用的基础上的。

目前我们产品中的服务调用有哪些呢？

集群环境下的分发？也用不到，直接分发就好了。

集群环境下的事务一致性？也不能解决一致性的问题。

这方面能替代的也就只有 http，就看需不需要。

预览等用户直接使用的，这肯定走 http 了，没有什么疑问。

集群环境下的同步，嗯，好像可以走 rpc。不过还是需要解事务一致性问题。

远程设计问题，可以走 rpc。

与云中心的一些接口调用，例如发送短信等，可以走 rpc。

另外，我们可以暴露 rpc 接口，供客户进行第三方集成，应该也能算是一个业务方向。

场景还是有点儿，主要看替代 http 之后能不能有比较好的效果，安全性，性能等方面。

了解了一下 rpc，还是不错。

```java
// todo
```

## 参考

[Remote Procedure Calls](https://www.cs.rutgers.edu/~pxk/417/notes/03-rpc.html)
[轻量级分布式 RPC 框架](https://gitee.com/huangyong/rpc)
[java io](https://github.com/anxpp/Java-IO)