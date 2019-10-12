package research.data.reader.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * 图片工具单元测试
 * 
 * @author liweigu
 *
 */
public class ImageDataToolTests {
	/**
	 * 测试图片数据读写
	 */
	@Test
	public void readWriteImage() {
		String path = "src/test/resources/image/modules.jpg";
		int width = 1024;
		int height = 661;
		int channel = 3;
		double[] values = ImageDataTool.readImage(path, width, height, channel);
		assertNotNull(values);
		String outputPath = "src/test/resources/image/output.jpg";
		System.out.println("writeImage to " + outputPath);
		ImageDataTool.writeImage(outputPath, values, width, height, channel);
		double[] outputValues = ImageDataTool.readImage(outputPath, width, height, channel);
		assertEquals(values.length, outputValues.length);
	}
}
