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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * This class stores text using standard UTF8 encoding.  It provides methods
 * to serialize, deserialize, and compare texts at byte level.
 */
public class Text extends BinaryComparable implements WritableComparable<BinaryComparable> {

    private static ThreadLocal<CharsetEncoder> ENCODER_FACTORY = new ThreadLocal<CharsetEncoder>() {
        protected CharsetEncoder initialValue() {
            return Charset.forName("UTF-8").newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        }
    };

    private static ThreadLocal<CharsetDecoder> DECODER_FACTORY = new ThreadLocal<CharsetDecoder>() {
        protected CharsetDecoder initialValue() {
            return Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        }
    };

    private static final byte[] EMPTY_BYTES = new byte[0];

    private byte[] bytes;
    private int length;

    /**
     * Construct from empty bytes.
     */
    public Text() {
        bytes = EMPTY_BYTES;
    }

    /**
     * Construct from a string.
     *
     * @param string  value for constructing
     */
    public Text(String string) {
        set(string);
    }

    public Text(Object string) {
        set((String) string);
    }

    /**
     * Construct from another text.
     *
     * @param utf8 another text
     */
    public Text(Text utf8) {
        set(utf8);
    }

    /**
     * Construct from a byte array
     *
     * @param utf8  a byte array
     */
    public Text(byte[] utf8) {
        set(utf8);
    }

    /**
     * Return the raw bytes; however, only data up to {@link #getLength()} is
     * valid.
     *
     * @return the raw bytes
     */
    @Override
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Return the number of bytes in the byte array
     *
     * @return the number of bytes in the byte array
     */
    @Override
    public int getLength() {
        return length;
    }

    /**
     * Set to contain the contents of a string.
     *
     * @param string  the string to be contained
     */
    public void set(String string) {
        try {
            ByteBuffer bb = encode(string, true);
            bytes = bb.array();
            length = bb.limit();
        } catch (CharacterCodingException e) {
            throw new RuntimeException("Should not have happened " + e.toString());
        }
    }

    /**
     * Set to a utf8 byte array.
     *
     * @param utf8  a utf8 byte array
     */
    public void set(byte[] utf8) {
        set(utf8, 0, utf8.length);
    }

    /**
     * Copy a text.
     *
     * @param other another text
     */
    public void set(Text other) {
        set(other.getBytes(), 0, other.getLength());
    }

    /**
     * Set the Text to range of bytes.
     *
     * @param utf8   the data to copy from
     * @param start  the first position of the new string
     * @param len    the number of bytes of the new string
     */
    public void set(byte[] utf8, int start, int len) {
        setCapacity(len, false);
        System.arraycopy(utf8, start, bytes, 0, len);
        this.length = len;
    }

    /**
     * Append a range of bytes to the end of the given text.
     *
     * @param utf8   the data to copy from
     * @param start  the first position to append from utf8
     * @param len    the number of bytes to append
     */
    public void append(byte[] utf8, int start, int len) {
        setCapacity(length + len, true);
        System.arraycopy(utf8, start, bytes, length, len);
        length += len;
    }

    /**
     * Clear the string to empty.
     */
    public void clear() {
        length = 0;
    }

    /**
     * Sets the capacity of this Text object to <em>at least</em>
     * <code>len</code> bytes. If the current buffer is longer, then the
     * capacity and existing content of the buffer are unchanged. If
     * <code>len</code> is larger than the current capacity, the Text object's
     * capacity is increased to match.
     *
     * @param len the number of bytes we need
     *
     * @param keepData should the old data be kept
     */
    private void setCapacity(int len, boolean keepData) {
        if (bytes == null || bytes.length < len) {
            byte[] newBytes = new byte[len];
            if (bytes != null && keepData) {
                System.arraycopy(bytes, 0, newBytes, 0, length);
            }
            bytes = newBytes;
        }
    }

    /**
     * Convert text back to string
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        try {
            return decode(bytes, 0, length);
        } catch (CharacterCodingException e) {
            throw new RuntimeException("Should not have happened " + e.toString());
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#readFields(java.io.DataInput)
     */
    @Override
    public void readFields(DataInput in) throws IOException {
        int newLength = WritableUtil.readVInt(in);
        setCapacity(newLength, false);
        in.readFully(bytes, 0, newLength);
        length = newLength;
    }

    /**
     * (non-Javadoc)
     *
     * @param bf ByteBuffer object
     * @see net.librec.io.Writable#readFields(java.io.DataInput)
     */
    public void readFields(ByteBuffer bf) {
        int newLength = bf.getInt();
        setCapacity(newLength, false);
        bf.get(bytes, 0, newLength);
        length = newLength;
    }

    /**
     * Skips over one Text in the input.
     *
     * @param in  input stream
     * @throws IOException
     *         if IOException happens
     */
    public static void skip(DataInput in) throws IOException {
        int length = WritableUtil.readVInt(in);
        WritableUtil.skipFully(in, length);
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#write(java.io.DataOutput)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        WritableUtil.writeVInt(out, length);
        out.write(bytes, 0, length);
    }

