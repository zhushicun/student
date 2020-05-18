# redis分布式锁的使用
传统的使用redis做分布式锁，一般会使用lua脚本，因为redis在执行命令的时候是具有原子性。但是存在一种情况
set命令要用set key value px milliseconds nx;value要具有唯一性;释放锁时要验证value值，不能误解锁;

事实上这类琐最大的缺点就是它加锁时只作用在一个Redis节点上，即使Redis通过sentinel保证高可用，如果这个master节点由于某些原因发生了主从切换，那么就会出现锁丢失的情况:

在Redis的master节点上拿到了锁;但是这个加锁的key还没有同步到slave节点;master故障，发生故障转移，slave节点升级为master节点;导致锁丢失。

正因为如此，Redis作者antirez基于分布式环境下提出了一种更高级的分布式锁的实现方式:Redlock。笔者认为，Redlock也是Redis所有分布式锁实现方式中唯一能让面试官高潮的方式。

Redlock实现

antirez提出的redlock算法大概是这样的:

在Redis的分布式环境中，我们假设有N个Redis master。这些节点完全互相独立，不存在主从复制或者其他集群协调机制。我们确保将在N个实例上使用与在Redis单实例下相同方法获取和释放锁。现在我们假设有5个Redis master节点，同时我们需要在5台服务器上面运行这些Redis实例，这样保证他们不会同时都宕掉。

为了取到锁，客户端应该执行以下操作:

获取当前Unix时间，以毫秒为单位。依次尝试从5个实例，使用相同的key和具有唯一性的value(例如UUID)获取锁。当向Redis请求获取锁时，客户端应该设置一个网络连接和响应超时时间，这个超时时间应该小于锁的失效时间。例如你的锁自动失效时间为10秒，则超时时间应该在5-50毫秒之间。这样可以避免服务器端Redis已经挂掉的情况下，客户端还在死死地等待响应结果。如果服务器端没有在规定时间内响应，客户端应该尽快尝试去另外一个Redis实例请求获取锁。客户端使用当前时间减去开始获取锁时间(步骤1记录的时间)就得到获取锁使用的时间。当且仅当从大多数(N/2+1，这里是3个节点)的Redis节点都取到锁，并且使用的时间小于锁失效时间时，锁才算获取成功。如果取到了锁，key的真正有效时间等于有效时间减去获取锁所使用的时间(步骤3计算的结果)。如果因为某些原因，获取锁失败(没有在至少N/2+1个Redis实例取到锁或者取锁时间已经超过了有效时间)，客户端应该在所有的Redis实例上进行解锁(即便某些Redis实例根本就没有加锁成功，防止某些节点获取到锁但是客户端没有得到响应而导致接下来的一段时间不能被重新获取锁)。Redlock源码

redisson已经有对redlock算法封装，接下来对其用法进行简单介绍，并对核心源码进行分析(假设5个redis实例)。
## gradle
```json
compile 'org.redisson:redisson-spring-boot-starter:3.12.5'
```


## 配置文件引入
文件名称 application-release.yml
文件名称自定义，然后在springboot配置文件的主文件中引入

```json
spring:
	redis:
	    redisson:
	      config: redisson-release.yaml
```

```json
#Redisson配置
singleServerConfig:
  address: "redis://域名:6379"
  password:密码
  clientName: null
  database: 6 #选择使用哪个数据库0~15
  idleConnectionTimeout: 10000
  connectTimeout: 10000
  timeout: 3000
  retryAttempts: 3
  retryInterval: 1500
  subscriptionsPerConnection: 5
  subscriptionConnectionMinimumIdleSize: 1
  subscriptionConnectionPoolSize: 50
  connectionMinimumIdleSize: 32
  connectionPoolSize: 64
  dnsMonitoringInterval: 5000
  #dnsMonitoring: false

threads: 10
nettyThreads: 10
codec: !<org.redisson.codec.JsonJacksonCodec> {}
transportMode: "NIO"

```

## java code
```json
package com.kc.utils.config;

import com.kc.utils.cache.lock.DistributedLocker;
import com.kc.utils.cache.lock.RedissLockUtil;
import com.kc.utils.cache.lock.RedissonDistributedLocker;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
public class RedissonAutoConfiguration {

    @Value("${spring.redis.redisson.config}")
    private String activeFileName;


    @Bean
    public RedissonClient redissonClient() throws IOException {
        //Config config = new Config();
        // 本例子使用的是yaml格式的配置文件，读取使用Config.fromYAML，如果是Json文件，则使用Config.fromJSON
        Config config = Config.fromYAML(RedissonAutoConfiguration.class.getClassLoader().getResource(activeFileName));
        return Redisson.create(config);
    }



    /**
     * 装配locker类，并将实例注入到RedissLockUtil中
     * @return
     */
    @Bean
    DistributedLocker distributedLocker(RedissonClient redissonClient) {
        RedissonDistributedLocker locker = new RedissonDistributedLocker();
        locker.setRedissonClient(redissonClient);
        RedissLockUtil.setLocker(locker);
        return locker;
    }

}
```

