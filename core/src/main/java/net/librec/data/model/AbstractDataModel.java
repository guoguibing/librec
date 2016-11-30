/**
 * Copyright (C) 2016 LibRec
 * 
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.data.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.librec.conf.Configured;
import net.librec.data.DataFeature;
import net.librec.data.DataModel;
import net.librec.data.DataSplitter;

/**
 * AbstractDataModel
 * 
 * @author WangYuFeng
 */
public abstract class AbstractDataModel extends Configured implements DataModel {
	/**
	 * LOG
	 */
	protected final Log LOG = LogFactory.getLog(this.getClass());
	/**
	 * Data Splitter {@link net.librec.data.DataSplitter}
	 */
	public DataSplitter dataSplitter;
	/**
	 * Data Splitter {@link net.librec.data.DataFeature}
	 */
	public DataFeature dataFeature;
	
	public DataFeature getDataFeature() {
		return null;
	}

	/**
	 * @return the data Splitter
	 */
	public DataSplitter getDataSplitter() {
		return dataSplitter;
	}
}
