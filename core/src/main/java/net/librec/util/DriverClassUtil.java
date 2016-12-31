/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.librec.recommender.Recommender;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Driver Class Util
 *
 * @author WangYuFeng
 */
public class DriverClassUtil {
    /**
     * driver Class BiMap matches configuration of driver.classes.props
     */
    private static BiMap<String, String> driverClassBiMap;
    /**
     * inverse configuration of driver.classes.props
     */
    private static BiMap<String, String> driverClassInverseBiMap;

    static {
        driverClassBiMap = HashBiMap.create();
        Properties prop = new Properties();
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
        Iterator<Entry<Object, Object>> propIte = prop.entrySet().iterator();
        while (propIte.hasNext()) {
            Entry<Object, Object> entry = propIte.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            driverClassBiMap.put(key, value);
        }
        driverClassInverseBiMap = driverClassBiMap.inverse();
    }

    /**
     * get Class by driver name.
     *
     * @param driver driver name
     * @return  Class object
     * @throws ClassNotFoundException  if can't find the Class
     */
    public static Class<?> getClass(String driver) throws ClassNotFoundException {
        if (StringUtils.isBlank(driver)) {
            return null;
        } else if (StringUtils.contains(driver, ".")) {
            return Class.forName(driver);
        } else {
            String fullName = driverClassBiMap.get(driver);
            return Class.forName(fullName);
        }
    }

    /**
     * get Driver Name by clazz
     *
     * @param clazz  clazz name
     * @return  driver name
     * @throws ClassNotFoundException if can't find the Class
     */
    public static String getDriverName(String clazz) throws ClassNotFoundException {
        if (StringUtils.isBlank(clazz)) {
            return null;
        } else {
            return driverClassInverseBiMap.get(clazz);
        }
    }

    /**
     * get Driver Name by clazz
     *
     * @param clazz clazz name
     * @return driver name
     * @throws ClassNotFoundException if can't find the Class
     */
    public static String getDriverName(Class<? extends Recommender> clazz) throws ClassNotFoundException {
        if (clazz == null) {
            return null;
        } else {
            String driverName = driverClassInverseBiMap.get(clazz.getName());
            if (StringUtils.isNotBlank(driverName)) {
                return driverName;
            } else {
                return clazz.getSimpleName().toLowerCase().replace("recommender", "");
            }
        }
    }
}
