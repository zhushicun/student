之前文章介绍过一种单点部署服务防重复提交的一种方式，但是实际开发中，单点服务是很少见得，之前的那种防重复提交的方案在分布式环境下也就嗝屁了。本文实现一种分布式服务防重复提交的方案，跟之前那篇文章的思想是一致的，也就是是一线一个锁，在方法请求前，要先获取锁，不同的是，本文的锁是分布式锁，而之前那篇文章的锁是本地锁。其实分布式锁的实现方式有很多种，比如使用Mysql、或者Zookeeper等都可以实现分布式锁。Mysql实现的方式存在单点的不足，实际开发中使用比较少。比较常用的方式是使用Redis或者Zookeeper实现分布式锁，本篇文章简单介绍一下如何使用Redis实现分布式锁，后续文章我会介绍通过Zookeeper客户端Curator的实现方式。

1. 项目结构
|   pom.xml
|   springboot-17-distributed-repeat-submit.iml
|
+---src
|   +---main
|   |   +---java
|   |   |   \---com
|   |   |       \---zhuoli
|   |   |           \---service
|   |   |               \---springboot
|   |   |                   \---distributed
|   |   |                       \---repeat
|   |   |                           \---submit
|   |   |                               |   AntiRepeatedSubmitApplicationContext.java
|   |   |                               |
|   |   |                               +---annotation
|   |   |                               |       CacheLock.java
|   |   |                               |       CacheParam.java
|   |   |                               |
|   |   |                               +---aop
|   |   |                               |       LockMethodInterceptor.java
|   |   |                               |
|   |   |                               +---common
|   |   |                               |   |   User.java
|   |   |                               |   |
|   |   |                               |   +---keygenerator
|   |   |                               |   |   |   CacheKeyGenerator.java
|   |   |                               |   |   |
|   |   |                               |   |   \---impl
|   |   |                               |   |           LockKeyGenerator.java
|   |   |                               |   |
|   |   |                               |   \---redis
|   |   |                               |           RedisConfig.java
|   |   |                               |           RedisLockHelper.java
|   |   |                               |
|   |   |                               +---controller
|   |   |                               |       UserController.java
|   |   |                               |
|   |   |                               \---service
|   |   |                                   |   UserControllerService.java
|   |   |                                   |
|   |   |                                   \---impl
|   |   |                                           UserControllerServiceImpl.java
|   |   |
|   |   \---resources
|   \---test
|       \---java    
CacheLock.java、CacheParam.java连个类为自定义注解接口，CacheLock方法注解用来指定分布式锁的key前缀和失效时间等信息，CacheParam参数注解用于确定分布式锁的key。
LockKeyGenerator.java为切面，用于拦截@CacheParam注解，生成分布式锁的key
LockMethodInterceptor.java为切面，用于拦截@CacheLock方法，实现在执行方法之前要先获取锁逻辑
RedisLockHelper.java为分布式锁的实现
2. pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.zhuoli.service</groupId>
    <artifactId>springboot-17-distributed-repeat-submit</artifactId>
    <version>1.0-SNAPSHOT</version>

    <!-- Spring Boot 启动父依赖 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.0.3.RELEASE</version>
    </parent>

    <dependencies>
        <!-- Exclude Spring Boot's Default Logging -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
     
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
     
        <!-- https://mvnrepository.com/artifact/redis.clients/jedis -->
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>2.9.0</version>
        </dependency>
     
        <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.2</version>
            <scope>provided</scope>
        </dependency>
     
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>21.0</version>
        </dependency>

    </dependencies>

</project>
3. 自定义注解
3.1 @CacheParam
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CacheParam {
    /**
     * 字段名称
       *
     * @return String
       */
    String name() default "";
}
3.2 @CacheLock
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CacheLock {

    /**
     * redis 锁key的前缀
       */
    String prefix() default "";

    /**
     * redis key过期时间
       */
    int expire() default 5;

    /**
     * 超时时间单位
       *
       */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * Key分隔符
     * 比如：Key:1
       */
    String delimiter() default ":";
}
4. 分布式锁key生成
public class LockKeyGenerator implements CacheKeyGenerator {

    @Override
    public String getLockKey(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        CacheLock lockAnnotation = method.getAnnotation(CacheLock.class);
        final Object[] args = pjp.getArgs();
        final Parameter[] parameters = method.getParameters();
        StringBuilder builder = new StringBuilder();
        //默认解析方法里面带CacheParam注解的属性,如果没有尝试着解析实体对象中的CacheParam注解属性
        for (int i = 0; i < parameters.length; i++) {
            final CacheParam annotation = parameters[i].getAnnotation(CacheParam.class);
            if (annotation == null) {
                continue;
            }
            builder.append(lockAnnotation.delimiter()).append(args[i]);
        }
        if (StringUtils.isEmpty(builder.toString())) {
            //CacheLock注解的方法参数没有CacheParam注解，则迭代解析参数实体中的CacheParam注解属性
            final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                final Object object = args[i];
                final Field[] fields = object.getClass().getDeclaredFields();
                for (Field field : fields) {
                    final CacheParam annotation = field.getAnnotation(CacheParam.class);
                    if (annotation == null) {
                        continue;
                    }
                    field.setAccessible(true);
                    builder.append(lockAnnotation.delimiter()).append(ReflectionUtils.getField(field, object));
                }
            }
        }
        return lockAnnotation.prefix() + builder.toString();
    }
}
5. 分布式锁实现
5.1 Redis配置
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration("127.0.0.1", 6379);
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

}
配置之前在讲redis的时候都已经讲过了，这里不多说了

