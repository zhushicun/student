protobuffer和thrift 两种对于rpc传递数据序列化和反序列化工具

thrift不支持date日期格式，所以往往使用String类型传递日期格式

和rpc异曲同工的有java本身提供的rmi  RMI（Remote Method Invocation）远程方法调用。能够让在客户端Java虚拟机上的对象像调用本地对象一样调用服务端java 虚拟机中的对象上的方法。**使用代表：EJB**

大的不同点上，rmi只支持对于java系统之间的远程调用，而rpc则是跨语言的。

详细的区别https://cloud.tencent.com/developer/article/1353191

http1.1提供一个keepalive属性，例如给定2s时间，第一次请求连接后，通信完，在2s之内再次通信，那么会复用第一次连接，如果2s内没有再次通信，则断开连接。

websocket协议，依附于http，在http请求时，在http请求头里面会附带一些数据，这些数据要求服务器端请求升级，如果服务器端支持websocket协议，将连接升级成websocket连接，服务器端和客户端将实现全双工的通信，适用于即时通信。与此同时，有个心跳包的功能，可以检测通道是否需要关闭（app和服务端通信，手机开启飞行模式或强制关机）

