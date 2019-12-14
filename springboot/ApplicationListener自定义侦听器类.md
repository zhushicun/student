```
ApplicationListener自定义侦听器类
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
@Component
public class InstantiationTracingBeanPostProcessor implements
        ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOG = Logger.getLogger(InstantiationTracingBeanPostProcessor.class);
    private static boolean initialized;
    
    @Autowired
    private ManageResolver manageResolver;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            //只在初始化“根上下文”的时候执行
            final ApplicationContext app = event.getApplicationContext();
            if (null == app.getParent()
                    && ("Root WebApplicationContext".equals(app.getDisplayName())
                            || app.getDisplayName().contains("AnnotationConfigEmbeddedWebApplicationContext"))
                    && "/xweb".equals(app.getApplicationName())
                    ) { // 当存在父子容器时，此判断很有用
                LOG.info("*************:" + event.getSource());
                LOG.info("*************:" + app.getDisplayName());
                LOG.info("*************:" + app.getApplicationName());
                LOG.info("*************:" + app.getBeanDefinitionCount());
                LOG.info("*************:" + app.getEnvironment());
                LOG.info("*************:" + app.getParent());
                LOG.info("*************:" + app.getParentBeanFactory());
                LOG.info("*************:" + app.getId());
                LOG.info("*************:" + app.toString());
                LOG.info("*************:" + app);
                if(!initialized && !manageResolver.IsInitialCompleted()) {
                    manageResolver.initLater();
                    initialized = true;
                }
            }
        } catch (Exception e) {
            LOG.error("((XmlWebApplicationContext) event.getSource()).getDisplayName() 执行失败，请检查Spring版本是否支持");
        }
    }

}
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

SpringBoot应用程序启动类

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
@SpringBootApplication
@ImportResource({"classpath:config/applicationContext-xweb-dubbo.xml","classpath:config/applicationContext-xweb.xml"})
@Configuration
@ComponentScan
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class, RedisAutoConfiguration.class})
public class XwebApplication extends WebMvcConfigurerAdapter {

    public static void main(String[] args) {
        SpringApplication springApplication =new SpringApplication(XwebApplication.class);
        springApplication.addListeners(new InstantiationTracingBeanPostProcessor());
        springApplication.run(args);
    }  
    
    /**
     * 上传附件容量限制
     * @return
     */
    @Bean  
    public MultipartConfigElement multipartConfigElement() {  
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("102400KB");  
        factory.setMaxRequestSize("112400KB");  
        return factory.createMultipartConfig();  
    } 
    
    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 多个拦截器组成一个拦截器链
        // addPathPatterns 用于添加拦截规则
        // excludePathPatterns 用户排除拦截
        registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/**");
        super.addInterceptors(registry);
    }
    
}  
```