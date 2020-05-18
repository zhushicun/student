# redis配置多数据源录入不同database

## 业务需求
在一个业务中，一般会存放不同的缓存数据，还有redis分布式锁，请求唯一性记录等各种key，这就需要将不同的数据放到不同的database中进行存放。
## gradle

```json
compile group: 'org.springframework.boot', name: 'spring-boot-starter-data-redis', version: "${springBootStarterDataRedis}"
springBootStarterDataRedis="2.2.4.RELEASE"

compile group: 'redis.clients', name: 'jedis', version: '2.9.0'
```
## java code

```json
package com.kc.utils.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 完成对Redis的整合的一些配置
 *
 *
 */
@SuppressWarnings("deprecation")
@Configuration
public class RedisConfig {

	@Autowired
	RedisProperties redisProperties;

	/**
	 * 1.创建JedisPoolConfig对象。在该对象中完成一些链接池配置
	 * @ConfigurationProperties:会将前缀相同的内容创建一个实体。
	 */
	@Bean
	public JedisPoolConfig jedisPoolConfig(){
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxIdle(redisProperties.getMaxIdle());
		config.setMinIdle(redisProperties.getMinIdle());
		config.setMaxTotal(redisProperties.getMaxTotal());
		return config;
	}
	
	/**
	 * 2.创建JedisConnectionFactory：配置redis链接信息
	 */
	@Bean(name = "redisdatabase1")
	@Primary
	public JedisConnectionFactory jedisConnectionFactoryOne(){
		JedisConnectionFactory factory = new JedisConnectionFactory();
		factory.setDatabase(redisProperties.getDataBaseOne());
		String password=redisProperties.getPassWordOne();
		if(StringUtils.isNotBlank(password)){
			factory.setPassword(password);
		}
		factory.setHostName(redisProperties.getHostNameOne());
		factory.setPort(redisProperties.getPortOne());
		return factory;
	}

	/**
	 * 3.创建JedisConnectionFactory：配置redis链接信息
	 */
	@Bean(name = "redisdatabase2")
	public JedisConnectionFactory jedisConnectionFactoryTwo(){
		JedisConnectionFactory factory = new JedisConnectionFactory();
		factory.setDatabase(redisProperties.getDataBaseTwo());
		String password=redisProperties.getPassWordTwo();
		if(StringUtils.isNotBlank(password)){
			factory.setPassword(password);
		}
		factory.setHostName(redisProperties.getHostNameTwo());
		factory.setPort(redisProperties.getPortTwo());
		return factory;
	}

	/**
	 * 3.创建JedisConnectionFactory：配置redis链接信息
	 */
	@Bean(name = "redisdatabase3")
	public JedisConnectionFactory jedisConnectionFactoryThree(){
		JedisConnectionFactory factory = new JedisConnectionFactory();
		factory.setDatabase(redisProperties.getDataBaseThree());
		String password=redisProperties.getPassWordThree();
		if(StringUtils.isNotBlank(password)){
			factory.setPassword(password);
		}
		factory.setHostName(redisProperties.getHostNameThree());
		factory.setPort(redisProperties.getPortThree());
		return factory;
	}

	
	/**
	 * 3.创建RedisTemplate:用于执行Redis操作的方法
	 * (最重要的方法,之前的方法全是为了获得最后的RedisTemplates对象)
	 */
	@Bean(name="redisTemplateOne")
	@Primary
	public RedisTemplate<String,Object> redisTemplateOne(@Qualifier("redisdatabase1")JedisConnectionFactory factory){
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		//关联
		template.setConnectionFactory(factory);
		
		//为key设置序列化器
		template.setKeySerializer(new StringRedisSerializer());
		//为value设置序列化器
		template.setValueSerializer(new StringRedisSerializer());
		
		return template;
	}


	@Bean(name="redisTemplateTwo")
	public RedisTemplate<String,Object> redisTemplateTwo(@Qualifier("redisdatabase2")JedisConnectionFactory factory){
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		//关联
		template.setConnectionFactory(factory);

		//为key设置序列化器
		template.setKeySerializer(new StringRedisSerializer());
		//为value设置序列化器
		template.setValueSerializer(new StringRedisSerializer());
		return template;
	}

	@Bean(name="redisTemplateThree")
	public RedisTemplate<String,Object> redisTemplateThree(@Qualifier("redisdatabase3")JedisConnectionFactory factory){
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		//关联
		template.setConnectionFactory(factory);
		//为key设置序列化器
		template.setKeySerializer(new StringRedisSerializer());
		//为value设置序列化器
		template.setValueSerializer(new StringRedisSerializer());
		return template;
	}
}


```

