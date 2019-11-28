redis 单机安装

登录redis网站下载redis

https://redis.io/download

根据官网操作指令

```
$ wget http://download.redis.io/releases/redis-5.0.7.tar.gz
$ tar xzf redis-5.0.7.tar.gz
$ cd redis-5.0.7
$ make
```

启动

```
$ src/redis-server
```

操作

```
$ src/redis-cli
redis> set foo bar
OK
redis> get foo
"bar"
```

**2.7 安装编译后的redis代码到指定目录,一般存放于/usr/local下的redis目录,指令如下**

make install PREFIX=/usr/local/redis

**2.10 前端启动的话,如果客户端关闭,redis服务也会停掉,所以需要改成后台启动redis.**
具体做法分为两步 -> 第一步:将redis解压文件里面的redis.conf文件复制到当前目录,指令如下

cp ~/redis-3.0.0/redis.conf .

vim redis.conf

**2.12 查看redis是否在运行,指令如下**

./redis-cli

执行ping

**2.14 将连接到其他端口,指令如下**

./redis-cli -h 192.168.25.153 -p 6379



//查看str1的有效期
ttl str1

//判断是否存在str1这个key
exits str1

//获取str1的数据类型
type str1

set str1 abc

get str1

//存储数据
hset str2 field def

//获取数据
hget str2 field

//设置key的时间

expire shawn 10