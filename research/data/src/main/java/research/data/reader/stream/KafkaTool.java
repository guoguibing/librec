package research.data.reader.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka数据读写工具
 * 
 * @author liweigu
 *
 */
public class KafkaTool {
	private static Logger LOG = LoggerFactory.getLogger(KafkaTool.class);
	// 超时时间，单位是ms
	private static final int POLL_TIMEOUT = 100;

	/**
	 * 提交数据
	 * 
	 * @param server 服务地址
	 * @param topic 主题
	 * @param data 数据内容
	 */
	public static void produce(String server, String topic, HashMap<String, String> data) {
		LOG.debug("produce start");
		Properties props = new Properties();
		// 至少设置1个地址，kafka会自动识别集群里的其他地址。
		props.put("bootstrap.servers", server);
		// acks的值：
		// 0: 不需要进行确认，速度最快。存在丢失数据的风险。
		// 1: 仅需要Leader进行确认，不需要ISR进行确认。是一种效率和安全折中的方式。
		// all: 需要ISR中所有的Replica给予接收确认，速度最慢，安全性最高，但是由于ISR可能会缩小到仅包
		props.put("acks", "1");
		// props.put("retries", 0);
		// props.put("batch.size", 16384);
		// Producer默认会把两次发送时间间隔内收集到的所有Requests进行一次聚合然后再发送，以此提高吞吐量，而linger.ms则更进一步，这个参数为每次发送增加一些delay，以此来聚合更多的Message。每批数据会间隔linger.ms合并发送。
		// props.put("linger.ms", 1);
		props.put("linger.ms", 10);
		// props.put("compression.type", "none");
		// This setting should correspond roughly to the total memory the producer will use
		// props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		Producer<String, String> producer = new KafkaProducer<String, String>(props);
		for (String key : data.keySet()) {
			Future<RecordMetadata> future = producer.send(new ProducerRecord<String, String>(topic, key, data.get(key)));
		}

		producer.close();
	}

	/**
	 * 消费数据
	 * 
	 * @param server 服务地址
	 * @param topic 主题
	 * @param consumerRecordHandler 数据处理工具
	 * @param groupId 消费组分组id。如果传null，那么默认值是"test"。同一个分组id，只会取到一个结果，不会重复；不同分组id，并行取结果，相互不影响。
	 * @param autoCommit 是否自动提交
	 * @param beginOffset 非自动提交时，是否设置起始索引
	 */
	public static void consume(String server, String topic, ConsumerRecordHandler consumerRecordHandler, String groupId, boolean autoCommit, Long beginOffset) {
		LOG.debug("consume start. server = " + server + ", topic = " + topic + ", autoCommit = " + autoCommit + ", beginOffset = " + beginOffset);
		Properties props = new Properties();
		// 至少设置1个地址，kafka会自动识别集群里的其他地址。
		props.put("bootstrap.servers", server);
		if (groupId == null) {
			groupId = "test";
		}
		props.put("group.id", groupId);
		props.put("enable.auto.commit", "" + autoCommit);
		// 默认值是5000
		// props.put("auto.commit.interval.ms", "1000");
		// 启动Consumer的个数，适当增加可以提高并发度。
		props.put("num.consumer.fetchers", 4);
		// 需要提升性能时设置false
		// props.put("check.crcs", false);
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
		if (beginOffset != null) {
			// 从头读取
			TopicPartition partition0 = new TopicPartition(topic, 0);
			consumer.assign(Arrays.asList(partition0));
			// consumer.seekToBeginning(consumer.assignment());
			long offset = 0;
			if (beginOffset < 0) {
				Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
				LOG.debug("endOffsets.size():" + endOffsets.size());
				for (TopicPartition partition : endOffsets.keySet()) {
					Long endOffset = endOffsets.get(partition);
					LOG.debug("endOffset=" + endOffset);
					offset = beginOffset + endOffset;
					if (offset < 0) {
						offset = 0L;
					}
					LOG.debug("offset=" + offset);
					// consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(offset + 1)));
				}
			}
			consumer.seek((TopicPartition) consumer.assignment().toArray()[0], offset);
		} else {
			// 订阅
			consumer.subscribe(Arrays.asList(topic));
			// consumer.subscribe(Arrays.asList(topic, "test"));
		}
		if (autoCommit) {
			// 自动管理索引
			while (true) {
				// LOGGER.debug("poll");
				try {
					ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
					List<ConsumerRecord<String, String>> buffer = new ArrayList<ConsumerRecord<String, String>>();
					for (ConsumerRecord<String, String> record : records) {
						buffer.add(record);
					}
					// LOGGER.debug("after poll");
					consumerRecordHandler.handle(buffer);
				} catch (Exception ex) {
					ex.printStackTrace();
					String expMessage = ex.getMessage();
					try {
						ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
						ex.printStackTrace(new java.io.PrintWriter(buf, true));
						expMessage = buf.toString();
						buf.close();
					} catch (IOException ex1) {
						LOG.warn("解析ex发生异常。", ex1);
					}
					LOG.warn("KafkaTool.consume发生异常。msg=" + expMessage);
				}
			}
		} else {
			// 手动管理索引
			long minBatchSize = 1000;
			long maxTime = 1000;
			// 批量取数据
			List<ConsumerRecord<String, String>> buffer = new ArrayList<ConsumerRecord<String, String>>();
			long time = System.currentTimeMillis();
			while (true) {
				// LOGGER.debug("poll");
				ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
				for (ConsumerRecord<String, String> record : records) {
					buffer.add(record);
				}
				if (buffer.size() >= minBatchSize || (System.currentTimeMillis() - time >= maxTime)) {
					boolean needMoreData = consumerRecordHandler.handle(buffer);
					if (!needMoreData) {
						break;
					}
					// // 执行后才会更新offset
					// consumer.commitSync();
					// 恢复数据
					buffer.clear();
					time = System.currentTimeMillis();
				}
			}
		}
		LOG.debug("consume end");
	}

	/**
	 * 处理数据流
	 * 
	 * @param server 服务地址
	 * @param topic 主题
	 * @param newTopic 新主题
	 * @param valueMapper 值映射
	 */
	public static void stream(String server, String topic, String newTopic, ValueMapper<Object, Object> valueMapper) {
		LOG.debug("stream start");
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-stream-processing-application");
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, server);
		// props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
		// props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		// props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		StreamsConfig config = new StreamsConfig(props);

		StreamsBuilder builder = new StreamsBuilder();
		// KStream<String, String> source = builder.stream("streams-plaintext-input");

		builder.stream(topic).mapValues(valueMapper).to(newTopic);

		KafkaStreams streams = new KafkaStreams(builder.build(), config);
		streams.start();
	}

}
