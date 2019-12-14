# MQ手动进行ACK

前期在MQ的config配置里SpringBoot集成RabbitMq手动确认消息ACK（亲测）
Linux部署环境
采用Docker快速部署rabbitMq环境
docker安装

安装yum-utils：

sudo yum install -y yum-utils device-mapper-persistent-data lvm2
添加阿里yum源
sudo yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
生成缓存
sudo yum makecache fast

sudo yum -y install docker-ce
1
2
3
4
5
6
7
启动docker并设置为开机启动

    systemctl start docker
   systemctl enable docker
1
2
4.部署rabbitMq容器
下载镜像文件

docker pull rabbitmq:management

创建实例并启动(以下是一段完整命令)

docker run -d --name rabbitmq --publish 5671:5671 \
--publish 5672:5672 --publish 4369:4369 --publish 25672:25672 --publish 15671:15671  \
--publish 15672:15672 \
rabbitmq:management
5. 开放端口25672 供外部访问rabbitMq的Web界面
1
2
3
4
5
添加（–permanent永久生效，没有此参数重启后失效）

firewall-cmd --zone=public --add-port=15672/tcp --permanent

重新载入

firewall-cmd --reload

浏览器访问(其中 ip 是你自己的虚拟机或者线上服务器地址):

http:// ip :15672

登录默认用户名为: guest
登录默认密码为: guest
新建用户


新建virtual


登录 test用户

登录test

=到此，linux环境部署完成===
postMan测试访问:http://127.0.0.1:9011/test/do/send
java代码地址:https://github.com/tangchuHoney/springBoot-rabbitmq-demo 觉得有用的话就star一下
部分代码:
采用纯注解方式绑定交换机，routingkey的指定等，简单易懂


点赞
————————————————
版权声明：本文为CSDN博主「雷军ivan」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/Geekelove/article/details/95361508面配置手动ack

public class LogConsumeProcess extends MessageListenerAdapter {

    private Logger logger = LoggerFactory.getLogger(LogConsumeProcess.class);
     
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        final DateTime begin = now();
        try {
            String messageBody = new String(message.getBody());
           
            //手动ACK
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    
        } catch (Exception e) {
            //消费失败重新放入队列
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}