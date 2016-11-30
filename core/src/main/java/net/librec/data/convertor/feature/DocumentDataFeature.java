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
package net.librec.data.convertor.feature;

import net.librec.data.DataFeature;
import net.librec.math.structure.SparseMatrix;

/**
 * Document Data Feature
 * @author YuFeng Wang
 */
public class DocumentDataFeature implements DataFeature {

	/* (non-Javadoc)
	 * @see net.librec.data.DataFeature#getUserFeature()
	 */
	public SparseMatrix getUserFeature() {
		return null;
	}

	/* (non-Javadoc)
	 * @see net.librec.data.DataFeature#getItemFeature()
	 */
	public SparseMatrix getItemFeature() {
		return null;
	}

}
