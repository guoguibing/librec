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
package net.librec.data.convertor;

import net.librec.conf.Configuration;
import net.librec.data.DataConvertor;
import net.librec.job.progress.ProgressReporter;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SparseTensor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * A <tt>AbstractDataConvertor</tt> is a class to convert
 * a data file from one source format to a target format.
 *
 * @author WangYuFeng
 */
public abstract class AbstractDataConvertor extends ProgressReporter implements DataConvertor {
    protected final Log LOG = LogFactory.getLog(this.getClass());

    /**
     * store rate data as {user, item, rate} matrix
     */
    protected DataFrame matrix;

    /**
     * store rate data as {user, item, rate} matrix
     */
    protected SequentialAccessSparseMatrix preferenceMatrix;

    /**
     * store time data as {user, item, datetime} matrix
     */
    protected SequentialAccessSparseMatrix datetimeMatrix;

    /**
     * store rate data as a sparse tensor
     */
    protected SparseTensor sparseTensor;

    /**
     * Return the rate matrix.
     *
     * @return {@link #matrix}
     */
    @Override
    public DataFrame getMatrix() {
        if (null == matrix){
            try {
                processData();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
        return matrix;
    }

    @Override
    public SequentialAccessSparseMatrix getPreferenceMatrix(){
        if (null == preferenceMatrix){
            preferenceMatrix = getMatrix().toSparseMatrix("preferenceMatrix");
        }
        return preferenceMatrix;
    }

    @Override
    public SequentialAccessSparseMatrix getPreferenceMatrix(Configuration conf){
        if (null == preferenceMatrix){
            preferenceMatrix = getMatrix().toSparseMatrix(conf);
        }
        return preferenceMatrix;
    }

    @Override
    public SequentialAccessSparseMatrix getDatetimeMatrix(){
        if (null == datetimeMatrix){
            datetimeMatrix = getMatrix().toSparseMatrix("datetimeMatrix");
        }
        return datetimeMatrix;
    }

    @Override
    public  SparseTensor getSparseTensor(){
        if (null == sparseTensor){
            sparseTensor = getMatrix().toSparseTensor();
        }
        return sparseTensor;
    }

    @Override
    public  SparseTensor getSparseTensor(String[] indices, String valueIndex){
        if (null == sparseTensor){
            sparseTensor = getMatrix().toSparseTensor(indices, valueIndex);
        }
        return sparseTensor;
    }
}
