# 目录

目录 1

\1. 前言 2

\2. 名词解释 2

\3. 部署计划 2

\4. 修改系统参数 3

4.1. 修改最大可打开文件数 3

4.2. TCP监听队列大小 4

4.3. OOM相关：vm.overcommit_memory 4

4.4. /sys/kernel/mm/transparent_hugepage/enabled 4

\5. 目录结构 4

\6. 编译安装 5

\7. 配置redis 5

\8. 启动redis实例 9

\9. 创建和启动redis集群 10

9.1. 创建redis cluster 10

9.2. ps aux|grep redis 12

\10. redis cluster client 12

10.1. 命令行工具redis-cli 12

10.2. 从slaves读数据 13

10.3. jedis（java cluster client） 13

10.4. r3c（C++ cluster client） 13

\11. 新增节点 13

11.1. 添加一个新主（master）节点 13

11.2. 添加一个新从（slave）节点 15

\12. 删除节点 16

\13. master机器硬件故障 16

\14. 检查节点状态 16

\15. 变更主从关系 17

\16. slots相关命令 17

\17. 迁移slosts 17

\18. 人工主备切换 18

\19. 查看集群信息 19

\20. 禁止指定命令 19

\21. 数据迁移 20

\22. 各版本配置文件 20

\23. 大压力下Redis参数调整要点 20

\24. 问题排查 22

 

# 1. 前言

本文档基于以前写的《Redis-3.0.5集群配置》和《Redis-4.0.11集群配置》。

redis-3.0.0开始支持集群，redis-4.0.0开始支持module，redis-5.0.0开始支持类似于kafka那样的消息队列。

本文参考官方文档而成：http://redis.io/topics/cluster-tutorial，不适用redis-5.0.0以下版本，原因是从redis-5.0.0版本开始，redis-trib.rb的功能被redis-cli替代了。

redis-5.0.0以下版本的安装和部署，可参考：https://blog.csdn.net/Aquester/article/details/50150163。

redis运维工具和部署工具：https://github.com/eyjian/redis-tools。

# 2. 名词解释

| 名词    | 解释                                                        |
| ------- | ----------------------------------------------------------- |
| ASAP    | As Soon As Possible，尽可能                                 |
| RESP    | Redis Serialization Protocol，redis的序列化协议             |
| replica | 从5.0开始，原slave改叫replica，相关的配置参数也做了同样改名 |

# 3. 部署计划

redis要求至少三主三从共6个节点才能组成redis集群，测试环境可一台物理上启动6个redis节点，但生产环境至少要准备3台物理机。

| 服务端口 | IP地址        | 配置文件名      |
| -------- | ------------- | --------------- |
| 6381     | 192.168.0.251 | redis-6381.conf |
| 6382     | 192.168.0.251 | redis-6382.conf |
| 6383     | 192.168.0.251 | redis-6383.conf |
| 6384     | 192.168.0.251 | redis-6384.conf |
| 6385     | 192.168.0.251 | redis-6385.conf |
| 6386     | 192.168.0.251 | redis-6386.conf |

 

疑问：如果是3台物理机，会不会主和从节点分布在同一个物理机上？

# 4. 修改系统参数

## 4.1. 修改最大可打开文件数

修改文件/etc/security/limits.conf，加入以下两行：



 

其中102400为一个进程最大可以打开的文件个数，当与RedisServer的连接数多时，需要设定为合适的值。

有些环境修改后，root用户需要重启机器才生效，而普通用户重新登录后即生效。如果是crontab，则需要重启crontab，如：service crond restart，有些平台可能是service cron restart。

有些环境下列设置即可让root重新登录即生效，而不用重启机器：



 

但是要小心，有些环境上面这样做，可能导致无法ssh登录，所以在修改时最好打开两个窗口，万一登录不了还可自救。

如何确认更改对一个进程生效？按下列方法（其中$PID为被查的进程ID）：



 

系统关于/etc/security/limits.conf文件的说明：



 

