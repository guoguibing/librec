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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractMapWritable implements Writable {

    /** Class to id mappings */
    Map<Class, Byte> classToIdMap = new ConcurrentHashMap<Class, Byte>();

    /** Id to Class mappings */
    Map<Byte, Class> idToClassMap = new ConcurrentHashMap<Byte, Class>();

    /** The number of new classes (those not established by the constructor) */
    private volatile byte newClasses = 0;

    byte getNewClasses() {
        return newClasses;
    }

    /**
     * Used to add "predefined" classes and by Writable to copy "new" classes.
     */
    private synchronized void addToMap(Class clazz, byte id) {
        if (classToIdMap.containsKey(clazz)) {
            byte b = classToIdMap.get(clazz);
            if (b != id) {
                throw new IllegalArgumentException(
                        "Class " + clazz.getName() + " already registered but maps to " + b + " and not " + id);
            }
        }
        if (idToClassMap.containsKey(id)) {
            Class c = idToClassMap.get(id);
            if (!c.equals(clazz)) {
                throw new IllegalArgumentException(
                        "Id " + id + " exists but maps to " + c.getName() + " and not " + clazz.getName());
            }
        }
        classToIdMap.put(clazz, id);
        idToClassMap.put(id, clazz);
    }

    /**
     * Add a Class to the maps if it is not already present.
     *
     * @param clazz  the Class to be added
     */
    protected synchronized void addToMap(Class clazz) {
        if (classToIdMap.containsKey(clazz)) {
            return;
        }
        if (newClasses + 1 > Byte.MAX_VALUE) {
            throw new IndexOutOfBoundsException(
                    "adding an additional class would" + " exceed the maximum number allowed");
        }
        byte id = ++newClasses;
        addToMap(clazz, id);
    }

    /**
     * Return the Class class for the specified id.
     *
     * @param id  the specified id of Class
     * @return the Class class for the specified id
     */
    protected Class getClass(byte id) {
        return idToClassMap.get(id);
    }

    /**
     * Return the id for the specified Class.
     *
     * @param  clazz  the Class object
     * @return the id for the specified Class
     */
    protected byte getId(Class clazz) {
        return classToIdMap.containsKey(clazz) ? classToIdMap.get(clazz) : -1;
    }

    /**
     * Used by child copy constructors.
     *
     * @param other  another writable object
     */
    protected synchronized void copy(Writable other) {
        if (other != null) {
            try {
                DataOutputBuffer out = new DataOutputBuffer();
                other.write(out);
                DataInputBuffer in = new DataInputBuffer();
                in.reset(out.getData(), out.getLength());
                readFields(in);

            } catch (IOException e) {
                throw new IllegalArgumentException("map cannot be copied: " + e.getMessage());
            }

        } else {
            throw new IllegalArgumentException("source map cannot be null");
        }
    }

    /**
     * Constructor.
     */
    protected AbstractMapWritable() {

        addToMap(BooleanWritable.class, Byte.valueOf(Integer.valueOf(-126).byteValue()));
        addToMap(DatetimeWritable.class, Byte.valueOf(Integer.valueOf(-124).byteValue()));
        addToMap(DoubleWritable.class, Byte.valueOf(Integer.valueOf(-123).byteValue()));
        addToMap(IntWritable.class, Byte.valueOf(Integer.valueOf(-122).byteValue()));
        addToMap(LongWritable.class, Byte.valueOf(Integer.valueOf(-121).byteValue()));
        addToMap(NullWritable.class, Byte.valueOf(Integer.valueOf(-119).byteValue()));
        addToMap(Text.class, Byte.valueOf(Integer.valueOf(-117).byteValue()));
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#write(java.io.DataOutput)
     */
    @Override
    public void write(DataOutput out) throws IOException {

        // First write out the size of the class table and any classes that are
        // "unknown" classes

        out.writeByte(newClasses);

        for (byte i = 1; i <= newClasses; i++) {
            out.writeByte(i);
            out.writeUTF(getClass(i).getName());
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#readFields(java.io.DataInput)
     */
    @Override
    public void readFields(DataInput in) throws IOException {

        // Get the number of "unknown" classes

        newClasses = in.readByte();

        // Then read in the class names and add them to our tables

        for (int i = 0; i < newClasses; i++) {
            byte id = in.readByte();
            String className = in.readUTF();
            try {
                addToMap(Class.forName(className), id);

            } catch (ClassNotFoundException e) {
                throw new IOException("can't find class: " + className + " because " + e.getMessage());
            }
        }
    }
}