    /**
     * Return true if <i>o</i> is a Text with the same contents.
     *
     * @param o another object to compare with
     * @return true if <i>o</i> is a Text with the same contents.
     */
    public boolean equals(Object o) {
        if (o instanceof Text)
            return super.equals(o);
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public Object getValue() {
        return bytes;
    }

    @Override
    public void setValue(Object value) {
        this.bytes = (byte[]) value;
    }

    // / STATIC UTILITIES FROM HERE DOWN

    /**
     * Converts the provided byte array to a String using the UTF-8 encoding. If
     * the input is malformed, replace by a default value.
     *
     * @param utf8  provided byte array
     * @return  the converted string
     * @throws CharacterCodingException
     *         if character coding exception happens
     */
    public static String decode(byte[] utf8) throws CharacterCodingException {
        return decode(ByteBuffer.wrap(utf8), true);
    }

    public static String decode(byte[] utf8, int start, int length) throws CharacterCodingException {
        return decode(ByteBuffer.wrap(utf8, start, length), true);
    }

    /**
     * Converts the provided byte array to a String using the UTF-8 encoding. If
     * <code>replace</code> is true, then malformed input is replaced with the
     * substitution character, which is U+FFFD. Otherwise the method throws a
     * MalformedInputException.
     *
     * @param utf8    provided byte array
     * @param start   start position
     * @param length  length to convert
     * @param replace If <code>replace</code> is true, then malformed input is replaced with the
     *                substitution character, which is U+FFFD. Otherwise the method throws a
     *                MalformedInputException.
     * @return  converted string
     * @throws CharacterCodingException
     *         if character coding exception happens
     */
    public static String decode(byte[] utf8, int start, int length, boolean replace) throws CharacterCodingException {
        return decode(ByteBuffer.wrap(utf8, start, length), replace);
    }

    private static String decode(ByteBuffer utf8, boolean replace) throws CharacterCodingException {
        CharsetDecoder decoder = DECODER_FACTORY.get();
        if (replace) {
            decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }
        String str = decoder.decode(utf8).toString();
        // set decoder back to its default value: REPORT
        if (replace) {
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return str;
    }

    /**
     * Converts the provided String to bytes using the UTF-8 encoding. If the
     * input is malformed, invalid chars are replaced by a default value.
     *
     * @param string  provided string
     * @return ByteBuffer: bytes stores at ByteBuffer.array() and length is
     *         ByteBuffer.limit()
     * @throws CharacterCodingException
     *         if character coding exception happens
     */
    public static ByteBuffer encode(String string) throws CharacterCodingException {
        return encode(string, true);
    }

    /**
     * Converts the provided String to bytes using the UTF-8 encoding. If the
     * input is malformed, invalid chars are replaced by a default value.
     *
     * @param string  provided string
     * @param replace If <code>replace</code> is true, then malformed input is replaced with the
     *                substitution character, which is U+FFFD. Otherwise the method throws a
     *                MalformedInputException.
     * @return ByteBuffer: bytes stores at ByteBuffer.array() and length is
     *         ByteBuffer.limit()
     * @throws CharacterCodingException
     *         if character coding exception happens
     */
    public static ByteBuffer encode(String string, boolean replace) throws CharacterCodingException {
        CharsetEncoder encoder = ENCODER_FACTORY.get();
        if (replace) {
            encoder.onMalformedInput(CodingErrorAction.REPLACE);
            encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }
        ByteBuffer bytes = encoder.encode(CharBuffer.wrap(string.toCharArray()));
        if (replace) {
            encoder.onMalformedInput(CodingErrorAction.REPORT);
            encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return bytes;
    }

    /**
     * Read a UTF8 encoded string from <i>in</i>.
     *
     * @param in input stream
     * @return  the string read from <i>in</i>
     * @throws IOException
     *         if IOException happens
     */
    public static String readString(DataInput in) throws IOException {
        int length = WritableUtil.readVInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes, 0, length);
        return decode(bytes);
    }

    /**
     * Write a UTF8 encoded string to <i>out</i>.
     *
     * @param out  output stream
     * @param s    the string to be written
     * @return length
     * @throws IOException
     *         if IOException happens
     */
    public static int writeString(DataOutput out, String s) throws IOException {
        ByteBuffer bytes = encode(s);
        int length = bytes.limit();
        WritableUtil.writeVInt(out, length);
        out.write(bytes.array(), 0, length);
        return length;
    }

    // //// states for validateUTF8

    private static final int LEAD_BYTE = 0;

    private static final int TRAIL_BYTE_1 = 1;

    private static final int TRAIL_BYTE = 2;

    /**
     * Check if a byte array contains valid utf-8
     *
     * @param utf8 byte array
     * @throws MalformedInputException if the byte array contains invalid utf-8
     */
    public static void validateUTF8(byte[] utf8) throws MalformedInputException {
        validateUTF8(utf8, 0, utf8.length);
    }

    /**
     * Check to see if a byte array is valid utf-8
     *
     * @param utf8  the array of bytes
     * @param start the offset of the first byte in the array
     * @param len   the length of the byte sequence
     * @throws MalformedInputException if the byte array contains invalid bytes
     */
    public static void validateUTF8(byte[] utf8, int start, int len) throws MalformedInputException {
        int count = start;
        int leadByte = 0;
        int length = 0;
        int state = LEAD_BYTE;
        while (count < start + len) {
            int aByte = ((int) utf8[count] & 0xFF);

            switch (state) {
                case LEAD_BYTE:
                    leadByte = aByte;
                    length = bytesFromUTF8[aByte];

                    switch (length) {
                        case 0: // check for ASCII
                            if (leadByte > 0x7F)
                                throw new MalformedInputException(count);
                            break;
                        case 1:
                            if (leadByte < 0xC2 || leadByte > 0xDF)
                                throw new MalformedInputException(count);
                            state = TRAIL_BYTE_1;
                            break;
                        case 2:
                            if (leadByte < 0xE0 || leadByte > 0xEF)
                                throw new MalformedInputException(count);
                            state = TRAIL_BYTE_1;
                            break;
                        case 3:
                            if (leadByte < 0xF0 || leadByte > 0xF4)
                                throw new MalformedInputException(count);
                            state = TRAIL_BYTE_1;
                            break;
                        default:
                            // too long! Longest valid UTF-8 is 4 bytes (lead + three)
                            // or if < 0 we got a trail byte in the lead byte position
                            throw new MalformedInputException(count);
                    } // switch (length)
                    break;

                case TRAIL_BYTE_1:
                    if (leadByte == 0xF0 && aByte < 0x90)
                        throw new MalformedInputException(count);
                    if (leadByte == 0xF4 && aByte > 0x8F)
                        throw new MalformedInputException(count);
                    if (leadByte == 0xE0 && aByte < 0xA0)
                        throw new MalformedInputException(count);
                    if (leadByte == 0xED && aByte > 0x9F)
                        throw new MalformedInputException(count);
                    // falls through to regular trail-byte test!!
                case TRAIL_BYTE:
                    if (aByte < 0x80 || aByte > 0xBF)
                        throw new MalformedInputException(count);
                    if (--length == 0) {
                        state = LEAD_BYTE;
                    } else {
                        state = TRAIL_BYTE;
                    }
                    break;
            } // switch (state)
            count++;
        }
    }

    /**
     * Magic numbers for UTF-8. These are the number of bytes that
     * <em>follow</em> a given lead byte. Trailing bytes have the value -1. The
     * values 4 and 5 are presented in this table, even though valid UTF-8
     * cannot include the five and six byte sequences.
     */
    static final int[] bytesFromUTF8 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // trail bytes
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5};

    /**
     * Returns the next code point at the current position in the buffer. The
     * buffer's position will be incremented. Any mark set on this buffer will
     * be changed by this method!
     *
     * @param bytes  ByteBuffer object
     * @return  the next code point at the current position in the buffer
     */
    public static int bytesToCodePoint(ByteBuffer bytes) {
        bytes.mark();
        byte b = bytes.get();
        bytes.reset();
        int extraBytesToRead = bytesFromUTF8[(b & 0xFF)];
        if (extraBytesToRead < 0)
            return -1; // trailing byte!
        int ch = 0;

        switch (extraBytesToRead) {
            case 5:
                ch += (bytes.get() & 0xFF);
                ch <<= 6; /* remember, illegal UTF-8 */
            case 4:
                ch += (bytes.get() & 0xFF);
                ch <<= 6; /* remember, illegal UTF-8 */
            case 3:
                ch += (bytes.get() & 0xFF);
                ch <<= 6;
            case 2:
                ch += (bytes.get() & 0xFF);
                ch <<= 6;
            case 1:
                ch += (bytes.get() & 0xFF);
                ch <<= 6;
            case 0:
                ch += (bytes.get() & 0xFF);
        }
        ch -= offsetsFromUTF8[extraBytesToRead];

        return ch;
    }

    static final int offsetsFromUTF8[] = {0x00000000, 0x00003080, 0x000E2080, 0x03C82080, 0xFA082080, 0x82082080};

    /**
     * For the given string, returns the number of UTF-8 bytes required to
     * encode the string.
     *
     * @param string text to encode
     * @return number of UTF-8 bytes required to encode
     */
    public static int utf8Length(String string) {
        CharacterIterator iter = new StringCharacterIterator(string);
        char ch = iter.first();
        int size = 0;
        while (ch != CharacterIterator.DONE) {
            if ((ch >= 0xD800) && (ch < 0xDC00)) {
                // surrogate pair?
                char trail = iter.next();
                if ((trail > 0xDBFF) && (trail < 0xE000)) {
                    // valid pair
                    size += 4;
                } else {
                    // invalid pair
                    size += 3;
                    iter.previous(); // rewind one
                }
            } else if (ch < 0x80) {
                size++;
            } else if (ch < 0x800) {
                size += 2;
            } else {
                // ch < 0x10000, that is, the largest char value
                size += 3;
            }
            ch = iter.next();
        }
        return size;
    }
}
