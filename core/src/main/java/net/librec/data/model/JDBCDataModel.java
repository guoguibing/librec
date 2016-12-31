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
package net.librec.data.model;

import com.google.common.collect.BiMap;
import net.librec.common.LibrecException;
import net.librec.math.structure.DataSet;

/**
 * JDBC Data Model
 */
public class JDBCDataModel extends AbstractDataModel {

    @Override
    protected void buildConvert() throws LibrecException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public BiMap<String, Integer> getUserMappingData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BiMap<String, Integer> getItemMappingData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataSet getDatetimeDataSet() {
        // TODO Auto-generated method stub
        return null;
    }

}
