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
package net.librec.data.convertor.appender;

import com.google.common.collect.BiMap;
import net.librec.data.DataAppender;

import java.io.IOException;

/**
 * A <tt>DocumentDataAppender</tt> is a class to process and store
 * document appender data.
 *
 * @author WangYuFeng
 */
public class DocumentDataAppender implements DataAppender {

    /**
     * Process appender data.
     *
     * @throws IOException
     *         if the path is not valid
     */
    @Override
    public void processData() throws IOException {
        // TODO Auto-generated method stub

    }

    /**
     * Set user mapping data.
     *
     * @param userMappingData  user {raw id, inner id} map
     */
    @Override
    public void setUserMappingData(BiMap<String, Integer> userMappingData) {
        // TODO Auto-generated method stub

    }

    /**
     * Set item mapping data.
     *
     * @param itemMappingData   item {raw id, inner id} map
     */
    @Override
    public void setItemMappingData(BiMap<String, Integer> itemMappingData) {
        // TODO Auto-generated method stub

    }

}