PAM：全称“Pluggable Authentication Modules”，中文名“插入式认证模块”。/etc/security/limits.conf实际为pam_limits.so（位置：/lib/security/pam_limits.so）的配置文件，只针对单个会话。要使用limits.conf生效，必须保证pam_limits.so被加入到了启动文件中。

注释说明只对通过PAM登录的用户生效，与PAM相关的文件（均位于/etc/pam.d目录下）：



 

如果需要设置Linux用户的密码策略，可以修改文件/etc/login.defs，但这个只对新增的用户有效，如果要影响已有用户，可使用命令chage。

## 4.2. TCP监听队列大小

即TCP listen的backlog大小，“/proc/sys/net/core/somaxconn”的默认值一般较小如128，需要修改大一点，比如改成32767。立即生效还可以使用命令：sysctl -w net.core.somaxconn=32767。

要想永久生效，需要在文件/etc/sysctl.conf中增加一行：net.core.somaxconn = 32767，然后执行命令“sysctl -p”以生效。

Redis配置项tcp-backlog的值不能超过somaxconn的大小。

## 4.3. OOM相关：vm.overcommit_memory

如果“/proc/sys/vm/overcommit_memory”的值为0，则会表示开启了OOM。可以设置为1关闭OOM，设置方法请参照net.core.somaxconn完成。

## 4.4. /sys/kernel/mm/transparent_hugepage/enabled

默认值为“[always] madvise never”，建议设置为never，以开启内核的“Transparent Huge Pages (THP)”特性，设置后redis进程需要重启。为了永久生效，请将“echo never > /sys/kernel/mm/transparent_hugepage/enabled”加入到文件/etc/rc.local中。

什么是Transparent Huge Pages？为提升性能，通过大内存页来替代传统的4K页，使用得管理虚拟地址数变少，加快从虚拟地址到物理地址的映射，以及摒弃内存页面的换入换出以提高内存的整体性能。内核Kernel将程序缓存内存中，每页内存以2M为单位。相应的系统进程为khugepaged。

在Linux中，有两种方式使用Huge Pages，一种是2.6内核引入的HugeTLBFS，另一种是2.6.36内核引入的THP。HugeTLBFS主要用于数据库，THP广泛应用于应用程序。

一般可以在rc.local或/etc/default/grub中对Huge Pages进行设置。

# 5. 目录结构

redis.conf为从https://raw.githubusercontent.com/antirez/redis/5.0/redis.conf下载的配置文件，带端口号的配置文件基于redis.conf修改。实际只需要完成公共的redis.conf和一个端口号的，如redis-6381.conf，其它端口号的配置文件基于一个修改后的端口号配置文件即可。

本文将redis安装在/data/redis，建议将bin目录加入到环境变量PATH中，以简化后续的使用。

如果拿到的是redis源代码，在make成功后，推荐按下列目录结构部署各程序文件：



 

注意，redis-check-dump和redis-check-rdb是同一个程序，在redis-3.0.0之前叫redis-check-dump，之后更名为redis-check-rdb。

# 6. 编译安装

打开redis的Makefile文件，可以看到如下内容：



 

Makefile中的“?=”表示，如果该变量之前没有定义过，则赋值为/usr/local，否则什么也不做。

如果不设置环境变量PREFIX或不修改Makefile中的值，则默认安装到/usr/local/bin目录下。建议不要使用默认配置，而是指定安装目录，如/data/redis-5.0.0：



# 7. 配置redis

推荐配置分成两部分：一是公共配置，另一个与端口相关的配置。公共配置文件名可命令为redis.conf，而端口相关的配置文件名可命令为redis-PORT.conf或redis_PORT.conf。假设端口为6379，则端口相关的配置文件名为redis-6379.conf。redis-PORT.conf通过include的方式包含redis.conf，如：include /data/redis/conf/redis.conf。

从https://raw.githubusercontent.com/antirez/redis/5.0/redis.conf下载配置文件（也可直接复制源代码包中的redis.conf，然后在它的基础上进行修改），在这个基础上，进行如下表所示的修改（配置文件名redis-PORT.conf中的PORT替换为实际使用的端口号，如6381等）。

