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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class DataInputBuffer extends DataInputStream {

    private static class Buffer extends ByteArrayInputStream {

        public Buffer() {
            super(new byte[]{});
        }

        public void reset(byte[] input, int start, int length) {
            this.buf = input;
            this.count = start + length;
            this.mark = start;
            this.pos = start;
        }

        public byte[] getData() {
            return buf;
        }

        public int getPosition() {
            return pos;
        }

        public int getLength() {
            return count;
        }
    }

    private Buffer buffer;

    /**
     * Constructs a new empty buffer
     */
    public DataInputBuffer() {
        this(new Buffer());
    }

    /**
     * Constructs from a given buffer.
     *
     * @param buffer a given buffer
     */
    private DataInputBuffer(Buffer buffer) {
        super(buffer);
        this.buffer = buffer;
    }

    /**
     * Resets the data that the buffer reads.
     *
     * @param input  the input bytes
     * @param length length of the input
     */
    public void reset(byte[] input, int length) {
        buffer.reset(input, 0, length);
    }

    /**
     * Resets the data that the buffer reads.
     *
     * @param input  the input bytes
     * @param start  start position of the buffer
     * @param length length of the input
     */
    public void reset(byte[] input, int start, int length) {
        buffer.reset(input, start, length);
    }

    /**
     * Get the data from the input.
     *
     * @return data from the input
     */
    public byte[] getData() {
        return buffer.getData();
    }

    /**
     * Returns the current position in the input
     *
     * @return the current position in the input
     */
    public int getPosition() {
        return buffer.getPosition();
    }

    /**
     * Returns the length of the input.
     *
     * @return the length of the input
     */
    public int getLength() {
        return buffer.getLength();
    }

}
