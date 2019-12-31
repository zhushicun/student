# [Spring Security (CORS)跨域资源访问配置](https://www.cnblogs.com/famary/p/10336223.html)

1、CORS介绍

CORS是一个W3C标准，全称是"跨域资源共享"（Cross-origin resource sharing）。它允许浏览器向跨源(协议 + 域名 + 端口)服务器，发出XMLHttpRequest请求，从而克服了AJAX只能同源使用的限制。

CORS需要浏览器和服务器同时支持。它的通信过程，都是浏览器自动完成，不需要用户参与。对于开发者来说，CORS通信与同源的AJAX通信没有差别，代码完全一样。浏览器一旦发现AJAX请求跨源，就会自动添加一些附加的头信息，有时还会多出一次附加的请求，但用户不会有感觉。
因此，实现CORS通信的关键是服务器。只要服务器实现了对CORS的支持，就可以跨源通信。

2、基于Spring Boot + Spring Security的配置代码

1> 开启Spring Security cors支持

```
 @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 允许跨域访问
        http.cors();
    }
```

2、在Spring容器中添加以个CorsConfigurationSource实例

![img](https://img2018.cnblogs.com/blog/1187323/201901/1187323-20190129223208353-1782261631.jpg)

SpringSecurity会自动寻找name=corsConfigurationSource的Bean

![img](https://img2018.cnblogs.com/blog/1187323/201901/1187323-20190129223122199-1091236313.jpg)

 3、配置文件中关于CORS的属性

![img](https://img2018.cnblogs.com/blog/1187323/201901/1187323-20190129223541232-615284276.jpg)