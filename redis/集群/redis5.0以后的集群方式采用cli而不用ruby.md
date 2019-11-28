## 背景：

​      Redis Cluster 在5.0之后取消了ruby脚本 **redis-trib.rb**的支持（手动命令行添加集群的方式不变），集合到redis-cli里，避免了再安装ruby的相关环境。直接使用redis-clit的参数--cluster 来取代。为方便自己后面查询就说明下如何使用该命令进行Cluster的创建和管理，关于Cluster的相关说明可以查看[官网](https://redis.io/topics/cluster-tutorial)或则[Redis Cluster部署、管理和测试](https://www.cnblogs.com/zhoujinyi/p/6477133.html)。

## 环境：

```
系统版本：Ubuntu 14.04
Redis版本：5.0.5
机器IP：192.168.163.132
```

## 说明：redis-cli --cluster help

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
redis-cli --cluster help
Cluster Manager Commands:
  create         host1:port1 ... hostN:portN   #创建集群
                 --cluster-replicas <arg>      #从节点个数
  check          host:port                     #检查集群
                 --cluster-search-multiple-owners #检查是否有槽同时被分配给了多个节点
  info           host:port                     #查看集群状态
  fix            host:port                     #修复集群
                 --cluster-search-multiple-owners #修复槽的重复分配问题
  reshard        host:port                     #指定集群的任意一节点进行迁移slot，重新分slots
                 --cluster-from <arg>          #需要从哪些源节点上迁移slot，可从多个源节点完成迁移，以逗号隔开，传递的是节点的node id，还可以直接传递--from all，这样源节点就是集群的所有节点，不传递该参数的话，则会在迁移过程中提示用户输入
                 --cluster-to <arg>            #slot需要迁移的目的节点的node id，目的节点只能填写一个，不传递该参数的话，则会在迁移过程中提示用户输入
                 --cluster-slots <arg>         #需要迁移的slot数量，不传递该参数的话，则会在迁移过程中提示用户输入。
                 --cluster-yes                 #指定迁移时的确认输入
                 --cluster-timeout <arg>       #设置migrate命令的超时时间
                 --cluster-pipeline <arg>      #定义cluster getkeysinslot命令一次取出的key数量，不传的话使用默认值为10
                 --cluster-replace             #是否直接replace到目标节点
  rebalance      host:port                                      #指定集群的任意一节点进行平衡集群节点slot数量 
                 --cluster-weight <node1=w1...nodeN=wN>         #指定集群节点的权重
                 --cluster-use-empty-masters                    #设置可以让没有分配slot的主节点参与，默认不允许
                 --cluster-timeout <arg>                        #设置migrate命令的超时时间
                 --cluster-simulate                             #模拟rebalance操作，不会真正执行迁移操作
                 --cluster-pipeline <arg>                       #定义cluster getkeysinslot命令一次取出的key数量，默认值为10
                 --cluster-threshold <arg>                      #迁移的slot阈值超过threshold，执行rebalance操作
                 --cluster-replace                              #是否直接replace到目标节点
  add-node       new_host:new_port existing_host:existing_port  #添加节点，把新节点加入到指定的集群，默认添加主节点
                 --cluster-slave                                #新节点作为从节点，默认随机一个主节点
                 --cluster-master-id <arg>                      #给新节点指定主节点
  del-node       host:port node_id                              #删除给定的一个节点，成功后关闭该节点服务
  call           host:port command arg arg .. arg               #在集群的所有节点执行相关命令
  set-timeout    host:port milliseconds                         #设置cluster-node-timeout
  import         host:port                                      #将外部redis数据导入集群
                 --cluster-from <arg>                           #将指定实例的数据导入到集群
                 --cluster-copy                                 #migrate时指定copy
                 --cluster-replace                              #migrate时指定replace
  help           

For check, fix, reshard, del-node, set-timeout you can specify the host and port of any working node in the cluster.
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

注意：Redis Cluster最低要求是3个主节点

① 创建集群主节点

```
redis-cli --cluster create 192.168.163.132:6379 192.168.163.132:6380 192.168.163.132:6381
```

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
>>> Performing hash slots allocation on 3 nodes...
Master[0] -> Slots 0 - 5460
Master[1] -> Slots 5461 - 10922
Master[2] -> Slots 10923 - 16383
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
Can I set the above configuration? (type 'yes' to accept): yes #slot分配
>>> Nodes configuration updated
>>> Assign a different config epoch to each node
>>> Sending CLUSTER MEET messages to join the cluster
Waiting for the cluster to join
..
>>> Performing Cluster Check (using node 192.168.163.132:6379)
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

② 创建集群主从节点

```
/redis-cli --cluster create 192.168.163.132:6379 192.168.163.132:6380 192.168.163.132:6381 192.168.163.132:6382 192.168.163.132:6383 192.168.163.132:6384 --cluster-replicas 1
```

说明：--cluster-replicas 参数为数字，1表示每个主节点需要1个从节点。

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
>>> Performing hash slots allocation on 6 nodes...
Master[0] -> Slots 0 - 5460
Master[1] -> Slots 5461 - 10922
Master[2] -> Slots 10923 - 16383
Adding replica 192.168.163.132:6383 to 192.168.163.132:6379
Adding replica 192.168.163.132:6384 to 192.168.163.132:6380
Adding replica 192.168.163.132:6382 to 192.168.163.132:6381
>>> Trying to optimize slaves allocation for anti-affinity
[WARNING] Some slaves are in the same host as their master
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
S: 3a1d04983ab6c4ae853f9602dd922d4ebadc4dbf 192.168.163.132:6382
   replicates 56005b9413cbf225783906307a2631109e753f8f
S: 0c21b6cee354594a23f4d5abf0d01b48bdc96d55 192.168.163.132:6383
   replicates 117457eab5071954faab5e81c3170600d5192270
S: f6a6957421b80409106cb36be3c7ba41f3b603ff 192.168.163.132:6384
   replicates 815da8448f5d5a304df0353ca10d8f9b77016b28
Can I set the above configuration? (type 'yes' to accept): yes
>>> Nodes configuration updated
>>> Assign a different config epoch to each node
>>> Sending CLUSTER MEET messages to join the cluster
Waiting for the cluster to join
..
>>> Performing Cluster Check (using node 192.168.163.132:6379)
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
S: 3a1d04983ab6c4ae853f9602dd922d4ebadc4dbf 192.168.163.132:6382
   slots: (0 slots) slave
   replicates 56005b9413cbf225783906307a2631109e753f8f
S: 0c21b6cee354594a23f4d5abf0d01b48bdc96d55 192.168.163.132:6383
   slots: (0 slots) slave
   replicates 117457eab5071954faab5e81c3170600d5192270
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: f6a6957421b80409106cb36be3c7ba41f3b603ff 192.168.163.132:6384
   slots: (0 slots) slave
   replicates 815da8448f5d5a304df0353ca10d8f9b77016b28
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

通过该方式创建的带有从节点的机器不能够自己手动指定主节点，所以如果需要指定的话，需要自己手动指定，先使用①创建好主节点后，再通过③来处理。

③ 添加集群主节点

```
redis-cli --cluster add-node 192.168.163.132:6382 192.168.163.132:6379 
```

说明：为一个指定集群添加节点，需要先连到该集群的任意一个节点IP（192.168.163.132:6379），再把新节点加入。该2个参数的顺序有要求：新加入的节点放前

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
>>> Adding node 192.168.163.132:6382 to cluster 192.168.163.132:6379
>>> Performing Cluster Check (using node 192.168.163.132:6379)
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
>>> Send CLUSTER MEET to node 192.168.163.132:6382 to make it join the cluster.
[OK] New node added correctly.
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

④ 添加集群从节点

```
redis-cli --cluster add-node 192.168.163.132:6382 192.168.163.132:6379 --cluster-slave --cluster-master-id 117457eab5071954faab5e81c3170600d5192270
```

说明：把6382节点加入到6379节点的集群中，并且当做node_id为 117457eab5071954faab5e81c3170600d5192270 的从节点。如果不指定 **--cluster-master-id** 会随机分配到任意一个主节点。

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
>>> Adding node 192.168.163.132:6382 to cluster 192.168.163.132:6379
>>> Performing Cluster Check (using node 192.168.163.132:6379)
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
>>> Send CLUSTER MEET to node 192.168.163.132:6382 to make it join the cluster.
Waiting for the cluster to join
..
>>> Configure node as replica of 192.168.163.132:6379.
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

⑤ 删除节点

```
redis-cli --cluster del-node 192.168.163.132:6384 f6a6957421b80409106cb36be3c7ba41f3b603ff
```

说明：指定IP、端口和node_id 来删除一个节点，从节点可以直接删除，主节点不能直接删除，删除之后，该节点会被shutdown。

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
删除从节点：
redis-cli --cluster del-node 192.168.163.132:6384 f6a6957421b80409106cb36be3c7ba41f3b603ff
>>> Removing node f6a6957421b80409106cb36be3c7ba41f3b603ff from cluster 192.168.163.132:6384
>>> Sending CLUSTER FORGET messages to the cluster...
>>> SHUTDOWN the node.

删除主节点：
redis-cli --cluster del-node 192.168.163.132:6380 815da8448f5d5a304df0353ca10d8f9b77016b28
>>> Removing node 815da8448f5d5a304df0353ca10d8f9b77016b28 from cluster 192.168.163.132:6380
[ERR] Node 192.168.163.132:6380 is not empty! Reshard data away and try again.
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

注意：当被删除掉的节点重新起来之后不能自动加入集群，但其和主的复制还是正常的，也可以通过该节点看到集群信息（通过其他正常节点已经看不到该被del-node节点的信息）。

如果想要再次加入集群，则需要先在该节点执行cluster reset，再用add-node进行添加，进行增量同步复制。

到此，目前整个集群的状态如下：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
192.168.163.132:6379> cluster nodes
815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380@16380 master - 0 1569748297177 2 connected 5461-10922
0c21b6cee354594a23f4d5abf0d01b48bdc96d55 192.168.163.132:6383@16383 slave 56005b9413cbf225783906307a2631109e753f8f 0 1569748295000 4 connected
3a1d04983ab6c4ae853f9602dd922d4ebadc4dbf 192.168.163.132:6382@16382 slave 815da8448f5d5a304df0353ca10d8f9b77016b28 0 1569748295000 5 connected
117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379@16379 myself,master - 0 1569748297000 1 connected 0-5460
56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381@16381 master - 0 1569748295000 3 connected 10923-16383
f6a6957421b80409106cb36be3c7ba41f3b603ff 192.168.163.132:6384@16384 slave 117457eab5071954faab5e81c3170600d5192270 0 1569748298185 6 connected
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

⑥ 检查集群

```
redis-cli --cluster check 192.168.163.132:6384 --cluster-search-multiple-owners
```

说明：任意连接一个集群节点，进行集群状态检查

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
redis-cli --cluster check 192.168.163.132:6384 --cluster-search-multiple-owners
192.168.163.132:6380 (815da844...) -> 0 keys | 5462 slots | 1 slaves.
192.168.163.132:6381 (56005b94...) -> 0 keys | 5461 slots | 1 slaves.
192.168.163.132:6379 (117457ea...) -> 2 keys | 5461 slots | 1 slaves.
[OK] 2 keys in 3 masters.
0.00 keys per slot on average.
>>> Performing Cluster Check (using node 192.168.163.132:6384)
S: f6a6957421b80409106cb36be3c7ba41f3b603ff 192.168.163.132:6384
   slots: (0 slots) slave
   replicates 117457eab5071954faab5e81c3170600d5192270
S: 0c21b6cee354594a23f4d5abf0d01b48bdc96d55 192.168.163.132:6383
   slots: (0 slots) slave
   replicates 56005b9413cbf225783906307a2631109e753f8f
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: 3a1d04983ab6c4ae853f9602dd922d4ebadc4dbf 192.168.163.132:6382
   slots: (0 slots) slave
   replicates 815da8448f5d5a304df0353ca10d8f9b77016b28
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
>>> Check for multiple slot owners...
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

⑦ 集群信息查看

```
redis-cli --cluster info 192.168.163.132:6384
```

说明：检查key、slots、从节点个数的分配情况

```
/redis-cli --cluster info 192.168.163.132:6384
192.168.163.132:6380 (815da844...) -> 0 keys | 5462 slots | 1 slaves.
192.168.163.132:6381 (56005b94...) -> 0 keys | 5461 slots | 1 slaves.
192.168.163.132:6379 (117457ea...) -> 2 keys | 5461 slots | 1 slaves.
[OK] 2 keys in 3 masters.
0.00 keys per slot on average.
```

⑧ 修复集群

```
redis-cli --cluster fix 192.168.163.132:6384 --cluster-search-multiple-owners
```

说明：修复集群和槽的重复分配问题

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
redis-cli --cluster fix 192.168.163.132:6384 --cluster-search-multiple-owners
192.168.163.132:6380 (815da844...) -> 0 keys | 5462 slots | 1 slaves.
192.168.163.132:6381 (56005b94...) -> 0 keys | 5461 slots | 1 slaves.
192.168.163.132:6379 (117457ea...) -> 2 keys | 5461 slots | 1 slaves.
[OK] 2 keys in 3 masters.
0.00 keys per slot on average.
>>> Performing Cluster Check (using node 192.168.163.132:6384)
S: f6a6957421b80409106cb36be3c7ba41f3b603ff 192.168.163.132:6384
   slots: (0 slots) slave
   replicates 117457eab5071954faab5e81c3170600d5192270
S: 0c21b6cee354594a23f4d5abf0d01b48bdc96d55 192.168.163.132:6383
   slots: (0 slots) slave
   replicates 56005b9413cbf225783906307a2631109e753f8f
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: 3a1d04983ab6c4ae853f9602dd922d4ebadc4dbf 192.168.163.132:6382
   slots: (0 slots) slave
   replicates 815da8448f5d5a304df0353ca10d8f9b77016b28
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
>>> Check for multiple slot owners...
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

⑨ 设置集群的超时时间 

```
redis-cli --cluster set-timeout 192.168.163.132:6382 10000
```

说明：连接到集群的任意一节点来设置集群的超时时间参数cluster-node-timeout

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
redis-cli --cluster set-timeout 192.168.163.132:6382 10000
>>> Reconfiguring node timeout in every cluster node...
*** New timeout set for 192.168.163.132:6382
*** New timeout set for 192.168.163.132:6384
*** New timeout set for 192.168.163.132:6383
*** New timeout set for 192.168.163.132:6379
*** New timeout set for 192.168.163.132:6381
*** New timeout set for 192.168.163.132:6380
>>> New node timeout set. 6 OK, 0 ERR.
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

⑩ 集群中执行相关命令

```
redis-cli --cluster call 192.168.163.132:6381 config set requirepass cc
redis-cli -a cc --cluster call 192.168.163.132:6381 config set masterauth cc
redis-cli -a cc --cluster call 192.168.163.132:6381 config rewrite
```

说明：连接到集群的任意一节点来对整个集群的所有节点进行设置。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
redis-cli --cluster call 192.168.163.132:6381 config set cluster-node-timeout 12000
>>> Calling config set cluster-node-timeout 12000
192.168.163.132:6381: OK
192.168.163.132:6383: OK
192.168.163.132:6379: OK
192.168.163.132:6384: OK
192.168.163.132:6382: OK
192.168.163.132:6380: OK......
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

到此，相关集群的基本操作已经介绍完，现在说明集群迁移的相关操作。

### 迁移相关

① **在线迁移slot** ：在线把集群的一些slot从集群原来slot节点迁移到新的节点，即可以完成集群的在线横向扩容和缩容。有2种方式进行迁移

一是根据提示来进行操作：

```
直接连接到集群的任意一节点
redis-cli -a cc --cluster reshard 192.168.163.132:6379
```

信息如下：

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
redis-cli -a cc --cluster reshard 192.168.163.132:6379
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
>>> Performing Cluster Check (using node 192.168.163.132:6379)
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
S: 0c21b6cee354594a23f4d5abf0d01b48bdc96d55 192.168.163.132:6383
   slots: (0 slots) slave
   replicates 56005b9413cbf225783906307a2631109e753f8f
S: 3a1d04983ab6c4ae853f9602dd922d4ebadc4dbf 192.168.163.132:6382
   slots: (0 slots) slave
   replicates 815da8448f5d5a304df0353ca10d8f9b77016b28
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: f6a6957421b80409106cb36be3c7ba41f3b603ff 192.168.163.132:6384
   slots: (0 slots) slave
   replicates 117457eab5071954faab5e81c3170600d5192270
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
How many slots do you want to move (from 1 to 16384)? 1
What is the receiving node ID? 815da8448f5d5a304df0353ca10d8f9b77016b28
Please enter all the source node IDs.
  Type 'all' to use all the nodes as source nodes for the hash slots.
  Type 'done' once you entered all the source nodes IDs.
Source node #1: 117457eab5071954faab5e81c3170600d5192270
Source node #2: done

Ready to move 1 slots.
  Source nodes:
    M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
       slots:[0-5460] (5461 slots) master
       1 additional replica(s)
  Destination node:
    M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
       slots:[5461-10922] (5462 slots) master
       1 additional replica(s)
  Resharding plan:
    Moving slot 0 from 117457eab5071954faab5e81c3170600d5192270
Do you want to proceed with the proposed reshard plan (yes/no)? yes
Moving slot 0 from 192.168.163.132:6379 to 192.168.163.132:6380: 
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

二是根据参数进行操作：

```
redis-cli -a cc --cluster reshard 192.168.163.132:6379 --cluster-from 117457eab5071954faab5e81c3170600d5192270 --cluster-to 815da8448f5d5a304df0353ca10d8f9b77016b28 --cluster-slots 10 --cluster-yes --cluster-timeout 5000 --cluster-pipeline 10 --cluster-replace
```

说明：连接到集群的任意一节点来对指定节点指定数量的slot进行迁移到指定的节点。 

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
>>> Performing Cluster Check (using node 192.168.163.132:6379)
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[11-5460] (5450 slots) master
   1 additional replica(s)
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[0-10],[5461-10922] (5473 slots) master
   1 additional replica(s)
S: 0c21b6cee354594a23f4d5abf0d01b48bdc96d55 192.168.163.132:6383
   slots: (0 slots) slave
   replicates 56005b9413cbf225783906307a2631109e753f8f
S: 3a1d04983ab6c4ae853f9602dd922d4ebadc4dbf 192.168.163.132:6382
   slots: (0 slots) slave
   replicates 815da8448f5d5a304df0353ca10d8f9b77016b28
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: f6a6957421b80409106cb36be3c7ba41f3b603ff 192.168.163.132:6384
   slots: (0 slots) slave
   replicates 117457eab5071954faab5e81c3170600d5192270
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.

Ready to move 10 slots.
  Source nodes:
    M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
       slots:[11-5460] (5450 slots) master
       1 additional replica(s)
  Destination node:
    M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
       slots:[0-10],[5461-10922] (5473 slots) master
       1 additional replica(s)
  Resharding plan:
    Moving slot 11 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 12 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 13 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 14 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 15 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 16 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 17 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 18 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 19 from 117457eab5071954faab5e81c3170600d5192270
    Moving slot 20 from 117457eab5071954faab5e81c3170600d5192270
Moving slot 11 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 12 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 13 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 14 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 15 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 16 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 17 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 18 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 19 from 192.168.163.132:6379 to 192.168.163.132:6380: 
Moving slot 20 from 192.168.163.132:6379 to 192.168.163.132:6380: 
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

② 平衡（rebalance）**slot** ：

1）平衡集群中各个节点的slot数量

```
redis-cli -a cc --cluster rebalance 192.168.163.132:6379
```

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
>>> Performing Cluster Check (using node 192.168.163.132:6379)
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
>>> Rebalancing across 3 nodes. Total weight = 3.00
Moving 522 slots from 192.168.163.132:6380 to 192.168.163.132:6379
##########################################################################################################################################################################################################################################################################################################################################################################################################################################################################################################################################
Moving 500 slots from 192.168.163.132:6380 to 192.168.163.132:6381
####################################################################################################################################################################################################################################################################################################################################################################################################################################################################################################################
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 2）根据集群中各个节点设置的权重等平衡slot数量（不执行，只模拟）

```
redis-cli -a cc --cluster rebalance --cluster-weight 117457eab5071954faab5e81c3170600d5192270=5 815da8448f5d5a304df0353ca10d8f9b77016b28=4 56005b9413cbf225783906307a2631109e753f8f=3 --cluster-simulate 192.168.163.132:6379
```

③ 导入集群

```
redis-cli --cluster import 192.168.163.132:6379 --cluster-from 192.168.163.132:9021 --cluster-replace
```

说明：外部Redis实例（9021）导入到集群中的任意一节点。

![img](https://images.cnblogs.com/OutliningIndicators/ExpandedBlockStart.gif)

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
>>> Importing data from 192.168.163.132:9021 to cluster 192.168.163.132:6379
>>> Performing Cluster Check (using node 192.168.163.132:6379)
M: 117457eab5071954faab5e81c3170600d5192270 192.168.163.132:6379
   slots:[1366-5961],[11423-12287] (5461 slots) master
   1 additional replica(s)
M: 815da8448f5d5a304df0353ca10d8f9b77016b28 192.168.163.132:6380
   slots:[1365],[5962-11422] (5462 slots) master
   1 additional replica(s)
S: 0c21b6cee354594a23f4d5abf0d01b48bdc96d55 192.168.163.132:6383
   slots: (0 slots) slave
   replicates 56005b9413cbf225783906307a2631109e753f8f
S: 3a1d04983ab6c4ae853f9602dd922d4ebadc4dbf 192.168.163.132:6382
   slots: (0 slots) slave
   replicates 815da8448f5d5a304df0353ca10d8f9b77016b28
M: 56005b9413cbf225783906307a2631109e753f8f 192.168.163.132:6381
   slots:[0-1364],[12288-16383] (5461 slots) master
   1 additional replica(s)
S: f6a6957421b80409106cb36be3c7ba41f3b603ff 192.168.163.132:6384
   slots: (0 slots) slave
   replicates 117457eab5071954faab5e81c3170600d5192270
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
*** Importing 97847 keys from DB 0
Migrating 9223372011174675807 to 192.168.163.132:6381: OK
Migrating 9223372033047675807 to 192.168.163.132:6381: OK
...
...
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

注意：测试下来发现参数--cluster-replace没有用，如果集群中已经包含了某个key，在导入的时候会失败，不会覆盖，只有清空集群key才能导入。

```
*** Importing 97847 keys from DB 0
Migrating 9223372011174675807 to 192.168.163.132:6381: Source 192.168.163.132:9021 replied with error:
ERR Target instance replied with error: BUSYKEY Target key name already exists
```

并且发现如果集群设置了密码，也会导入失败，需要设置集群密码为空才能进行导入（call）。通过monitor（9021）的时候发现，在migrate的时候需要密码进行auth认证。 

## 总结：

​      Redis Cluster 通过redis-cli --cluster来创建和管理集群的方式和 **redis-trib.rb**脚本绝大部分都是一样的，所以对于比较熟悉 redis-trib.rb 脚本的，使用--cluster也非常顺手。