```json
package com.kc.utils.cache;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ICache {
    Boolean isExistKey(String key);
    /**
     *
     * <Description> 根据KEY进行清除缓存<br>
     * @param key
     * <br>
     */
    void delCacheByKey(String key);
    /**
     *
     * <Description> 添加缓存字符串<br>
     * @param key
     * @param data
     * @param minus
     * @return <br>
     */
    void addStrCache(String key, String data, Long minus);
    /**
     *
     * <Description> 获取缓存字符串<br>
     * @param key
     * @return String<br>
     */
    String getStrCache(String key);
    /**
     *
     * <Description> 添加对象<br>
     * @param key
     * @param data
     * @param minus
     * @return <br>
     */
    void addCacheSimple(String key, Object data, Long minus);
    /**
     *
     * <Description> 获取对象<br>
     * @param key
     * @return <br>
     */
    Object getCacheSimple(String key);
    /**
     *
     * <Description> 添加List对象<br>
     * @param key
     * @param list
     * @param minus
     * @return <br>
     */
    void addCacheList(String key, List<?> list, Long minus);
    /**
     *
     * <Description> 获取List对象<br>
     * @param key

     * @return  List<Object><br>
     */
    List<Object> getCacheList(String key);
    /**
     *
     * <Description> 缓存SET数据<br>
     *
     * @author

     * @param key
     * @param datas
     * @param minus
     * <br>
     */
    void addCacheSet(String key, Set<?> datas, long minus);
    /**
     *
     * <Description> 获取 SET数据<br>
     *
     * @author
     * @param key
     * <br>
     */
    Set<Object> getCacheSet(String key);
    /**
     *
     * <Description> 缓存map数据<br>
     *
     * @author

     * @param key
     * @param datas
     * @param minus
     * <br>
     */
    void addCacheMap(String key, Map<Object, Object> datas, long minus);
    /**
     *
     * <Description> 获取map数据<br>
     *
     * @author
     * @param key

     * <br>
     */
    Map<Object,Object> getCacheMap(String key);

    /**
    * 添加String数据
    * @author shawn
    * @param [key, data, secondes]
    * @return void
    * @date 2020/3/20
    **/
    void addStrKeyCache(String key, String data, Long times, TimeUnit timeUnit);
}

```

```json
package com.kc.utils.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component("redisService1")
@SuppressWarnings("unchecked")
public class RedisServiceImpl implements ICache {

    private RedisTemplate redisTemplate;

    @Autowired
    public void setRedisTemplate(@Qualifier("redisTemplateOne") RedisTemplate redisTemplate) {
        RedisSerializer stringSerializer = new StringRedisSerializer();
        if(redisTemplate!=null) {
            redisTemplate.setKeySerializer(stringSerializer);
            redisTemplate.setHashKeySerializer(stringSerializer);

            redisTemplate.setHashKeySerializer(stringSerializer);
            redisTemplate.setValueSerializer(stringSerializer);






            redisTemplate.afterPropertiesSet();


        }
        this.redisTemplate = redisTemplate;
    }
    @Override
    public Boolean isExistKey(String key){
        return redisTemplate.hasKey(key);
    }
    @Override
    public void delCacheByKey(String key){
        if (null != key) {
            redisTemplate.delete(key);
        }
    }
    @Override
    public void addStrCache(String key,String data,Long minus){
        redisTemplate.opsForValue().set(key,data);
        if(minus>0){
            redisTemplate.expire(key,minus, TimeUnit.MINUTES);
        }
    }
    @Override
    public  String getStrCache(String key){

        return (String)redisTemplate.opsForValue().get(key);
    }
    @Override
    public void addCacheSimple(String key,Object data,Long minus){
        if(key!=null && data!=null){
            redisTemplate.opsForValue().set(key,data);
            if(minus>0){
                redisTemplate.expire(key,minus,TimeUnit.MINUTES);
            }
        }
    }
    @Override
    public Object getCacheSimple(String key){
        return redisTemplate.opsForValue().get(key);
    }
    @Override
    public void addCacheList(String key, List<?> list, Long minus){
        ListOperations<String,Object> opsList;
        if(null!=redisTemplate){
            opsList=redisTemplate.opsForList();
            if(null!=opsList){
                int size=list.size();
                for(int i=0;i<size;i++)
                {
                    opsList.leftPush(key,list.get(i));
                }
                if (minus>0){
                    redisTemplate.expire(key,minus,TimeUnit.MINUTES);
                }
            }
        }
    }
    @Override
    public List<Object> getCacheList(String key){
        List<Object> dataList= new ArrayList<>();
        ListOperations<String,Object> opsList=null;
        if(null!=redisTemplate){
            opsList=redisTemplate.opsForList();
            if(null!=opsList){
                Long size=opsList.size(key);
                for(int i=0;i<size;i++){
                    dataList.add(opsList.index(key,i));
                }
            }
        }
        return  dataList;
    }
    @Override
    public void addCacheSet(String key, Set<?> datas, long minus){
        SetOperations<String,Object> opsSet;
        if(null!=redisTemplate){
            opsSet=redisTemplate.opsForSet();
            if(null!=opsSet){
                opsSet.add(key,datas);
                if(minus>0){
                    redisTemplate.expire(key,minus,TimeUnit.MINUTES);
                }
            }
        }
    }
    @Override
    public Set<Object> getCacheSet(String key){
        Set<Object> dataSet=new HashSet<>();
        SetOperations<String,Object> opsSet;
        if(null!=redisTemplate){
            opsSet=redisTemplate.opsForSet();
            if(null!=opsSet){
                dataSet=opsSet.members(key);
            }
        }
        return  dataSet;
    }
    @Override
    public  void addCacheMap(String key, Map<Object,Object> datas, long minus){

        HashOperations<String,Object,Object> hashOperations;
        if(null!=redisTemplate){
            hashOperations=redisTemplate.opsForHash();
            if(null!=hashOperations){
                hashOperations.putAll(key,datas);
            }
            if(minus>0){
                redisTemplate.expire(key,minus,TimeUnit.MINUTES);
            }
        }
    }
    @Override
    public Map<Object,Object> getCacheMap(String key){
        Map<Object,Object> objectMap=new HashMap<>();
        HashOperations<String,Object,Object> hashOperations;
        if(null!=redisTemplate){
            hashOperations=redisTemplate.opsForHash();
            if(null!=hashOperations){
                objectMap=hashOperations.entries(key);
            }
        }
        return  objectMap;
    }

    @Override
    public void addStrKeyCache(String key,String data,Long times,TimeUnit timeUnit){
        redisTemplate.opsForValue().set(key,data);
        if(times>0){
            redisTemplate.expire(key,times, timeUnit);
        }
    }

}

```
