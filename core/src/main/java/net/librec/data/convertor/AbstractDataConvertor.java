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
package net.librec.data.convertor;

import net.librec.data.DataConvertor;
import net.librec.job.progress.ProgressReporter;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseTensor;

/**
 * Abstract Data Convertor
 * 
 * @author WangYuFeng
 */
public abstract class AbstractDataConvertor extends ProgressReporter implements DataConvertor {

	protected SparseMatrix preferenceMatrix;

	protected SparseMatrix datetimeMatrix;

	protected SparseTensor sparseTensor;

	public SparseMatrix getPreferenceMatrix() {
		return preferenceMatrix;
	}

	public SparseMatrix getDatetimeMatrix() {
		return datetimeMatrix;
	}

	public SparseTensor getSparseTensor() {
		return sparseTensor;
	}

}