高效完成多个端口配置的一个方法是先完成一个指定端口的配置文件，然后替换端口方式生成另一个端口的配置文件。如通过端口6381的配置文件redis-6381.conf生成端口号6382的配置文件redis-6382.conf，只需要这样：sed 's/6381/6382/g' redis-6381.conf > redis-6382.conf。

 

下表配置项，加粗部分是必须和建议修改的，其它可根据实际需求修改：

| 配置项（加粗部分必须或建议修改）                             | 值                                 | 配置文件                                                     | 说明                                            |
| ------------------------------------------------------------ | ---------------------------------- | ------------------------------------------------------------ | ----------------------------------------------- |
| include                                                      | redis.conf                         | 指定端口的配置文件redis-PORT.conf（该文件定义所有与端口相关的配置项，PORT需要替换为具体的端口，如6381） | 引用公共的配置文件，建议为全路径值              |
| port                                                         | PORT                               | 客户端连接端口，并且总有一个刚好大于10000的端口，这个大的端口用于主从复制和集群内部通讯。 |                                                 |
| cluster-config-file                                          | nodes-PORT.conf                    | 默认放在dir指定的目录，注意不能包含目录，纯文件名，为redis-server进程自动维护，不能手工修改 |                                                 |
| pidfile                                                      | /var/run/redis-PORT.pid            | 只有当daemonize值为yes时，才有意义；并且这个要求对目录/var/run有写权限，否则可以考虑设置为/tmp/redis-PORT.pid，或者放在bin或log目录下，如：/data/redis/log/redis-PORT.pid。只有当配置项daemonize的值为yes时，才会产生这个文件。 |                                                 |
| dir                                                          | /data/redis/data/PORT              |                                                              |                                                 |
| dbfilename                                                   | dump-PORT.rdb                      | 纯文件名，位于dir指定的目录下，不能包含目录，否则报错“appendfilename can't be a path, just a filename” |                                                 |
| appendfilename                                               | "appendonly-PORT.aof"              | 纯文件名，位于dir指定的目录下，不能包含目录，否则报错“appendfilename can't be a path, just a filename” |                                                 |
| logfile                                                      | /data/redis/log/redis-PORT.log     | 日志文件，包含目录和文件名，注意redis不会自动滚动日志文件    |                                                 |
| cluster-enabled                                              | yes                                | redis.conf（公共配置文件，定义所有与端口无关的配置项）       | yes表示以集群方式运行，为no表示以非集群方式运行 |
| loglevel                                                     | verbose                            | 日志级别，建议为notice，另外注意redis是不会滚动日志文件的，每次写日志都是先打开日志文件再写日志再关闭方式 |                                                 |
| maxclients                                                   | 10000                              | 最大连接数                                                   |                                                 |
| timeout                                                      | 0                                  | 客户端多长（秒）时间没发包过来关闭它，0表示永不关闭          |                                                 |
| cluster-node-timeout                                         | 15000                              | 单位为毫秒：repl-ping-slave-period+(cluster-node-timeout*cluster-slave-validity-factor)判断节点失效（fail）之前，允许不可用的最大时长（毫秒），如果master不可用时长超过此值，则会被failover。不能太小，建议默认值15000 |                                                 |
| cluster-slave-validity-factor（5.0开始请使用cluster-replica-validity-factor） | 0                                  | 如果要最大的可用性，值设置为0。定义slave和master失联时长的倍数，如果值为0，则只要失联slave总是尝试failover，而不管与master失联多久。失联最大时长：(cluster-slave-validity-factor*cluster-node-timeout) |                                                 |
| repl-timeout                                                 | 10                                 | 该配置项的值要求大于repl-ping-slave-period的值               |                                                 |
| repl-ping-slave-period（5.0开始请使用repl-ping-replica-period） | 1                                  | 定义slave多久（秒）ping一次master，如果超过repl-timeout指定的时长都没有收到响应，则认为master挂了 |                                                 |
| slave-read-only（5.0开始请用replica-read-only）              | yes                                | slave是否只读                                                |                                                 |
| slave-serve-stale-data（5.0开始请使用replica-serve-stale-data） | yes                                | 当slave与master断开连接，slave是否继续提供服务               |                                                 |
| slave-priority（5.0开始请使用replica-priority）              | 100                                | slave权重值，当master挂掉，只有权重最大的slave接替master     |                                                 |
| aof-use-rdb-preamble                                         |                                    | 4.0新增配置项，用于控制是否启用RDB-AOF混用，值为no表示关闭   |                                                 |
| appendonly                                                   | yes                                | 当同时写AOF或RDB，则redis启动时只会加载AOF，AOF包含了全量数据。如果当队列使用，入队压力又很大，建议设置为no |                                                 |
| appendfsync                                                  | no                                 | 可取值everysec，其中no表示由系统自动，当写压力很大时，建议设置为no，否则容易造成整个集群不可用 |                                                 |
| daemonize                                                    | yes                                | 相关配置项pidfile                                            |                                                 |
| protected-mode                                               | no                                 | 3.2.0新增的配置项，默认值为yes，限制从其它机器登录Redis server，而只能从127.0.0.1登录。 |                                                 |
| tcp-backlog                                                  | 32767                              | 取值不能超过系统的/proc/sys/net/core/somaxconn               |                                                 |
| auto-aof-rewrite-percentage                                  | 100                                | 设置自动rewite AOF文件（手工rewrite只需要调用命令BGREWRITEAOF） |                                                 |
| auto-aof-rewrite-min-size                                    | 64mb                               | 触发rewrite的AOF文件大小，只有大于此大小时才会触发rewrite    |                                                 |
| no-appendfsync-on-rewrite                                    | yes                                | 子进程在做rewrite时，主进程不调用fsync（由内核默认调度）     |                                                 |
| stop-writes-on-bgsave-error                                  | yes                                | 如果因为磁盘故障等导致保存rdb失败，停止写操作，可设置为NO。  |                                                 |
| cluster-require-full-coverage                                | no                                 | 为no表示有slots不可服务时其它slots仍然继续服务，建议值为no，以提供最高的可用性 |                                                 |
| maxmemory                                                    | 26843545600                        | 设置最大的内存，单位为字节                                   |                                                 |
| maxmemory-policy                                             | volatile-lru                       | 设置达到最大内存时的淘汰策略                                 |                                                 |
| client-output-buffer-limit                                   |                                    | 设置master端的客户端缓存，三种：normal、slave和pubsub        |                                                 |
| cluster-migration-barrier                                    | 1                                  | 最少slave数，用来保证集群中不会有裸奔的master。当某个master节点的slave节点挂掉裸奔后，会从其他富余的master节点分配一个slave节点过来，确保每个master节点都有至少一个slave节点，不至于因为master节点挂掉而没有相应slave节点替换为master节点导致集群崩溃不可用。 |                                                 |
| repl-backlog-size                                            | 1mb                                | 当slave失联时的，环形复制缓区大小，值越大可容忍更长的slave失联时长 |                                                 |
| repl-backlog-ttl                                             |                                    | slave失联的时长达到该值时，释放backlog缓冲区                 |                                                 |
| save                                                         | save 900 1save 300 10save 60 10000 | 刷新快照（RDB）到磁盘的策略，根据实际调整值，“save 900 1”表示900秒后至少有1个key被修改才触发save操作，其它类推。注意执行flushall命令也会产生RDB文件，不过是空文件。如果不想生成RDB文件，可以将save全注释掉。 |                                                 |

