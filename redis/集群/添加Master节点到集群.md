在[Redis集群(一)](http://my.oschina.net/tongyufu/blog/406829)中讲了Redis集群的基本搭建。这一节主要讲对Redis集群的操作。

Redis版本：5.0



### 添加Master节点到集群

- 按照Redis集群一的方式，创建端口为7003的新实例，并启动该实例
- 将7003添加到集群：
  第二个参数127.0.0.1:7000为当前集群已存在的节点，这里只要是该集群中的任意一个可用节点都可以，不要求必须是第一个。
  新节点不能有数据，否则会报错：[ERR] Node 127.0.0.1:7003 is not empty. Either the node already knows other nodes (check with CLUSTER NODES) or contains some key in database 0。一般是直接复制正在使用的redis目录导致的，手动删除根目录下的dump.rdb文件即可。

```bash
./redis-cli --cluster add-node 127.0.0.1:7003 127.0.0.1:7000
返回信息：
>>> Adding node 127.0.0.1:7003 to cluster 127.0.0.1:7000
>>> Performing Cluster Check (using node 127.0.0.1:7000)
M: 3bcdfbed858bbdd92dd760632b9cb4c649947fed 127.0.0.1:7000
   slots:[0-5460] (5461 slots) master
M: 9b022d79cf860c87dc2190cdffc55b282dd60e42 127.0.0.1:7002
   slots:[10923-16383] (5461 slots) master
M: 2a8f29e22ec38f56e062f588e5941da24a2bafa0 127.0.0.1:7001
   slots:[5461-10922] (5462 slots) master
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
>>> Send CLUSTER MEET to node 127.0.0.1:7003 to make it join the cluster.
[OK] New node added correctly.
```

查看操作后的节点信息：

```bash
./redis-cli -c -p 7003
127.0.0.1:7003> cluster nodes
返回信息：
2a8f29e22ec38f56e062f588e5941da24a2bafa0 127.0.0.1:7001@17001 master - 0 1542787865456 2 connected 5461-10922
a70d7fff6d6dde511cb7cb632a347be82dd34643 127.0.0.1:7003@17003 myself,slave 3bcdfbed858bbdd92dd760632b9cb4c649947fed 0 1542787863000 0 connected
9b022d79cf860c87dc2190cdffc55b282dd60e42 127.0.0.1:7002@17002 master - 0 1542787865000 3 connected 10923-16383
3bcdfbed858bbdd92dd760632b9cb4c649947fed 127.0.0.1:7000@17000 master - 0 1542787862000 1 connected 0-5460
```

可以看到7003节点的connected后面没有Hash槽(slot)，新加入的加点是一个主节点， 当集群需要将某个从节点升级为新的主节点时， 这个新节点不会被选中，也不会参与选举。

- 给新节点分配哈希槽：

```bash
#参数127.0.0.1:7000只是表示连接到这个集群，具体对哪个节点进行操作后面会提示输入
./redis-cli --cluster reshard 127.0.0.1:7000
返回信息：
>>> Performing Cluster Check (using node 127.0.0.1:7000)
M: 3bcdfbed858bbdd92dd760632b9cb4c649947fed 127.0.0.1:7000
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
M: 9b022d79cf860c87dc2190cdffc55b282dd60e42 127.0.0.1:7002
   slots:[10923-16383] (5461 slots) master
S: a70d7fff6d6dde511cb7cb632a347be82dd34643 127.0.0.1:7003
   slots: (0 slots) slave
   replicates 3bcdfbed858bbdd92dd760632b9cb4c649947fed
M: 2a8f29e22ec38f56e062f588e5941da24a2bafa0 127.0.0.1:7001
   slots:[5461-10922] (5462 slots) master
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.

#根据提示选择要迁移的slot数量(这里选择1000)
How many slots do you want to move (from 1 to 16384)? 1000

#选择要接受这些slot的node-id(这里是7003)
What is the receiving node ID? a70d7fff6d6dde511cb7cb632a347be82dd34643

#选择slot来源:
#all表示从所有的master重新分配，
#或者数据要提取slot的master节点id(这里是7000),最后用done结束
Please enter all the source node IDs.
  Type 'all' to use all the nodes as source nodes for the hash slots.
  Type 'done' once you entered all the source nodes IDs.
Source node #1:3bcdfbed858bbdd92dd760632b9cb4c649947fed
Source node #2:done

#打印被移动的slot后，输入yes开始移动slot以及对应的数据.
Do you want to proceed with the proposed reshard plan (yes/no)? yes
#结束
```

- 查看操作结果：

```bash
./redis-cli -c -p 7000
cluster nodes
返回信息：
9b022d79cf860c87dc2190cdffc55b282dd60e42 127.0.0.1:7002@17002 master - 0 1542790503483 3 connected 10923-16383
3bcdfbed858bbdd92dd760632b9cb4c649947fed 127.0.0.1:7000@17000 myself,master - 0 1542790503000 1 connected 1000-5460
e852e07181f20dd960407e5b08f7122870f67c89 127.0.0.1:7003@17003 master - 0 1542790502458 4 connected 0-999
2a8f29e22ec38f56e062f588e5941da24a2bafa0 127.0.0.1:7001@17001 master - 0 1542790504513 2 connected 5461-10922
```

​    可以看到返回的集群信息中，7003拥有了0-999哈希槽，而7000变成了1000-5460



### 添加Slave节点到集群

- 按照Redis集群一的方式，创建端口为7004的新实例，并启动该实例
- 将7004添加到集群：
  由于没有指定master节点，所以redis会自动分配master节点，这里把7000作为7004的master。
  注意：add-node命令后面的127.0.0.1:7000并不是指7000作为新节点的master。

```bash
./redis-cli --cluster add-node 127.0.0.1:7004 127.0.0.1:7000 --cluster-slave
返回信息：
>>> Adding node 127.0.0.1:7004 to cluster 127.0.0.1:7000
>>> Performing Cluster Check (using node 127.0.0.1:7000)
M: 3bcdfbed858bbdd92dd760632b9cb4c649947fed 127.0.0.1:7000
   slots:[1000-5460] (4461 slots) master
M: 9b022d79cf860c87dc2190cdffc55b282dd60e42 127.0.0.1:7002
   slots:[10923-16383] (5461 slots) master
M: e852e07181f20dd960407e5b08f7122870f67c89 127.0.0.1:7003
   slots:[0-999] (1000 slots) master
M: 2a8f29e22ec38f56e062f588e5941da24a2bafa0 127.0.0.1:7001
   slots:[5461-10922] (5462 slots) master
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
Automatically selected master 127.0.0.1:7000
>>> Send CLUSTER MEET to node 127.0.0.1:7004 to make it join the cluster.
Waiting for the cluster to join

>>> Configure node as replica of 127.0.0.1:7000.
[OK] New node added correctly.
```

- 也可以添加时指定master节点：
  --cluster-master-id为master节点的 id

```bash
./redis-cli --cluster add-node 127.0.0.1:7004 127.0.0.1:7000 --cluster-slave --cluster-master-id 2a8f29e22ec38f56e062f588e5941da24a2bafa0
```

- 更改master节点为7002：

```bash
./redis-cli -p 7004
127.0.0.1:7004> cluster replicate 9b022d79cf860c87dc2190cdffc55b282dd60e42
OK
```



### 删除一个Slave节点

```bash
#redis-trib del-node ip:port '<node-id>'
#这里移除的是7004
./redis-cli --cluster del-node 127.0.0.1:7000 74957282ffa94c828925c4f7026baac04a67e291
返回信息：
>>> Removing node 74957282ffa94c828925c4f7026baac04a67e291 from cluster 127.0.0.1:7000
>>> Sending CLUSTER FORGET messages to the cluster...
>>> SHUTDOWN the node.
```



### 删除一个Master节点

删除master节点之前首先要使用reshard移除master的全部slot,然后再删除当前节点(目前只能把被删除master的slot迁移到一个节点上)

```bash
./redis-cli --cluster reshard 127.0.0.1:7000
#根据提示选择要迁移的slot数量(7003上有1000个slot全部转移)
How many slots do you want to move (from 1 to 16384)? 1000
#选择要接受这些slot的node-id
What is the receiving node ID? 3bcdfbed858bbdd92dd760632b9cb4c649947fed
#选择slot来源:
#all表示从所有的master重新分配，
#或者数据要提取slot的master节点id,最后用done结束
Please enter all the source node IDs.
  Type 'all' to use all the nodes as source nodes for the hash slots.
  Type 'done' once you entered all the source nodes IDs.
Source node #1:a70d7fff6d6dde511cb7cb632a347be82dd34643
Source node #2:done
#打印被移动的slot后，输入yes开始移动slot以及对应的数据.
#Do you want to proceed with the proposed reshard plan (yes/no)? yes
#结束

#删除空master节点
./redis-cli --cluster del-node 127.0.0.1:7000 'a70d7fff6d6dde511cb7cb632a347be82dd34643'
```