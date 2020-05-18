linux和mac等安装oh-my-zsh

这个zsh可以配置各个环节变量

Gradle clean build

学习过程中，不要过分依赖图形工具，要多掌握命令

netty并没有实现servlet的相关接口

netty可以充当http服务器

channel 通道		

channelHandler 通道处理器 类似过滤器拦截器一样

pieple一个pieple由多个channelHandler构成的

EventLoopGroup NIOEventLoopGroup 事件循环组，一个死循环

bossGroup workerGroup

Serverbootstrap nioserversocketchannel 

HttpServerCodec 用于pipeline

channelRead0

messagereviced

使用谷歌浏览器对netty搭建的简单服务器进行url请求时候，请求会发起两次，一次是请求内容，一次是请求网站的标志图片，favicon.ico？

netty本事不遵循servlet，使用curl可以完成完整的一个网络流程，请求完就关闭了，但是浏览器不行，基于http1.1有个等待时间，http1.0是没有等待时间的，使用netty需要手动关闭

mac检查端口

lsof -i:8899