# 8. 启动redis实例

在启动之前，需要创建好配置中的各目录。然后启动好所有的redis实例，如以本文中定义的6个节点为例（带个目录是个良好和规范的习惯）：



 

可以写一个启动脚本start-redis-cluster.sh：



 

一般需要加上进程监控，可直接使用process_monitor.sh，监控示例（放在crontab中，下载网址：https://github.com/eyjian/libmooon/blob/master/shell/process_monitor.sh）：



 

注意：redis的日志文件不会自动滚动，redis-server每次在写日志时，均会以追加方式调用fopen写日志，而不处理滚动。也可借助linux自带的logrotate来滚动redis日志，命令logrotate一般位于目录/usr/sbin下。

# 9. 创建和启动redis集群

如果只是想快速创建和启动redis集群，而不关心过程，可使用redis官方提供的脚本create-cluster，两步完成：



 

第二步“create-cluster create”是一个交互式过程，当提示时，请输入“yes”再回车继续，第一个节点的端口号为30001，一共会启动六个redis节点。

create-cluster在哪儿？它位于redis源代码的utils/create-cluster目录下，是一个bash脚本文件。停止集群：create-cluster stop。

但如果是为学习和运营，建议按下列步骤操作，以加深对redis集群的理解，提升掌控能力：

## 9.1. 创建redis cluster

