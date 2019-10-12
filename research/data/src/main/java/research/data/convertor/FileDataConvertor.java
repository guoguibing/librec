/**
 * 
 */
package research.data.convertor;

import java.io.IOException;

import research.core.data.Dataset;

/**
 * File数据转换器
 * @author ZhengYangPing
 *
 */
public abstract class FileDataConvertor implements DataConvertor{
	
	/**
     * store rate data as {user, item, rate} matrix
     */
    protected Dataset matrix;
    
    @Override
	public Dataset getMatrix() {
		if (null == matrix){
            try {
                processData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return matrix;
	}
}
