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

import net.librec.conf.Configuration;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.util.ReflectionUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;

/**
 * Recommender Model File
 *
 * @author WangYuFeng
 */
public class ModelFile {

    private static Log LOG = LogFactory.getLog(ModelFile.class);

    public static final byte[] VERSION = new byte[]{'L', 'I', 'B', 'R', 'E', 'C', '_', '2', '0', '0'};

    /**
     * Write model to a model-format file.
     */
    public static class Writer {
        Configuration conf;
        // DataOutputStream out;
        // DataOutputBuffer buffer = new DataOutputBuffer();

        public Writer() throws IOException {
        }

        public Writer(DataOutputStream out) throws IOException {
            writeFileHeader(out);
        }

        private void writeFileHeader(DataOutputStream out) throws IOException {
            out.write(VERSION);
            out.flush();
        }

        public void writeData(DataOutput out, Object val) throws IOException {
            WritableEnum type = WritableEnum.getWritableEnum(val);
            switch (type) {

                case NULL:
                    out.writeByte(WritableEnum.NULL.getValue());
                    break;

                case INTWRITABLE:
                    out.writeByte(WritableEnum.INTWRITABLE.getValue());
                    if (val instanceof Writable) {
                        ((IntWritable) val).write(out);
                    }
                    break;

                case LONGWRITABLE:
                    out.writeByte(WritableEnum.LONGWRITABLE.getValue());
                    if (val instanceof Writable) {
                        ((LongWritable) val).write(out);
                    }
                    break;

                case DATETIMEWRITABLE:
                    out.writeByte(WritableEnum.DATETIMEWRITABLE.getValue());
                    if (val instanceof Writable) {
                        ((DatetimeWritable) val).write(out);
                    }
                    break;

                case DOUBLEWRITABLE:
                    out.writeByte(WritableEnum.DOUBLEWRITABLE.getValue());
                    if (val instanceof Writable) {
                        ((DoubleWritable) val).write(out);
                    }
                    break;

                case BOOLEANWRITABLE:
                    out.writeByte(WritableEnum.BOOLEANWRITABLE.getValue());
                    if (val instanceof Writable) {
                        ((BooleanWritable) val).write(out);
                    }
                    break;

                case TEXT:
                    out.writeByte(WritableEnum.TEXT.getValue());
                    if (val instanceof Writable) {
                        ((Text) val).write(out);
                    } else {
                        (new Text(val)).write(out);
                    }
                    break;

                case DENSEVECTOR:
                    out.writeByte(WritableEnum.DENSEVECTOR.getValue());
                    if (val instanceof Writable) {
                        ((DenseVectorWritable) val).write(out);
                    } else {
                        (new DenseVectorWritable((DenseVector) val)).write(out);
                    }
                    break;

                case DENSEMATRIX:
                    out.writeByte(WritableEnum.DENSEMATRIX.getValue());
                    if (val instanceof Writable) {
                        ((DenseMatrixWritable) val).write(out);
                    } else {
                        (new DenseMatrixWritable((DenseMatrix) val)).write(out);
                    }
                    break;

                case SPARSEVECTOR:
                    out.writeByte(WritableEnum.SPARSEVECTOR.getValue());
                    ((SparseVectorWritable) val).write(out);
                    break;

                case SPARSEMATRIX:
                    out.writeByte(WritableEnum.SPARSEMATRIX.getValue());
                    ((SparseMatrixWritable) val).write(out);
                    break;

                case SYMMMATRIX:
                    out.writeByte(WritableEnum.SYMMMATRIX.getValue());
                    ((SymmMatrixWritable) val).write(out);
                    break;

                case BIMAP:
                    out.writeByte(WritableEnum.BIMAP.getValue());
                    ((BiMapWritable) val).write(out);
                    break;

                case NULLWRITABLE:
                    out.writeByte(WritableEnum.NULLWRITABLE.getValue());
                    ((NullWritable) val).write(out);
                    break;

                case UNKNOWN:
                    out.writeByte(WritableEnum.UNKNOWN.getValue());
                    out.writeUTF(val.getClass().getName());
                    ((Writable) val).write(out);
                    break;

                default:
                    throw new RuntimeException("Unexpected data type " + type + " found in stream.");
            }
        }
    }