创建redis集群命令（三主三从，每个主一个从，注意redis-5.0.0版本开始才支持“--cluster”，之前的版本会报错“Unrecognized option or bad number of args for: '--cluster'”）：



 

如果配置项cluster-enabled的值不为yes，则执行时会报错“[ERR] Node 192.168.0.251:6381 is not configured as a cluster node.”。这个时候需要先将cluster-enabled的值改为yes，然后重启redis-server进程，之后才可以重新执行redis-cli创建集群。

 

Ø redis-cli的参数说明：

1) create

表示创建一个redis集群。

2) --cluster-replicas 1

表示为集群中的每一个主节点指定一个从节点，即一比一的复制。\

 

运行过程中，会有个提示，输入yes回车即可。从屏幕输出，可以很容易地看出哪些是主（master）节点，哪些是从（slave）节点：



## 9.2. ps aux|grep redis

查看redis进程是否已切换为集群状态（cluster）：



 

停止redis实例，直接使用kill命令即可，如：kill 3825，重启和单机版相同。

# 10. redis cluster client

## 10.1. 命令行工具redis-cli

官方提供的命令行客户端工具，在单机版redis基础上指定参数“-c”即可。以下是在192.168.0.251上执行redis-cli的记录：



## 10.2. 从slaves读数据

默认不能从slaves读取数据，但建立连接后，执行一次命令[READONLY](http://redis.io/commands/readonly) ，即可从slaves读取数据。如果想再次恢复不能从slaves读取数据，可以执行下命令READWRITE。

## 10.3. jedis（java cluster client）

官网：https://github.com/xetorthio/jedis，编程示例：



## 10.4. r3c（C++ cluster client）

官网：https://github.com/eyjian/r3c

# 11. 新增节点

## 11.1. 添加一个新主（master）节点

假设要添加新的节点“192.168.0.251:6390”，先以单机版配置和启动好6387，然后执行命令（“192.168.0.251:6381”为集群中任一可用的节点）：



 

如果执行顺利，看到的输出如下：



 

在执行“add-node”之前的集群：



 

执行“add-node”之后的集群（可以看到新增的master节点192.168.0.251:6390没有负责任何slots）：



 

如果报错“[ERR] Node 192.168.0.251:4077 is not configured as a cluster node.”，是因为新节点的配置项“cluster-enabled”的值不为“yes”。这时需要将“cluster-enabled”的值改为“yes”，并重启该节点，然后再重新执行“add-node”操作。

 

也可能遇到错误“[ERR] Sorry, can't connect to node 127.0.0.1:6390”，引起这个问题的原因是从Redis 3.2.0版本开始引入了“保护模式（protected mode），防止redis-cli远程访问”，仅限redis-cli绑定到127.0.0.1才可以连接Redis server。

为了完成添加新主节点，可以暂时性的关闭保护模式，使用redis-cli，不指定-h参数（但可以指定-p参数，或者-h参数值为127.0.0.1）进入操作界面：CONFIG SET protected-mode no。

注意：6390是新增的节点，而6381是已存在的节点（可为master或slave）。如果需要将6390变成某master（假如为3c3a0c74aae0b56170ccb03a76b60cfe7dc1912e）的slave节点，只需要在6390上执行redis命令（前提：这个master没有负责任何slots，亦即需为一个空master）：



 

新加入的master节点上没有任何数据（slots，运行redis命令cluster nodes可以看到这个情况）。当一个slave想成为master时，由于这个新的master节点不管理任何slots，它不参与选举。可以使用redis-cli的reshard为这个新master节点分配slots，如：



## 11.2. 添加一个新从（slave）节点

以添加“192.168.0.251:6390”为例：



 

“192.168.0.251:6390”为新添加的从节点，“192.168.0.251:6381”可为集群中已有的任意节点，这种方法随机为6390指定一个master，如果想明确指定master，假设目标master的ID为“3c3a0c74aae0b56170ccb03a76b60cfe7dc1912e”，则：



# 12. 删除节点

从集群中删除一个节点命令格式：



 

“127.0.0.1:7000”为集群中任意一个非待删除节点，“node-id”为待删除节点的ID。如果待删除的是master节点，则在删除之前需要将该master负责的slots先全部迁到其它master。



 

如果删除后，其它节点还看得到这个被删除的节点，则可通过FORGET命令解决，需要在所有还看得到的其它节点上执行：



 

FORGET做两件事：

1) 从节点表剔除节点；