## 分布式锁的使用

```json
package com.kc.utils.cache.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.kc.models.exceptions.RedisLockException;
import org.redisson.api.RLock;


/**
 * redis分布式锁帮助类
 *
 */
public class RedissLockUtil {

    private static DistributedLocker redissLock;
    
    public static void setLocker(DistributedLocker locker) {
        redissLock = locker;
    }
    
    /**
     * 加锁
     * @param lockKey
     * @return
     */
    public static RLock lock(String lockKey) {
        return redissLock.lock(lockKey);
    }

    /**
     * 释放锁 默认当前线程
     * @param lockKey
     */
    public static void unlock(String lockKey) {
        redissLock.unlock(lockKey);
    }

    /**
     * 根据标记释放锁
     * @param lockKey
     * @param flag
     */
    public static void unlockByFlag(String lockKey,Boolean flag) {
        if(flag){
            redissLock.unlock(lockKey);
        }
    }

    /**
     * 释放锁
     * @param lock
     */
    public static void unlock(RLock lock) {
        redissLock.unlock(lock);
    }

    /**
     * 带超时的锁
     * @param lockKey
     * @param timeout 超时时间   单位：秒
     */
    public static RLock lock(String lockKey, int timeout) {
        return redissLock.lock(lockKey, timeout);
    }
    
    /**
     * 带超时的锁
     * @param lockKey
     * @param unit 时间单位
     * @param timeout 超时时间
     */
    public static RLock lock(String lockKey, TimeUnit unit ,int timeout) {
        return redissLock.lock(lockKey, unit, timeout);
    }
    
    /**
     * 尝试获取锁
     * @param lockKey
     * @param waitTime 最多等待时间
     * @param leaseTime 上锁后自动释放锁时间
     * @return
     */
    public static boolean tryLock(String lockKey, int waitTime, int leaseTime) {
        return redissLock.tryLock(lockKey, TimeUnit.SECONDS, waitTime, leaseTime);
    }
    
    /**
     * 尝试获取锁
     * @param lockKey
     * @param unit 时间单位
     * @param waitTime 最多等待时间
     * @param leaseTime 上锁后自动释放锁时间
     * @return
     */
    public static boolean tryLock(String lockKey, TimeUnit unit, int waitTime, int leaseTime) {
        return redissLock.tryLock(lockKey, unit, waitTime, leaseTime);
    }

    /**
     * 尝试获取keys个数的锁集合
     * @author shawn
     * @param keys 锁的key值集合
     * @param unit 时间单位
     * @param waitTime 等待时间
     * @param leaseTime 存活时间
     * @return {@link KeysResult}
     * @date 2020/4/14
     **/
    public static KeysResult tryLockKeys(List<String> keys, TimeUnit unit, int waitTime, int leaseTime) throws RedisLockException {
        List<LockKeys> keysGroup = new ArrayList<>();
        KeysResult keysResult = new KeysResult();
        keysResult.setStatus(true);
        for (String key : keys) {
            if(key == null){
                String msg = "分布式锁的KEY值为NULL";
                throw new RedisLockException(msg);
            }
            if(redissLock.tryLock(key, unit, waitTime, leaseTime)){
                LockKeys lockKeys = new LockKeys();
                lockKeys.setKey(key);
                lockKeys.setStatus(true);
                keysGroup.add(lockKeys);
            }else{
                if(keysResult.getStatus()){
                    keysResult.setStatus(false);
                }
                break;
            }
        }
        keysResult.setKeys(keysGroup);
        return keysResult;
    }

    /**
     * 解分布式锁
     * @author shawn
     * @param keysResult 分布式锁集合对象
     * @return
     * @date 2020/4/14
     **/
    public static void unLockKeys(KeysResult keysResult){
        keysResult.getKeys().forEach(a->{
            Boolean flag = a.getStatus();
            if(flag){
                String key = a.getKey();
                if(key!=null) {
                    redissLock.unlock(key);
                }
            }
        });
    }
}
```

```json
trylock
unlock
finally{
	if(锁的标记){
		unlock
	}
}

```