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
package net.librec.data;

import com.google.common.collect.BiMap;

import java.io.IOException;

/**
 * A <tt>DataAppender</tt> is an interface to process and store
 * appender data.
 *
 * @author WangYuFeng
 */
public interface DataAppender {
    /**
     * Process appender data.
     *
     * @throws IOException if I/O error occurs
     */
    public void processData() throws IOException;

    /**
     * Set user mapping data.
     *
     * @param userMappingData
     *              user {raw id, inner id} map
     *
     */
    public void setUserMappingData(BiMap<String, Integer> userMappingData);

    /**
     * Set item mapping data.
     *
     * @param itemMappingData
     *              item {raw id, inner id} map
     */
    public void setItemMappingData(BiMap<String, Integer> itemMappingData);
}