2) 在60秒的时间内，阻止相同ID的节点加进来。

# 13. master机器硬件故障

这种情况下，master机器可能无法启动，导致其上的master无法连接，master将一直处于“master,fail”状态，如果是slave则处于“slave,fail”状态。

如果是master，则会它的slave变成了master，因此只需要添加一个新的从节点作为原slave（已变成master）的slave节点。完成后，通过CLUSTER FORGET将故障的master或slave从集群中剔除即可。

！！！请注意，需要在所有node上执行一次“CLUSTER FORGET”，否则可能遇到被剔除node的总是处于handshake状态。

# 14. 检查节点状态

以检查节点“192.168.0.251:6381”的状态为例：



 

如发现如下这样的错误：



 

可以使用redis命令取消slots迁移（5461为slot的ID）：



 

需要注意，须登录到192.168.0.251:6381上执行redis的setslot子命令。

# 15. 变更主从关系

在目标slave上执行，命令格式：



 

假设将“192.168.0.251:6381”的master改为“3c3a0c74aae0b56170ccb03a76b60cfe7dc1912e”：



 

使用命令cluster replicate，参数为master节点ID，注意不是IP和端口，在被迁移的slave上执行该命令。

# 16. slots相关命令



# 17. 迁移slosts

官方参考：https://redis.io/commands/cluster-setslot。

示例：将值为8的slot从源节点A迁移到目标节点B，有如下两种方法：



 

上述操作只是将slot标记为迁移状态，完成迁移还需要执行（在目标node上执行）：



 

其中node-id为目标的Node ID，取消迁移使用“CLUSTER SETSLOT <slot> STABLE”，操作示例：



# 18. 人工主备切换

在需要的slaves节点上执行命令：



 

人工发起failover，其它master会收到“Failover auth granted to 4291f18b5e9729e832ed15ceb6324ce5dfc2ffbe for epoch 31”，每次epoch值增一。



 

成为新master的slave日志：



 

原master收到failover后的日志：



# 19. 查看集群信息

对应的redis命令为：cluster info，示例：



# 20. 禁止指定命令

KEYS命令很耗时，FLUSHDB和FLUSHALL命令可能导致误删除数据，所以线上环境最好禁止使用，可以在Redis配置文件增加如下配置：



# 21. 数据迁移

可使用命令“redis-cli --cluster import”将数据从一个redis集群迁到另一个redis集群。

# 22. 各版本配置文件



# 23. 大压力下Redis参数调整要点

| 参数                   | 建议最小值 | 说明                                                         |
| ---------------------- | ---------- | ------------------------------------------------------------ |
| repl-ping-slave-period | 10         | 每10秒ping一次                                               |
| repl-timeout           | 60         | 60秒超时，也就是ping十次                                     |
| cluster-node-timeout   | 15000      |                                                              |
| repl-backlog-size      | 1GB        | Master对slave的队列大小                                      |
| appendfsync            | no         | 让系统自动刷                                                 |
| save                   |            | 大压力下，调大参数值，以减少写RDB带来的压力："900 20 300 200 60 200000" |
| appendonly             |            | 对于队列，建议单独建立集群，并且设置该值为no                 |

 

