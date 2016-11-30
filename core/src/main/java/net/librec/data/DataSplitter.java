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

import net.librec.common.LibrecException;
import net.librec.math.structure.SparseMatrix;

/**
 * Data Splitter
 * 
 * @author WangYuFeng
 */
public interface DataSplitter {
	enum SplitterType {
		GENERIC, GIEVNN, RATIO, VALIDATION
	}
	public void splitData() throws LibrecException;
	
	/**
	 * setDataConvertor
	 * @param dataConvertor
	 */
	public void setDataConvertor(DataConvertor dataConvertor);
	
	/**
	 * getTrainData
	 * 
	 * @return
	 */
	public SparseMatrix getTrainData();

	/**
	 * getTestData
	 * 
	 * @return
	 */
	public SparseMatrix getTestData();

	/**
	 * getValidData
	 * 
	 * @return
	 */
	public SparseMatrix getValidData();

}
