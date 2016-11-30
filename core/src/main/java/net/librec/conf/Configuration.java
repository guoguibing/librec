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
package net.librec.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides access to configuration parameters.
 *
 * <h4 id="Resources">Resources</h4>
 *
 * <p>Configurations are specified by resources. A resource contains a set of
 * name/value pairs as key-value data. Each resource is named by either a 
 * <code>String</code> or by a {@link Path}. If named by a <code>String</code>, 
 * then the classpath is examined for a file with that name.  If named by a 
 * <code>Path</code>, then the local filesystem is examined directly, without 
 * referring to the classpath.
 *
 * <p>Unless explicitly turned off, Librec by default specifies two 
 * resources, loaded in-order from the classpath: <ol>
 * <li><tt>
 * <a href="{@docRoot}/../librec.properties">
 * librec.properties</a></tt>: Read-only defaults for librec.</li>
 * <li><tt>librec.properties</tt>: Site-specific configuration for a given librec installation.</li>
 * </ol>
 * Applications may add additional resources, which are loaded
 * subsequent to these resources in the order they are added.
 * 
 * @author WangYuFeng 
 */
public class Configuration implements Iterable<Map.Entry<String, String>> {
	private static final Log LOG = LogFactory.getLog(Configuration.class);

	private static final ConcurrentMap<ClassLoader, Map<String, Class<?>>> CACHE_CLASSES = new ConcurrentHashMap<ClassLoader, Map<String, Class<?>>>();
	private Properties properties;
	private ClassLoader classLoader;