为何大压力下要这样调整？

最重要的原因之一Redis的主从复制，两者复制共享同一线程，虽然是异步复制的，但因为是单线程，所以也十分有限。如果主从间的网络延迟不是在0.05左右，比如达到0.6，甚至1.2等，那么情况是非常糟糕的，因此同一Redis集群一定要部署在同一机房内。

这些参数的具体值，要视具体的压力而定，而且和消息的大小相关，比如一条200~500KB的流水数据可能比较大，主从复制的压力也会相应增大，而10字节左右的消息，则压力要小一些。大压力环境中开启appendfsync是十分不可取的，容易导致整个集群不可用，在不可用之前的典型表现是QPS毛刺明显。

这么做的目的是让Redis集群尽可能的避免master正常时触发主从切换，特别是容纳的数据量很大时，和大压力结合在一起，集群会雪崩。

当Redis日志中，出现大量如下信息，即可能意味着相关的参数需要调整了：



# 24. 问题排查

1) 如果最后一条日志为“16367:M 08 Jun 14:48:15.560 # Server started, Redis version 3.2.0”，节点状态始终终于fail状态，则可能是aof文件损坏了，这时可以使用工具edis-check-aof --fix进行修改，如：

../../bin/redis-check-aof --fix appendonly-6380.aof 

0x        a1492b9b: Expected prefix '

AOF analyzed: size=2705928192, ok_up_to=2705927067, diff=1125

This will shrink the AOF from 2705928192 bytes, with 1125 bytes, to 2705927067 bytes

Continue? [y/N]: y

2) in `call': ERR Slot 16011 is already busy (Redis::CommandError)

将所有节点上的配置项cluster-config-file指定的文件删除，然后重新启；或者在所有节点上执行下FLUSHALL命令。

另外，如果使用主机名而不是IP，也可能遇到这个错误，如：“redis-cli create --replicas 1 redis1:6379 redis2:6379 redis3:6379 redis4:6379 redis5:6379 redis6:6379”，可能也会得到错误“ERR Slot 16011 is already busy (Redis::CommandError)”。

3) for lack of backlog (Slave request was: 51875158284)

默认值：

\# redis-cli config get repl-timeout

A) "repl-timeout"

B) "10"

\# redis-cli config get client-output-buffer-limit

A) "client-output-buffer-limit"

B) "normal 0 0 0 slave 268435456 67108864 60 pubsub 33554432 8388608 60"

 

增大：

redis-cli config set "client-output-buffer-limit" "normal 0 0 0 slave 2684354560 671088640 60 pubsub 33554432 8388608 60"

4) 复制中断场景

A) master的slave缓冲区达到限制的硬或软限制大小，与参数client-output-buffer-limit相关；

B) 复制时间超过repl-timeout指定的值，与参数repl-timeout相关。

 

slave反复循环从master复制，如果调整以上参数仍然解决不了，可以尝试删除slave上的aof和rdb文件，然后再重启进程复制，这个时候可能能正常完成复制。

5) 日志文件出现：Asynchronous AOF fsync is taking too long (disk is busy?). Writing the AOF buffer without waiting for fsync to complete, this may slow down Redis.

考虑优化以下配置项：

no-appendfsync-on-rewrite值设为yes

repl-backlog-size和client-output-buffer-limit调大一点

6) 日志文件出现：MISCONF Redis is configured to save RDB snapshots, but is currently not able to persist on disk. Commands that may modify the data set are disabled. Please check Redis logs for details about the error.

考虑设置stop-writes-on-bgsave-error值为“no”。

7) Failover auth granted to

当日志大量反反复复出现下列内容时，很可能表示master和slave间同步和通讯不顺畅，导致无效的failover和状态变更，这个时候需要调大相关参数值，容忍更长的延迟，因此也特别注意集群内所有节点间的网络延迟要尽可能的小，最好达到0.02ms左右的水平，调大参数的代价是主备切换变迟钝。

 

Slave日志：



 

Master日志：

