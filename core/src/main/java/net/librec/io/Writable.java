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
 * Writability of a class is enabled by the class implementing this interface.
 *
 * @author WangYuFeng
 */
public interface Writable {
    /**
     * Serialize the fields of this object to <i>out</i>.
     *
     * @param out <code>DataOuput</code> to serialize this object into.
     * @throws IOException
     *         if IOException happens during writing
     */
    void write(DataOutput out) throws IOException;

    /**
     * Deserialize the fields of this object from <i>in</i>.
     * <p>
     * For efficiency, implementations should attempt to re-use storage in the
     * existing object where possible.
     *
     * @param in <code>DataInput</code> to deserialize this object from.
     * @throws IOException
     *         if IOException happens during reading
     */
    void readFields(DataInput in) throws IOException;

    /**
     * Return the value of this object.
     *
     * @return the value of this object.
     */
    Object getValue();

    /**
     * Set the value of this object.
     *
     * @param value  the value to be set to this object.
     */
    void setValue(Object value);

}