	{
		classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = Configuration.class.getClassLoader();
		}
	}

	private boolean loadDefaults = true;
	/**
	 * List of configuration resources.
	 */
	private ArrayList<Resource> resources = new ArrayList<Resource>();

	private static final CopyOnWriteArrayList<String> defaultResources = new CopyOnWriteArrayList<String>();

	static {
		ClassLoader cL = Thread.currentThread().getContextClassLoader();
		if (cL == null) {
			cL = Configuration.class.getClassLoader();
		}
		if (cL.getResource("librec.properties") != null) {
			LOG.warn("DEPRECATED: librec.properties found in the classpath. ");
		}
//		if (cL.getResource("driver.classes.props") != null) {
//			LOG.warn("DEPRECATED: driver.classes.props found in the classpath. ");
//		}
		addDefaultResource("librec.properties");
//		addDefaultResource("driver.classes.props");
	}

	public static class Resource {
		private final Object resource;
		private final String name;

		public Resource(Object resource) {
			this(resource, resource.toString());
		}

		public Resource(Object resource, String name) {
			this.resource = resource;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Object getResource() {
			return resource;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * Add a default resource. Resources are loaded in the order of the
	 * resources added.
	 *
	 * @param name
	 *            file name. File should be present in the classpath.
	 */
	public static void addDefaultResource(String name) {
		synchronized (Configuration.class) {
			if (defaultResources.contains(name)) {
				return;
			}
			defaultResources.add(name);
		}
	}

	/**
	 * Get an {@link Iterator} to go through the list of <code>String</code>
	 * key-value pairs in the configuration.
	 * 
	 * @return an iterator over the entries.
	 */
	public Iterator<Entry<String, String>> iterator() {
		Map<String, String> result = new HashMap<String, String>();
		for (Map.Entry<Object, Object> item : getProps().entrySet()) {
			if (item.getKey() instanceof String && item.getValue() instanceof String) {
				result.put((String) item.getKey(), (String) item.getValue());
			}
		}
		return result.entrySet().iterator();
	}

	public synchronized void addResource(Resource resource) {
		resources.add(resource);
		loadProperty(getProps(), resource);
	}

	private void overlay(Properties to, Properties from) {
		for (Entry<Object, Object> entry : from.entrySet()) {
			to.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Set the <code>value</code> of the <code>name</code> property.
	 *
	 * @param name
	 *            property name.
	 * @param value
	 *            property value.
	 */
	public void set(String name, String value) {
		getProps().setProperty(name, value);
	}

	public String get(String name) {
		return getProps().getProperty(name);
	}

	public Float getFloat(String name, float defaultValue) {
		Float value = getFloat(name);
		return value != null ? value : defaultValue;
	}

	public Float getFloat(String name) {
		String value = getProps().getProperty(name);
		if (StringUtils.isNotBlank(value)) {
			return Float.valueOf(value);
		} else {
			return null;
		}
	}

	public Double getDouble(String name, double defaultValue) {
		Double value = getDouble(name);
		return value != null ? value : defaultValue;
	}

	public Double getDouble(String name) {
		String value = getProps().getProperty(name);
		if (StringUtils.isNotBlank(value)) {
			return Double.valueOf(value);
		} else {
			return null;
		}
	}

	public String get(String name, String defaultValue) {
		String value = get(name);
		return StringUtils.isNotBlank(value) ? value : defaultValue;
	}

	public Integer getInt(String name, Integer defaultValue) {
		Integer value = getInt(name);
		return value != null ? value : defaultValue;
	}

	public Integer getInt(String name) {
		String value = getProps().getProperty(name);
		if (StringUtils.isNotBlank(value)) {
			return Integer.valueOf(value);
		} else {
			return null;
		}
	}

	public boolean getBoolean(String name) {
		String value = get(name);
		return StringUtils.isNotBlank(value) ? Boolean.valueOf(value) : false;
	}

	private synchronized Properties getProps() {
		if (properties == null) {
			properties = new Properties();
			loadResources(properties, resources);
		}
		return properties;
	}

	private void loadResources(Properties properties, ArrayList<Resource> resources) {
		if (loadDefaults) {
			for (String resource : defaultResources) {
				loadProperty(properties, new Resource(resource));
			}
		}
		for (Resource resource : resources) {
			loadProperty(properties, resource);
		}
	}

	private void loadProperty(Properties properties, Resource wrapper) {
		Object resource = wrapper.getResource();
		try {
			InputStream fis;
			if (resource instanceof URL) { // an URL resource
				fis = ((URL) resource).openStream();
				properties.load(fis);
			} else if (resource instanceof String) { // a CLASSPATH resource
				URL url = getResource((String) resource);
				if (url != null) {
					URLConnection connection = url.openConnection();
					if (connection instanceof JarURLConnection) {
						// Disable caching for JarURLConnection to avoid sharing
						// JarFile
						// with other users.
						connection.setUseCaches(false);
					}
					fis = connection.getInputStream();
					properties.load(fis);
				}
			} else if (resource instanceof Path) { // a file resource
				fis = new FileInputStream(new File(((Path) resource).toUri().getPath()));
				properties.load(fis);
			} else if (resource instanceof InputStream) {
				fis = (InputStream) resource;
				properties.load(fis);
			} else if (resource instanceof Properties) {
				overlay(properties, (Properties) resource);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public URL getResource(String name) {
		return classLoader.getResource(name);
	}

	/**
	 * Load a class by name.
	 *
	 * @param name
	 *            the class name.
	 * @return the class object.
	 * @throws ClassNotFoundException
	 *             if the class is not found.
	 */
	public Class<?> getClassByName(String name) throws ClassNotFoundException {
		Map<String, Class<?>> map = CACHE_CLASSES.get(classLoader);
		if (map == null) {
			Map<String, Class<?>> newMap = new ConcurrentHashMap<String, Class<?>>();
			map = CACHE_CLASSES.putIfAbsent(classLoader, newMap);
			if (map == null) {
				map = newMap;
			}
		}
		Class<?> clazz = map.get(name);
		if (clazz == null) {
			clazz = Class.forName(name, true, classLoader);
			if (clazz != null) {
				map.put(name, clazz);
			}
		}

		return clazz;
	}

	/**
	 * Load a class by name.
	 *
	 * @param name
	 *            the class name.
	 * @param name
	 *            the default class.
	 * @return the class object.
	 * @throws ClassNotFoundException
	 *             if the class is not found.
	 */
	public Class<?> getClassByName(String name, String defaultName) throws ClassNotFoundException {
		try {
			return getClassByName(name);
		} catch (ClassNotFoundException e) {
			LOG.error(e);
			return getClassByName(defaultName);
		}
	}
}
