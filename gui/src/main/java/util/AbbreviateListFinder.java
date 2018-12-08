package util;
import net.librec.util.DriverClassUtil;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * <tt>AbbreviateListFinder</tt> gets the abbreviate list of classes.
 *
 * @author SunYatong
 */
public class AbbreviateListFinder {
    /**
     * configurations of driver.classes.props
     */
    private static Properties prop;

    static {
        prop = new Properties();
        InputStream is = null;
        try {
            is = DriverClassUtil.class.getClassLoader().getResourceAsStream("driver.classes.props");
            prop.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Return a list of abbreviates of classes with specified key word.
     *
     * @param keyword key word of the classes' full name.
     * @return a list of abbreviates of the classes
     */
    public static List<String> getListByKeyWord(String keyword) {
        ArrayList<String> abbrevList = new ArrayList<>();
        Iterator<Map.Entry<Object, Object>> propIter = prop.entrySet().iterator();

        while (propIter.hasNext()) {
            Map.Entry<Object, Object> entry = propIter.next();
            String value = (String) entry.getValue();
            if (StringUtils.contains(value, keyword)) {
                String key = (String) entry.getKey();
                abbrevList.add(key);
            }
        }
        return abbrevList;
    }



}
