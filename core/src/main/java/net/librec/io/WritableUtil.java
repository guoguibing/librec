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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Writable utility class.
 *
 * @author WangYuFeng
 */
public class WritableUtil {

    /**
     * Skip compressed byte array.
     *
     * @param in  the input stream
     * @throws IOException  if I/O error occurs
     */
    public static void skipCompressedByteArray(DataInput in) throws IOException {
        int length = in.readInt();
        if (length != -1) {
            skipFully(in, length);
        }
    }


    /**
     * Write a <code>String</code> value to the output stream.
     *
     * @param out  the output stream
     * @param s    the <code>String</code> value to be written
     * @throws IOException  if IOException happens
     */
    public static void writeString(DataOutput out, String s) throws IOException {
        if (s != null) {
            byte[] buffer = s.getBytes("UTF-8");
            int len = buffer.length;
            out.writeInt(len);
            out.write(buffer, 0, len);
        } else {
            out.writeInt(-1);
        }
    }

    /**
     * Read a <code>String</code> value from the input stream and returns it.
     *
     * @param in  the input stream
     * @return    the <code>String</code> value read from the stream
     * @throws IOException  if IOException happens
     */
    public static String readString(DataInput in) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        byte[] buffer = new byte[length];
        in.readFully(buffer); // could/should use readFully(buffer,0,length)?
        return new String(buffer, "UTF-8");
    }

    /**
     * Write an array of <code>String</code> objects to the output stream.
     *
     * @param out  the output stream
     * @param s    the array of <code>String</code> to be written
     * @throws IOException  if IOException happens
     */
    public static void writeStringArray(DataOutput out, String[] s) throws IOException {
        out.writeInt(s.length);
        for (int i = 0; i < s.length; i++) {
            writeString(out, s[i]);
        }
    }

    /**
     * Read an array of <code>String</code> objects from the input stream
     * and returns it.
     *
     * @param in  the input stream
     * @return    the array of <code>String</code> read from the stream
     * @throws IOException  if IOException happens
     */
    public static String[] readStringArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len == -1) {
            return null;
        }
        String[] s = new String[len];
        for (int i = 0; i < len; i++) {
            s[i] = readString(in);
        }
        return s;
    }

    /**
     * Write a <code>int</code> value to the output stream.
     *
     * @param stream  the output stream
     * @param i       the <code>int</code> value to be written
     * @throws IOException  if IOException happens
     */
    public static void writeVInt(DataOutput stream, int i) throws IOException {
        writeVLong(stream, i);
    }

    /**
     * Write a <code>long</code> value to the output stream.
     *
     * @param stream  the output stream
     * @param i       the <code>long</code> value to be written
     * @throws IOException  if IOException happens
     */
    public static void writeVLong(DataOutput stream, long i) throws IOException {
        if (i >= -112 && i <= 127) {
            stream.writeByte((byte) i);
            return;
        }

        int len = -112;
        if (i < 0) {
            i ^= -1L; // take one's complement'
            len = -120;
        }

        long tmp = i;
        while (tmp != 0) {
            tmp = tmp >> 8;
            len--;
        }

        stream.writeByte((byte) len);

        len = (len < -120) ? -(len + 120) : -(len + 112);

        for (int idx = len; idx != 0; idx--) {
            int shiftbits = (idx - 1) * 8;
            long mask = 0xFFL << shiftbits;
            stream.writeByte((byte) ((i & mask) >> shiftbits));
        }
    }

    /**
     * Read a <code>long</code> value from the input stream and returns it.
     *
     * @param stream  the input stream
     * @return        the <code>long</code> value read from the input stream
     * @throws IOException  if IOException happens
     */
    public static long readVLong(DataInput stream) throws IOException {
        byte firstByte = stream.readByte();
        int len = decodeVIntSize(firstByte);
        if (len == 1) {
            return firstByte;
        }
        long i = 0;
        for (int idx = 0; idx < len - 1; idx++) {
            byte b = stream.readByte();
            i = i << 8;
            i = i | (b & 0xFF);
        }
        return (isNegativeVInt(firstByte) ? (i ^ -1L) : i);
    }

    /**
     * Read a <code>int</code> value from the input stream and returns it.
     *
     * @param stream  the input stream
     * @return        the <code>int</code> value read from the input stream
     * @throws IOException  if IOException happens
     */
    public static int readVInt(DataInput stream) throws IOException {
        return (int) readVLong(stream);
    }

    /**
     * Check if the <code>int</code> value is negative.
     * Returning true means the <code>int</code> value is negative.
     * Returning false means the <code>int</code> value is not negative.
     *
     * @param value  first byte of the <code>int</code> value
     * @return       true if the <code>int</code> value is negative
     */
    public static boolean isNegativeVInt(byte value) {
        return value < -120 || (value >= -112 && value < 0);
    }

    /**
     * Parse the first byte of a vint/vlong to determine the number of bytes.
     *
     * @param value the first byte of the vint/vlong
     * @return number of bytes
     */
    public static int decodeVIntSize(byte value) {
        if (value >= -112) {
            return 1;
        } else if (value < -120) {
            return -119 - value;
        }
        return -111 - value;
    }

    /**
     * Get the encoded length if an integer is stored in a variable-length format.
     *
     * @param i  the encoded length
     * @return   the total number of bytes (1 to 9)
     */
    public static int getVIntSize(long i) {
        if (i >= -112 && i <= 127) {
            return 1;
        }

        if (i < 0) {
            i ^= -1L; // take one's complement'
        }
        // find the number of bytes with non-leading zeros
        int dataBits = Long.SIZE - Long.numberOfLeadingZeros(i);
        // find the number of data bytes + length byte
        return (dataBits + 7) / 8 + 1;
    }

    /**
     * Read an Enum value from DataInput, Enums are read and written
     * using String values.
     *
     * @param in        DataInput to read from
     * @param enumType  Class type of Enum
     * @param <T>       Enum type
     * @return          Enum represented by String read from DataInput
     * @throws IOException  if IOException happens
     */
    public static <T extends Enum<T>> T readEnum(DataInput in, Class<T> enumType) throws IOException {
        return T.valueOf(enumType, Text.readString(in));
    }

    /**
     * Write String value of enum to DataOutput.
     *
     * @param out      DataOutput to write for
     * @param enumVal  enum value
     * @throws IOException  if IOException happens
     */
    public static void writeEnum(DataOutput out, Enum<?> enumVal) throws IOException {
        Text.writeString(out, enumVal.name());
    }

    /**
     * Skip <i>len</i> number of bytes in input stream<i>in</i>.
     *
     * @param in  input stream
     * @param len number of bytes to skip
     * @throws IOException  if IOException happens
     */
    public static void skipFully(DataInput in, int len) throws IOException {
        int total = 0;
        int cur = 0;

        while ((total < len) && ((cur = in.skipBytes(len - total)) > 0)) {
            total += cur;
        }

        if (total < len) {
            throw new IOException("Not able to skip " + len + " bytes, possibly " + "due to end of input.");
        }
    }

    public static byte[] toByteArray(Writable... writables) {
        final DataOutputBuffer out = new DataOutputBuffer();
        try {
            for (Writable w : writables) {
                w.write(out);
            }
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Fail to convert writables to a byte array", e);
        }
        return out.getData();
    }

}
