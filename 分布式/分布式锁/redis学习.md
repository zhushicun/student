```java
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```java
/**
 * @author koma <komazhang@foxmail.com>
 * @date 2018-09-19 11:24
 */
@Slf4j
@Service
public class CacheService {
    private static final Long RELEASE_SUCCESS = 1L;
    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "EX";
    private static final String RELEASE_LOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 该加锁方法仅针对单实例 Redis 可实现分布式加锁
     * 对于 Redis 集群则无法使用
     *
     * 支持重复，线程安全
     *
     * @param lockKey   加锁键
     * @param clientId  加锁客户端唯一标识(采用UUID)
     * @param seconds   锁过期时间
     * @return
     */
    public Boolean tryLock(String lockKey, String clientId, long seconds) {
        redisTemplate.opsForValue().set();
        return redisTemplate.execute((RedisCallback<Boolean>) redisConnection -> {
            Jedis jedis = (Jedis) redisConnection.getNativeConnection();
            String result = jedis.set(lockKey, clientId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, seconds);
            if (LOCK_SUCCESS.equals(result)) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        });
    }

    /**
     * 与 tryLock 相对应，用作释放锁
     *
     * @param lockKey
     * @param clientId
     * @return
     */
    public Boolean releaseLock(String lockKey, String clientId) {
        return redisTemplate.execute((RedisCallback<Boolean>) redisConnection -> {
            Jedis jedis = (Jedis) redisConnection.getNativeConnection();
            Object result = jedis.eval(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey),
                    Collections.singletonList(clientId));
            if (RELEASE_SUCCESS.equals(result)) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        });
    }
}
```

上述代码实现，仅对 redis 单实例架构有效，当面对 redis 集群时就无效了。但是一般情况下，我们的 redis 架构多数会做成“主备”模式，然后再通过 redis 哨兵实现主从切换，这种模式下我们的应用服务器直接面向主机，也可看成是单实例，因此上述代码实现也有效。但是当在主机宕机，从机被升级为主机的一瞬间的时候，如果恰好在这一刻，由于 redis 主从复制的异步性，导致从机中数据没有即时同步，那么上述代码依然会无效，导致同一资源有可能会产生两把锁，违背了分布式锁的原则。

  为什么上面的代码可以实现分布式锁，根本原因在于 redis 对 set 命令中的 NX 选项和对 lua 脚本的执行都是原子的，因此当多个客户端去争抢执行上锁或解锁代码时，最终只会有一个客户端执行成功。同时 set 命令还可以指定key的有效期，这样即使当前客户端奔溃，过一段时间锁也会被 redis 自动释放，这就给了其它客户端获取锁的机会。



  上述代码不能使用 spring-boot 提供的 redisTemplate.opsForValue().set() 命令是因为 spring-boot 对 jedis 的封装中没有返回 set 命令的返回值，这就导致上层没有办法判断 set 执行的结果，因此需要通过 execute 方法调用 RedisCallback 去拿到底层的 Jedis 对象，来直接调用 set 命令。这个问题主要是在 spring-data-redis 的封装上，了解即可。 



 **四，分布式锁的原则**

- 独享: 即互斥属性，在同一时刻，一个资源只能有一把锁被一个客户端持有
- 无死锁: 当持有锁的客户端奔溃后，锁仍然可以被其它客户端获取
- 容错性: 当部分节点失活之后，其余节点客户端依然可以获取和释放锁
- 统一性: 即释放锁的客户端只能由获取锁的客户端释放



**五，一类常见错误实现和推荐使用方式**

```java
if (redisTemplate.opsForValue().setIfAbsent(lockKey, clientId)) {
    //这里存在宕机风险，导致设置有效期失败
    redisTemplate.expire(lockKey, seconds, TimeUnit.SECONDS);
}
```

  这是一种典型的错误实现，在早期的 redis 分布式锁实践中我们经常可以看到类似的实现，其中 spring-boot 中的 setIfAbsent 方法在底层调用的是 redis 的 setNx 命令，该命令和 set 命令的 NX 选项具有同样的功能，但是 setNx 命令不能够设置 key 的有效期，这也是为什么我们会在获取到锁之后马上去设置锁的有效期，但是恰好这里却隐藏着风险，因为这一整个操作并非是原子的。



```java
if (clientId.equals(redisTemplate.opsForValue().get(lockKey))) {
    redisTemplate.delete(lockKey);
}
```

  对于解锁代码，也存在同样的风险，因为在执行 delete 的时候，lockKey 现在可能已经被另外一个客户端持有了，那么这里直接删除就是删除了其它客户端的锁，导致的最终结果就是真正应该持有锁的客户端在没有完全执行完之后，锁又被另外的客户端持有了，这样一个资源就产生了两把锁，同样违背了分布式锁的原则。



  **推荐**的使用方式是，当 redis 的架构如上图所示一样是单实例模式时，如果存在主备且可以忍受小概率的锁出错，那么就可以直接使用上述代码，当然最严谨的方式还是使用官方的 Redlock 算法实现。其中 Java 包推荐使用 [redisson](https://github.com/redisson/redisson)。