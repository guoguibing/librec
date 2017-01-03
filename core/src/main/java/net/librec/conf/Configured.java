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

/**
 * Base class for things that may be configured with a {@link Configuration}.
 */
public class Configured implements Configurable {

    public static final String CONF_DATA_COLUMN_FORMAT = "data.column.format";

    public static final String CONF_DFS_DATA_DIR = "dfs.data.dir";

    public static final String CONF_DATA_INPUT_PATH = "data.input.path";

    protected Configuration conf;

    /**
     * Construct a Configured.
     */
    public Configured() {
        this(null);
    }

    /**
     * Construct a Configured.
     *
     * @param conf  object for construction
     */
    public Configured(Configuration conf) {
        setConf(conf);
    }

    // inherit javadoc
    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    // inherit javadoc
    public Configuration getConf() {
        return conf;
    }

}
