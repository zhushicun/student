# JAVA8 语法一 匿名内部类单方法返回
## 概述
在之前使用方法匿名内部类单对象返回。

```
public interface DbCallBack<T> {
    T findOne();
}
```
```
@Service
public class AaService {

    @Resource(name="*****")
    private ICache cache;

    public <T> T getCache(String key,TypeReference<T> clazz, DbCallBack<T> dbCallBack) {
        //查询缓存
        String strCache = cache.getStrCache(key);
        //缓存中没有获取到则从数据中查询
            //从redis 查 如果没有 下面 查一次数据库
        if (strCache == null) {
            T t = dbCallBack.findOne();
            //存进缓存
            cache.addStrKeyCache(key,JSON.toJSONString(t),1L, TimeUnit.DAYS);
            return t;
        } else {
            return JSON.parseObject(strCache,clazz);
        }
    }
}
```

```
Aa aa =
                aaService.getCache(key, new TypeReference<Aa>() {
                }, () -> aaMapper.selectByKey(key));
```
## 后续版本
不需要单一返回的接口类
直接使用java8的Supplier类

```
@Service
public class AaService {

    @Resource(name="*****")
    private ICache cache;

    public <T> T getCache(String key,TypeReference<T> clazz, Supplier<T> dbCallBack) {
        //查询缓存
        String strCache = cache.getStrCache(key);
        //缓存中没有获取到则从数据中查询
            //从redis 查 如果没有 下面 查一次数据库
        if (strCache == null) {
            T t = dbCallBack.get();
            //存进缓存
            cache.addStrKeyCache(key,JSON.toJSONString(t),1L, TimeUnit.DAYS);
            return t;
        } else {
            return JSON.parseObject(strCache,clazz);
        }
    }
}
```