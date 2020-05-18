# KAFKA学习

## gradle

```json
compile group: 'org.springframework.kafka', name: 'spring-kafka', version: "${springkafka}"
springkafka="2.3.4.RELEASE"
```
## java code
### config

```json
spring: 
	kafka:
	    bootstrap-servers: 127.0.0.1:9092
	    producer:
	      # 发生错误后，消息重发的次数。
	      retries: 0
	      #当有多个消息需要被发送到同一个分区时，生产者会把它们放在同一个批次里。该参数指定了一个批次可以使用的内存大小，按照字节数计算。
	      batch-size: 16384
	      # 设置生产者内存缓冲区的大小。
	      buffer-memory: 33554432
	      # 键的序列化方式
	      key-serializer: org.apache.kafka.common.serialization.StringSerializer
	      # 值的序列化方式
	      value-serializer: org.apache.kafka.common.serialization.StringSerializer
	      # acks=0 ： 生产者在成功写入消息之前不会等待任何来自服务器的响应。
	      # acks=1 ： 只要集群的首领节点收到消息，生产者就会收到一个来自服务器成功响应。
	      # acks=all ：只有当所有参与复制的节点全部收到消息时，生产者才会收到一个来自服务器的成功响应。
	      acks: 1
	    consumer:
	      # 自动提交的时间间隔 在spring boot 2.X 版本中这里采用的是值的类型为Duration 需要符合特定的格式，如1S,1M,2H,5D
	      auto-commit-interval: 1S
	      # 该属性指定了消费者在读取一个没有偏移量的分区或者偏移量无效的情况下该作何处理：
	      # latest（默认值）在偏移量无效的情况下，消费者将从最新的记录开始读取数据（在消费者启动之后生成的记录）
	      # earliest ：在偏移量无效的情况下，消费者将从起始位置读取分区的记录
	      auto-offset-reset: latest
	      # 是否自动提交偏移量，默认值是true,为了避免出现重复数据和数据丢失，可以把它设置为false,然后手动提交偏移量
	      enable-auto-commit: false
	      # 键的反序列化方式
	      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
	      # 值的反序列化方式
	      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
	
	    listener:
	      # 在侦听器容器中运行的线程数。
	      concurrency: 5
	      #listner负责ack，每调用一次，就立即commit
	      ack-mode: manual_immediate
	      missing-topics-fatal: false
```
```json
/**
 * kafka配置类
 * @datetime 2020/4/27
 * @author shawn
 **/
@Configuration
public class KafkaConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaTemplate<Object, Object> template) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition>
                consumerRecordExceptionTopicPartitionBiFunction = (cr, e) -> new TopicPartition(cr.topic() + "_dlt", cr.partition());
        //最大重试三次
        factory.setErrorHandler(new SeekToCurrentErrorHandler(new DeadLetterPublishingRecoverer(template,consumerRecordExceptionTopicPartitionBiFunction), 3));
        return factory;
    }
}

```
### produce

```json

@Service
public class CallBackEventServiceImpl implements ICallBackEventService {

    private static final Log LOG = LogFactory.getLog(CallBackEventServiceImpl.class);



    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private MessageService messageService;
    @Autowired
    private INumberService iNumberService;
    @Autowired
    IdGenerator idGenerator;
    /** 错误信息长度 */
    private final Integer MESSAGE_LENGTH=450;
    /** 存储在失败消息补偿db库中队列的名称 send_${topicname}*/
    private final String DB_MESSAGE_NAME="send_{0}";
    @Value("${spring.kafka.topic.partition-num}")
    private Integer partitionNum;


    @Override
    public void event(String traceId, Event event,String topic){
        final String topicName = topic;
        final String traceIdStr=MessageFormat.format(topic+"_{0}",traceId);
        /** json字符串 */
        String jsonStr = JSON.toJSONString(event);
        将java的object对象转成json字符串
        	  然后根据object中某个唯一业务[计划]id进行分片数量取模，这样子可以让同个id
        	  落在同一个partition中，这样子在消费者中消费就会
        BigDecimal remainder = iNumberService.getRemainder(Long.valueOf(event.getPlanId()),partitionNum);
        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName,remainder.intValue(),String.valueOf(idGenerator.getId()),jsonString);
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onFailure(Throwable throwable) {
                //发送失败的处理
                一般将消息记录到db 消息补偿表里
                然后做日志记录
                
            }
            @Override
            public void onSuccess(SendResult<String, Object> stringObjectSendResult) {
                //成功的处理
                LOG.info(MessageFormat.format("{0}请求的traceId:{1},请求的数据:{2},请求的topic{3},发送消息成功:{4}",topicName,traceId,jsonStr,topicName,stringObjectSendResult.toString()));
            }
        });
    }

}

```

### consumer

```json

@Component
public class ShowEventConsumer {

    private static final Log LOG = LogFactory.getLog(ShowEventConsumer.class);

    @KafkaListener(topics = "${spring.kafka.topic.show}", groupId = "${spring.kafka.consumer.group.show}")
    public void topicShow(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws Exception {
        String kafkaKey = (String) record.key();
        LOG.info(MessageFormat.format("kafka key:{0} TOPIC:{1}, topic_show 开始消费消息... ...", kafkaKey, messageConstant.getTopicShow()));
        Optional message = Optional.ofNullable(record.value());
        if (!message.isPresent()) {
            LOG.info(MessageFormat.format("kafka key:{0} TOPIC:{1}, topic_show kafka中 消息为null,结束本次消息消费", kafkaKey, messageConstant.getTopicShow()));
            return;
        }

        Object msg = message.get();
        LOG.info(MessageFormat.format("kafka key:{0} TOPIC:{1}, topic_show 消费消息:{2}", kafkaKey, messageConstant.getTopicShow(), msg));
        Message messageObject;
        try {
            messageObject = JSON.parseObject((String) msg, Message.class);
        } catch (Exception e) {
            LOG.error(MessageFormat.format("kafka key:{0} TOPIC:{1} JSON反序列化失败:{2}", kafkaKey, messageConstant.getTopicShow(), AdxLogs.getExceptionMsg(e)));
            ack.acknowledge();
            return;
        }

        ack.acknowledge();

    }

}

```

## 结果
这里主要是配置了kafka的死信队列功能，其中配置死信队列topic_name为 原topicName_dlt，同时设置了是原消费队列消费失败三次，才会将消息丢到死信队列中。
## 坑中坑
在死信队列的消费者中，如果遇到异常，不要再将异常抛出，用日志记录或其他手段处理，因为配置了死信队列的功能，会导致死信队列的消息被丢入到死信队列的死信队列，即原topic_name_dlt_dlt中，然后再次消费再抛，导致队列的无限增加。

## 建议

搭建kafka集群自己测试一下，中小公司直接用阿里云的kafka服务就好。


