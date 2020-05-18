# sharding jdbc 学习
## 目标功能
实现对数据每天入库分表插入操作，每天插入的表都是新表，并且能够聚合查询出结果来。
## 配置
### gradle
```json
compile group: 'io.shardingjdbc', name: 'sharding-jdbc-core', version: "${shardingJdbcCore}"
version: 2.0.3
```
###config
```json
/**
* 数据源以及sharding-jdbc 分表配置
* @datetime 2020/3/11
* @author shawn
**/
@Configuration
@MapperScan(basePackages = "com.kc.dao.mappers.opendsp",sqlSessionTemplateRef = "opendspSqlSessionTemplate")
public class ShardingConfig {
 
    @Value("${spring.datasource.opendsp.druid.jdbc-url}")
    private String druidUrl;

    @Value("${spring.datasource.opendsp.druid.driver-class-name}")
    private String druidDriverClassName;
 
    @Value("${spring.datasource.opendsp.druid.username}")
    private String druidUsername;
 
    @Value("${spring.datasource.opendsp.druid.password}")
    private String druidPassword;

 
    /**
     * shardingjdbc数据源
     *
     * @return
     */
    @Bean(name = "shardingDataSource")
    public DataSource dataSource() throws SQLException {
        // 封装dataSource
        Map<String, DataSource> dataSourceMap = new HashMap<>(16,0.75F);
        DruidDataSource dataSource = createDb();
        dataSourceMap.put("ds", dataSource);
 
        TableRuleConfiguration clickEventTableRuleConfiguration= getClickEventTableRuleConfiguration();
        TableRuleConfiguration showEventTableRuleConfiguration = getShowEventTableRuleConfiguration();

        // 设置分库分表规则
        //TableRuleConfiguration tableRuleConf = getUserTableRuleConfiguration();
        // 将规则写入ShardingDataSource
        ShardingRuleConfiguration shardingRuleConf = new ShardingRuleConfiguration();
        shardingRuleConf.getTableRuleConfigs().add(clickEventTableRuleConfiguration);
        shardingRuleConf.getTableRuleConfigs().add(showEventTableRuleConfiguration);
        Properties p = new Properties();
        p.setProperty("sql.show", Boolean.TRUE.toString());
        // 获取数据源对象
        try {
            return ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConf, new ConcurrentHashMap(16,0.75F), p);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据时间规则设置分表
     *
     * @return
     */
    @Bean(name = "openDspClickEventTableRuleConfiguration")
    public TableRuleConfiguration getClickEventTableRuleConfiguration() {
        TableRuleConfiguration tableRuleConfiguration = new TableRuleConfiguration();
        // 设置逻辑表
        tableRuleConfiguration.setLogicTable("click_event");
        // 所以注入时要获取当前日期作为添加逻辑表后缀，以便添加时找到当前月的表进行插入操作
        tableRuleConfiguration.setTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("add_time",ShardingAlgorithm.class.getTypeName()));
        return tableRuleConfiguration;
    }

    /**
     * 根据时间规则设置分表
     *
     * @return
     */
    @Bean(name = "openDspShowEventTableRuleConfiguration")
    public TableRuleConfiguration getShowEventTableRuleConfiguration() {
        TableRuleConfiguration tableRuleConfiguration = new TableRuleConfiguration();
        // 设置逻辑表
        tableRuleConfiguration.setLogicTable("show_event");
        // 所以注入时要获取当前日期作为添加逻辑表后缀，以便添加时找到当前月的表进行插入操作
        tableRuleConfiguration.setTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("add_time",ShardingAlgorithm.class.getTypeName()));
        return tableRuleConfiguration;
    }
 
    /**
     * 注入第一个数据源
     *
     * @return
     */
    private DruidDataSource createDb() {
        // 配置第一个数据源
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(druidDriverClassName);
        dataSource.setUrl(druidUrl);
        dataSource.setUsername(druidUsername);
        dataSource.setPassword(druidPassword);
        dataSource.setProxyFilters(Lists.newArrayList(statFilter()));
        // 每个分区最大的连接数
        dataSource.setMaxActive(20);
        // 每个分区最小的连接数
        dataSource.setMinIdle(5);
        return dataSource;
    }
    
 
    @Bean
    public Filter statFilter() {
        StatFilter filter = new StatFilter();
        filter.setSlowSqlMillis(5000);
        filter.setLogSlowSql(true);
        filter.setMergeSql(true);
        return filter;
    }
 
//    @Bean
//    public ServletRegistrationBean statViewServlet() {
//        //创建servlet注册实体
//        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new StatViewServlet(), "/druid/*");
//        //设置ip白名单
//        servletRegistrationBean.addInitParameter("allow", "127.0.0.1");
//        //设置ip黑名单，如果allow与deny共同存在时,deny优先于allow
//        servletRegistrationBean.addInitParameter("deny", "192.168.0.19");
//        // 设置控制台管理用户
//        servletRegistrationBean.addInitParameter("loginUsername", "admin");
//        servletRegistrationBean.addInitParameter("loginPassword", "123456");
//        // 是否可以重置数据
//        servletRegistrationBean.addInitParameter("resetEnable", "false");
//        return servletRegistrationBean;
//    }

    @Bean(name = "openDspSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("shardingDataSource") DataSource shardingDataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(shardingDataSource);
//        sqlSessionFactoryBean.setTypeAliasesPackage("com.kc.dao.entity.opendsp");

        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

        sqlSessionFactoryBean.setMapperLocations(resourcePatternResolver.getResources("classpath:mapper/opendsp/*.xml"));
        return sqlSessionFactoryBean.getObject();

    }
    @Bean(name = "openDspTransactionManager")
    public DataSourceTransactionManager transactitonManager(@Qualifier("shardingDataSource") DataSource shardingDataSource) {
        return new DataSourceTransactionManager(shardingDataSource);
    }



    @Bean(name = "opendspSqlSessionTemplate")
    public SqlSessionTemplate testSqlSessionTemplate(@Qualifier("openDspSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
 
 
}
```
```json
/**
* 分表规则
* @datetime 2020/3/11
* @author shawn
**/
@Slf4j
public class ShardingAlgorithm implements PreciseShardingAlgorithm<Date> {

    /** 
     * sql 中 = 操作时，table的映射 
　　　*　　根据传进来的日期命名表名称
     */  
    @Override
    public String doSharding(Collection<String> tableNames, PreciseShardingValue<Date> shardingValue) {
        SimpleDateFormat simpleFormatter = new SimpleDateFormat("yyyyMMdd");
        String tableName = shardingValue.getLogicTableName();
        String key=simpleFormatter.format(shardingValue.getValue());
        return tableName.concat("_").concat(key);
    }


    
}
```
###多数据源配置
```json
package com.kc.api.config.muldatasource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = {"com.kc.dao.mappers.odapi"},sqlSessionTemplateRef = "adApiSqlSessionTemplate")
public class OdApiConfig {

    @Bean(name = "odApiDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.odapi.druid")
    @Primary
    public DataSource odApiDataSource(){
      return   DataSourceBuilder.create().build();
    }

    @Bean(name = "odApiSqlSessionFactory")
    @Primary
    public SqlSessionFactory odApiSqlSessionFactory(@Qualifier("odApiDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean=new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/odapi/*.xml"));
        return sqlSessionFactoryBean.getObject();
    }

    @Bean(name = "odApiTransactionManager")
    @Primary
    public DataSourceTransactionManager odApiTransactionManager(@Qualifier("odApiDataSource") DataSource dataSource){
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "adApiSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate odApiSqlSessionTemplate(@Qualifier("odApiSqlSessionFactory") SqlSessionFactory sqlSessionFactory){
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}

```

聚合查询结果还没有实现，具体操作看sharding-jdbc官方文档