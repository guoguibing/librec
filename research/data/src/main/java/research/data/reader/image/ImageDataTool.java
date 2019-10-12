package research.data.reader.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图片数据读写工具
 * 
 * @author liweigu
 *
 */
public class ImageDataTool {
	private static Logger LOG = LoggerFactory.getLogger(ImageDataTool.class);

	/**
	 * 读取图片
	 * 
	 * @param path 图片路径
	 * @param width 宽度
	 * @param height 高度
	 * @param channel 通道数
	 * @return 图片数据
	 */
	public static double[] readImage(String path, int width, int height, int channel) {
		double[] values = null;

		File imgFile = new File(path);
		if (imgFile.exists()) {
			try {
				BufferedImage bufferedImage = ImageIO.read(imgFile);
				if (bufferedImage == null) {
					System.out.println("bufferedImage == null, 使用白图数据. path = " + path);
				}
				values = new double[channel * width * height];
				for (int i = 0; i < width; i++) {
					for (int j = 0; j < height; j++) {
						int r, g, b;
						if (bufferedImage != null) {
							// LOG.debug("i = " + i + ", j = " + j);
							int rgb = bufferedImage.getRGB(i, j);
							Color color = new Color(rgb);
							// [0, 255]
							r = color.getRed();
							g = color.getGreen();
							b = color.getBlue();
						} else {
							// 使用白图数据
							r = g = b = 255;
						}
						// 将值正则化到[-1,1]区间
						double rValue = norm(r);
						values[(i * height + j)] = rValue;

						if (channel == 3) {
							// 彩色图还需要设置g/b值
							double gValue = norm(g);
							double bValue = norm(b);
							values[(i * height + j) + 1 * width * height] = gValue;
							values[(i * height + j) + 2 * width * height] = bValue;
						}
					}
				}
			} catch (IOException e) {
				LOG.info("读取图片发生异常", e);
			}
		} else {
			LOG.info("读取图片失败，文件不存在。path = " + path);
		}

		return values;
	}

	/**
	 * 写入图片
	 * 
	 * @param path 图片路径
	 * @param values 图片数据
	 * @param width 宽度
	 * @param height 高度
	 * @param channel 通道数
	 */
	public static void writeImage(String path, double[] values, int width, int height, int channel) {
		try {
			if (values == null) {
				LOG.info("values == null");
			} else {
				BufferedImage image = null;

				int imageType;
				if (channel == 1) {
					imageType = BufferedImage.TYPE_BYTE_GRAY;
				} else {
					imageType = BufferedImage.TYPE_INT_RGB;
				}
				image = new BufferedImage(width, height, imageType);
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						for (int band = 0; band < channel; band++) {
							int index = (x * height + y) + band * width * height;
							double value = values[index];
							value = unnorm(value);
							image.getRaster().setSample(x, y, band, value);
						}
					}
				}

				ImageIO.write(image, "jpg", new File(path));
			}
		} catch (IOException e) {
			LOG.info("输出图片发生异常", e);
		}
	}

	// TODO: 开放标准化数据方法
	private static double norm(double value) {
		return value;
//		return (value - 127.5) / 127.5;
	}

	// TODO: 开放反标准化数据方法
	private static double unnorm(double value) {
		return value;
//		return value * 127.5 + 127.5;
	}
}
