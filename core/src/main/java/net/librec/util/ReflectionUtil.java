package net.librec.util;

import net.librec.conf.Configurable;
import net.librec.conf.Configuration;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//import net.librec.conf.Configurable;

public class ReflectionUtil {
    private static final Class<?>[] EMPTY_ARRAY = new Class[]{};
    /**
     * Cache of constructors for each class. Pins the classes so they can't be
     * garbage collected until ReflectionUtils can be collected.
     */
    private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<Class<?>, Constructor<?>>();

    /**
     * Create an object for the given class and initialize it from conf
     *
     * @param <T> type parameter
     * @param theClass   a given Class object
     * @param paramClass Class type of the constructor
     * @param paramValue object for the constructor
     * @return a new object
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> theClass, Class<?> paramClass, Object paramValue) {
        T result;
        try {
            Constructor<T> meth = (Constructor<T>) CONSTRUCTOR_CACHE.get(theClass);
            if (meth == null) {
                meth = theClass.getDeclaredConstructor(paramClass);
                meth.setAccessible(true);
                CONSTRUCTOR_CACHE.put(theClass, meth);
            }
            result = meth.newInstance(paramValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Create an object for the given class and initialize it from conf
     *
     * @param <T> type parameter
     * @param theClass class of which an object is created
     * @param conf     Configuration
     * @return a new object
     */
    public static <T> T newInstance(Class<T> theClass, Configuration conf) {
        T result = newInstance(theClass);
        setConf(result, conf);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> theClass) {
        T result;
        try {
            Constructor<T> meth = (Constructor<T>) CONSTRUCTOR_CACHE.get(theClass);
            if (meth == null) {
                meth = theClass.getDeclaredConstructor(EMPTY_ARRAY);
                meth.setAccessible(true);
                CONSTRUCTOR_CACHE.put(theClass, meth);
            }
            result = meth.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Check and set 'configuration' if necessary.
     *
     * @param theObject object for which to set configuration
     * @param conf      Configuration
     */
    public static void setConf(Object theObject, Configuration conf) {
        if (conf != null) {
            if (theObject instanceof Configurable) {
                ((Configurable) theObject).setConf(conf);
            }
        }
    }

}
