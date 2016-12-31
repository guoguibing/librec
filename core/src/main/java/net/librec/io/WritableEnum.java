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

import com.google.common.collect.BiMap;
import net.librec.math.structure.*;

import java.util.Date;

/**
 * Writable Enum
 *
 * @author WangYuFeng
 */
public enum WritableEnum {

    UNKNOWN((byte) 0),
    NULL((byte) 1, NullWritable.class),
    NULLWRITABLE((byte) 2, NullWritable.class),
    BOOLEANWRITABLE((byte) 3, BooleanWritable.class),
    INTWRITABLE((byte) 5, IntWritable.class),
    LONGWRITABLE((byte) 6, LongWritable.class),
    DATETIMEWRITABLE((byte) 7, DatetimeWritable.class),
    DOUBLEWRITABLE((byte) 8, DoubleWritable.class),
    TEXT((byte) 9, Text.class),
    DENSEVECTOR((byte) 100, DenseVectorWritable.class),
    DENSEMATRIX((byte) 101, DenseMatrixWritable.class),
    SPARSEVECTOR((byte) 102, SparseVectorWritable.class),
    SPARSEMATRIX((byte) 103, SparseMatrixWritable.class),
    SYMMMATRIX((byte) 104, SymmMatrixWritable.class),
    BIMAP((byte) 105, BiMapWritable.class);

    private byte value;
    private Class<? extends Writable> clazz;

    WritableEnum(byte value) {
        this.value = value;
    }

    WritableEnum(byte value, Class<? extends Writable> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    public static WritableEnum getWritableEnum(byte type) {
        switch (type) {
            case (byte) 0:
                return UNKNOWN;
            case (byte) 1:
                return NULL;
            case (byte) 2:
                return NULLWRITABLE;
            case (byte) 3:
                return BOOLEANWRITABLE;
            case (byte) 5:
                return INTWRITABLE;
            case (byte) 6:
                return LONGWRITABLE;
            case (byte) 7:
                return DATETIMEWRITABLE;
            case (byte) 8:
                return DOUBLEWRITABLE;
            case (byte) 9:
                return TEXT;
            case (byte) 100:
                return DENSEVECTOR;
            case (byte) 101:
                return DENSEMATRIX;
            case (byte) 102:
                return SPARSEVECTOR;
            case (byte) 103:
                return SPARSEMATRIX;
            case (byte) 104:
                return SYMMMATRIX;
            case (byte) 105:
                return BIMAP;
            default:
                break;
        }
        return UNKNOWN;
    }

    public static WritableEnum getWritableEnum(Object o) {
        if (o == null) {
            return NULL;
        }

        // Try to put the most common first
        if (o instanceof LongWritable || o instanceof Long) {
            return LONGWRITABLE;
        } else if (o instanceof IntWritable || o instanceof Integer) {
            return INTWRITABLE;
        } else if (o instanceof Text || o instanceof String) {
            return TEXT;
        } else if (o instanceof DoubleWritable || o instanceof Double) {
            return DOUBLEWRITABLE;
        } else if (o instanceof BooleanWritable || o instanceof Boolean) {
            return BOOLEANWRITABLE;
        } else if (o instanceof DatetimeWritable || o instanceof Date) {
            return DATETIMEWRITABLE;
        } else if (o instanceof DenseVectorWritable || o instanceof DenseVector) {
            return DENSEVECTOR;
        } else if (o instanceof DenseMatrixWritable || o instanceof DenseMatrix) {
            return DENSEMATRIX;
        } else if (o instanceof SparseVectorWritable || o instanceof SparseVector) {
            return SPARSEVECTOR;
        } else if (o instanceof SparseMatrixWritable || o instanceof SparseMatrix) {
            return SPARSEMATRIX;
        } else if (o instanceof SymmMatrixWritable || o instanceof SymmMatrix) {
            return SYMMMATRIX;
        } else if (o instanceof BiMapWritable || o instanceof BiMap) {
            return BIMAP;
        } else if (o instanceof NullWritable) {
            return NULLWRITABLE;
        }

        return UNKNOWN;
    }

    /**
     * @return the value
     */
    public byte getValue() {
        return value;
    }

    /**
     * @return the clazz
     */
    public Class<? extends Writable> getClazz() {
        return clazz;
    }

}