5.2 分布式锁实现
@Configuration
@RequiredArgsConstructor
public class RedisLockHelper {
    private static final String DELIMITER = "|";

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10);
     
    private final StringRedisTemplate stringRedisTemplate;
     
    /**
     * 获取锁
     * @param lockKey lockKey
     * @param uuid    UUID
     * @param timeout 超时时间
     * @param unit    过期单位
     * @return true or false
     */
    public boolean lock(String lockKey, final String uuid, long timeout, final TimeUnit unit) {
        final long milliseconds = Expiration.from(timeout, unit).getExpirationTimeInMilliseconds();
        boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, (System.currentTimeMillis() + milliseconds) + DELIMITER + uuid);
        if (success) {
            /*设置过期时间，防止系统崩溃而导致锁迟迟不释放形成死锁*/
            stringRedisTemplate.expire(lockKey, timeout, unit);
        } else {
            String oldVal = stringRedisTemplate.opsForValue().get(lockKey);
            final String[] oldValues = oldVal.split(Pattern.quote(DELIMITER));
            /*缓存已经到过期时间，但是还没释放，避免ddl失效造成死锁*/
            if (Long.parseLong(oldValues[0]) + unit.toSeconds(1) <= System.currentTimeMillis()) {
                stringRedisTemplate.opsForValue().set(lockKey, (System.currentTimeMillis() + milliseconds) + DELIMITER + uuid);
                stringRedisTemplate.expire(lockKey, timeout, unit);
                return true;
            }
        }
        return success;
    }
     
    public void unlock(String lockKey, String value) {
        unlock(lockKey, value, 0, TimeUnit.MILLISECONDS);
    }
     
    /**
     * 延迟unlock
     *
     * @param lockKey   key
     * @param uuid
     * @param delayTime 延迟时间
     * @param unit      时间单位
     */
    private void unlock(final String lockKey, final String uuid, long delayTime, TimeUnit unit) {
        if (StringUtils.isEmpty(lockKey)) {
            return;
        }
        if (delayTime <= 0) {
            doUnlock(lockKey, uuid);
        } else {
            /*定时任务延迟unlock*/
            EXECUTOR_SERVICE.schedule(() -> doUnlock(lockKey, uuid), delayTime, unit);
        }
    }
     
    /**
     * @param lockKey key
     * @param uuid
     */
    private void doUnlock(final String lockKey, final String uuid) {
        String val = stringRedisTemplate.opsForValue().get(lockKey);
        final String[] values = val.split(Pattern.quote(DELIMITER));
        if (values.length <= 0) {
            return;
        }
        if (uuid.equals(values[1])) {
            stringRedisTemplate.delete(lockKey);
        }
    }

}
简单讲一下锁的实现，Redis是线程安全的，利用该的特性可以很轻松的实现一个分布式锁。opsForValue().setIfAbsent(key,value)的作用是如果缓存中没有当前Key则进行缓存同时返回true，否则返回false。只靠这一个逻辑其实也算是实现了锁，但是为了防止防止系统崩溃而导致锁迟迟不释放形成死锁，或者Redis ddl失效导致死锁，又添加一些比如key失效时间等逻辑。可以仔细读一下，并不难理解。

6. 分布式锁切面
拦截@CacheLock注解方法，在方法执行前增加获取锁逻辑

@Aspect
@Configuration
@AllArgsConstructor
public class LockMethodInterceptor {
    private final RedisLockHelper redisLockHelper;

    private final CacheKeyGenerator cacheKeyGenerator;
     
    @Around("execution(public * *(..)) && @annotation(com.zhuoli.service.springboot.distributed.repeat.submit.annotation.CacheLock)")
    public Object interceptor(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        CacheLock lock = method.getAnnotation(CacheLock.class);
        if (StringUtils.isEmpty(lock.prefix())) {
            throw new RuntimeException("lock key don't null...");
        }
        final String lockKey = cacheKeyGenerator.getLockKey(pjp);
        String value = UUID.randomUUID().toString();
        try {
            final boolean success = redisLockHelper.lock(lockKey, value, lock.expire(), lock.timeUnit());
            if (!success) {
                throw new RuntimeException("重复提交");
            }
            try {
                return pjp.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException("系统异常");
            }
        } finally {
            //如果演示的话需要注释该代码，实际应该放开
            redisLockHelper.unlock(lockKey, value);
        }
    }
}
7. 切面使用
@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private UserControllerService userControllerService;

    @CacheLock(prefix = "user")
    @RequestMapping(value = "/get_user", method = RequestMethod.POST)
    public ResponseEntity getUserById(@CacheParam(name = "token") @RequestParam Long id){
        return ResponseEntity.status(HttpStatus.OK).body(userControllerService.getUserById(id));
    }
}
8. 测试
测试时，为了体现效果，可以将redisLockHelper.unlock(lockKey, value);这一行代码注释掉

Redis缓存生成：
五秒内再次请求：

其实除了将上述代码注释掉测试，更合理的测试方法可按如下步骤：

在上述ControllerService方法中打一个只阻塞当前线程的断点，用于阻塞调第一个请求(第一个请求已经获取锁)
使用postman开两个窗口，第一个窗口请求一次，进入断点
postman第二个窗口立刻发第二个请求，这时候由于锁已被前一个请占有，会报500，重复提交异常
继续执行第一个窗口的请求，第一个窗口的请求返回结果
注意，上述操作需要在一个过期时间周期内完成，否则第一个窗口的请求会由于缓存已过期，释放锁时报错

示例代码：码云 – 卓立 – 分布式服务防重复提交
————————————————
版权声明：本文为CSDN博主「卓立0」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/weixin_41835612/article/details/83738244