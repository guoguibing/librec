package research.data.reader.stream;

import java.util.HashMap;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.junit.Test;

/**
 * KafkaTool单元测试
 * 
 * @author liweigu714@163.com
 *
 */
public class KafkaToolTests {
	/**
	 * 测试数据读写
	 */
	@Test
	public void readWriteData() {
		// server为空时不运行测试
		String server = ""; // localhost:9092
		if (server != null && server.length() > 0) {
			String topic = "ditu";
			String method = "produce"; // consume, consume2, consume3, consume4, stream
			ConsumerRecordHandler consumerRecordHandler = new ConsumerRecordHandler() {
				@Override
				public boolean handle(List<ConsumerRecord<String, String>> records) {
					for (ConsumerRecord<String, String> record : records) {
						System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
					}
					return true;
				}
			};
			if (method.equals("produce")) {
				HashMap<String, String> data = new HashMap<String, String>();
				for (int i = 0; i < 6; i++) {
					data.put("k" + i, "v" + i);
				}
				KafkaTool.produce(server, topic, data);
			} else if (method.equals("consume")) {
				KafkaTool.consume(server, topic, consumerRecordHandler, null, true, null);
			} else if (method.equals("consume2")) {
				KafkaTool.consume(server, topic, consumerRecordHandler, null, false, null);
			} else if (method.equals("consume3")) {
				KafkaTool.consume(server, topic, consumerRecordHandler, null, false, 0L);
			} else if (method.equals("consume4")) {
				String newTopic = topic + "New";
				KafkaTool.consume(server, newTopic, consumerRecordHandler, null, false, 0L);
			} else if (method.equals("stream")) {
				ValueMapper<Object, Object> valueMapper = new ValueMapper<Object, Object>() {
					@Override
					public Object apply(Object arg0) {
						Object result;
						if (arg0 == null) {
							result = "size=0";
						} else {
							result = "size=" + arg0.toString().length();
						}
						return result;
					}
				};
				String newTopic = topic + "New";
				KafkaTool.stream(server, topic, newTopic, valueMapper);
			}
		}
	}
}
