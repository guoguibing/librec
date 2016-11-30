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
package net.librec.data;

import com.google.common.collect.BiMap;

import net.librec.common.LibrecException;

/**
 * DataModel
 * 
 * @author WangYuFeng
 */
public interface DataModel {
	/**
	 * build Data Model
	 * @throws LibrecException
	 */
	public void buildDataModel() throws LibrecException;
	/**
	 * load Data Model
	 * @throws LibrecException
	 */
	public void loadDataModel() throws LibrecException;
	/**
	 * save Data Model
	 * @throws LibrecException
	 */
	public void saveDataModel() throws LibrecException;
	/**
	 * get Data Splitter
	 * @return
	 */
	public DataSplitter getDataSplitter();
	/**
	 * get User Mapping Data
	 * @return
	 */
	public BiMap<String, Integer> getUserMappingData();
	/**
	 * get Item Mapping Data
	 * @return
	 */
	public BiMap<String, Integer> getItemMappingData();
	/**
	 * get Data Feature
	 * @return
	 */
	public DataFeature getDataFeature();
}
