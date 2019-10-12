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
package research.model.common;


import research.model.conf.Configuration;

/**
 * Abstract Context
 *
 * @author WangYuFeng
 */
public class AbstractContext implements LibrecContext {

    protected Configuration conf;

    /*
     * (non-Javadoc)
     * 
     * @see net.librec.common.Context#getConf()
     */
    @Override
    public Configuration getConf() {
        return this.conf;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.librec.common.Context#setConf()
     */
    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
    }

}
