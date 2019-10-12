package research.data.reader.stream;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * 数据处理工具接口
 * 
 * @author liweigu714@163.com
 * 
 */
public interface ConsumerRecordHandler {
	/**
	 * 数据处理
	 * 
	 * @param record 数据
	 * @return 是否还要持续取数据
	 */
	public boolean handle(List<ConsumerRecord<String, String>> records);
}