    /**
     * Reads values from a model-format file.
     */
    public static class Reader {
        Configuration conf;
        // DataInputStream in;

        public Reader() throws IOException {
        }

        public Reader(DataInputStream in) throws IOException {
            readFileHeader(in);
        }

        private void readFileHeader(DataInputStream in) throws IOException {
            byte[] versionBlock = new byte[VERSION.length];
            String exceptionMsg = this + " not a ModelFile";
            try {
                in.readFully(versionBlock);
            } catch (EOFException e) {
                throw new EOFException(exceptionMsg);
            }
            if ((versionBlock[0] != VERSION[0]) || (versionBlock[1] != VERSION[1]) || (versionBlock[2] != VERSION[2])
                    || (versionBlock[3] != VERSION[3]) || (versionBlock[4] != VERSION[4])
                    || (versionBlock[5] != VERSION[5])) {
                throw new IOException(this + " not a ModelFile");
            }
        }

        /**
         * Read the next key/value pair in the file into <code>key</code> and
         * <code>val</code>. Returns true if such a pair exists and false when
         * at end of file
         *
         * @param in  the input stream
         * @return    a writable object
         * @throws IOException
         *         if IOException happens during reading
         */
        public synchronized Writable readData(DataInput in) throws IOException {
            WritableEnum type = WritableEnum.getWritableEnum(in.readByte());
            switch (type) {

                case NULL:
                    return null;

                case INTWRITABLE:
                    IntWritable iw = new IntWritable();
                    iw.readFields(in);
                    return iw;

                case LONGWRITABLE:
                    LongWritable lw = new LongWritable();
                    lw.readFields(in);
                    return lw;

                case DATETIMEWRITABLE:
                    DatetimeWritable dtw = new DatetimeWritable();
                    dtw.readFields(in);
                    return dtw;

                case DOUBLEWRITABLE:
                    DoubleWritable dw = new DoubleWritable();
                    dw.readFields(in);
                    return dw;

                case BOOLEANWRITABLE:
                    BooleanWritable bw = new BooleanWritable();
                    bw.readFields(in);
                    return bw;

                case TEXT:
                    Text t = new Text();
                    t.readFields(in);
                    return t;

                case DENSEVECTOR:
                    DenseVectorWritable dv = new DenseVectorWritable();
                    dv.readFields(in);
                    return dv;

                case DENSEMATRIX:
                    DenseMatrixWritable dm = new DenseMatrixWritable();
                    dm.readFields(in);
                    return dm;

                case SPARSEVECTOR:
                    SparseVectorWritable sv = new SparseVectorWritable();
                    sv.readFields(in);
                    return sv;

                case SPARSEMATRIX:
                    SparseMatrixWritable sm = new SparseMatrixWritable();
                    sm.readFields(in);
                    return sm;

                case SYMMMATRIX:
                    SymmMatrixWritable smm = new SymmMatrixWritable();
                    smm.readFields(in);
                    return smm;

                case BIMAP:
                    BiMapWritable bmw = new BiMapWritable();
                    bmw.readFields(in);
                    return bmw;

                case NULLWRITABLE:
                    NullWritable nw = NullWritable.get();
                    nw.readFields(in);
                    return nw;

                case UNKNOWN:
                    String clsName = in.readUTF();
                    try {
                        Class<? extends Writable> cls = (Class<? extends Writable>) Class.forName(clsName);
                        Writable w = (Writable) ReflectionUtil.newInstance(cls, null);
                        w.readFields(in);
                        return w;
                    } catch (RuntimeException re) {
                        LOG.info(re.getMessage());
                        throw new IOException(re);
                    } catch (ClassNotFoundException cnfe) {
                        throw new IOException(cnfe);
                    }

                default:
                    throw new RuntimeException("Unexpected data type " + type + " found in stream.");
            }
        }

    }

}
