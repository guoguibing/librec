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
package net.librec.io;

import net.librec.conf.Configurable;
import net.librec.conf.Configuration;
import net.librec.util.ReflectionUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factories for non-public writables. Defining a factory permits
 * to be able to construct instances of non-public
 * classes.
 */
public class WritableFactories {
    private static final Map<Class, WritableFactory> CLASS_TO_FACTORY = new ConcurrentHashMap<Class, WritableFactory>();

    private WritableFactories() {
    } // singleton

    /**
     * Define a factory for a class.
     *
     * @param c        Class object
     * @param factory  WritableFactory object
     */
    public static void setFactory(Class c, WritableFactory factory) {
        CLASS_TO_FACTORY.put(c, factory);
    }

    /**
     * Define a factory for a class.
     *
     * @param c Class object
     * @return  WritableFactory object
     */
    public static WritableFactory getFactory(Class c) {
        return CLASS_TO_FACTORY.get(c);
    }

    /**
     * Create a new instance of a class with a defined factory.
     *
     * @param c    {@code Class<? extends Writable>} object
     * @param conf Configuration object
     * @return     Writable object
     */
    public static Writable newInstance(Class<? extends Writable> c, Configuration conf) {
        WritableFactory factory = WritableFactories.getFactory(c);
        if (factory != null) {
            Writable result = factory.newInstance();
            if (result instanceof Configurable) {
                ((Configurable) result).setConf(conf);
            }
            return result;
        } else {
            return ReflectionUtil.newInstance(c, conf);
        }
    }
    /**
     * Create a new instance of a class with a defined factory.
     *
     * @param c  {@code Class<? extends Writable>} object
     * @return   Writable object
     */
    public static Writable newInstance(Class<? extends Writable> c) {
        return newInstance(c, null);
    }

}