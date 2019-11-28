# 关于ruby升级

问题描述：

                      在Centos7中，通过yum安装ruby的版本是2.0.0，但是如果有些应用需要高版本的ruby环境，比如2.2，2.3，2.4...

　　　　　　那就有点麻烦了，譬如：我准备使用redis官方给的工具：redis-trib.rb 这个工具构建redis集群的时候，报错了：

　　　　　　　　　　　　　　　　　　“redis requires Ruby version >= 2.2.2”

解决方法（已经尝试，没有问题）

1，首先系统需要安装redis，毕竟是搭建redis集群，如果没有安装redis，请先执行(如果安装了，请略过此步骤)：

yum install gcc-c++

2，RVM 安装：

先执行一条官方 https://rvm.io/ 复制来的长命令：

gpg --keyserver hkp://keys.gnupg.net --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3 7D2BAF1CF37B13E2069D6956105BD0E739499BDB



继续执行#：curl -sSL https://get.rvm.io | bash -s stable            (预计7秒左右才会相应)



继续执行#：source  /etc/profile.d/rvm.sh　　　　//按照提示，执行该命令

继续执行#： rvm list known



3，安装ruby，

# rvm install 2.4.1　　　　// 安装ruby 2.4.1 ，直接跟版本号即可



#ruby -v    //查看验证下已经安装的版本

4，安装redis集群接口

#：gem install redis



至此，ruby安装完成了。然后启动集群就不会报错了./src/redis-trib.rb create --replicas 2 192.168.231.25:7000