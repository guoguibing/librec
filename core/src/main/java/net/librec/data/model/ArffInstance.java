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

import net.librec.common.LibrecException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A <tt>ArffInstance</tt> represents an instance
 * of ARFF format input.
 *
 * @author WangYuFeng TangJiaxi and Ma Chen
 */
public class ArffInstance {
    /** Attributes of the instance */
    public static ArrayList<ArffAttribute> attrs;

    /** Data of the instance */
    private ArrayList<String> instanceData;

    /**
     * Initializes a newly created {@code ArffInstance} object
     * with instance data.
     *
     * @param instanceData
     *              data of the instance
     */
    public ArffInstance(ArrayList<String> instanceData) {
        this.instanceData = instanceData;
    }

    /**
     * Get data value by the attribute name.
     *
     * @param attrName
     *              name of the attribute
     *
     * @return  data value
     * @throws LibrecException if attrName is invalid
     */
    public Object getValueByAttrName(String attrName) throws LibrecException {
        Object res = null;
        boolean isNameValid = false;
        for (ArffAttribute attr : attrs) {
            if (attrName.equals(attr.getName())) {
                res = getValueByIndex(attr.getIndex());
                isNameValid = true;
                break;
            }
        }
        if (isNameValid == false)
            throw new LibrecException("invalid attrName: " + attrName);
        return res;
    }

    /**
     * Get data value by index.
     *
     * @param idx
     *          index of the data.
     *
     * @return  data value
     */
    public Object getValueByIndex(int idx) {
        Object res = new Object();
        switch (getTypeByIndex(idx).toUpperCase()) {
            case "NUMERIC":
            case "REAL":
            case "INTEGER":
                res = Double.parseDouble(instanceData.get(idx));
                break;
            case "STRING":
                res = instanceData.get(idx);
                break;
            case "NOMINAL":
                String data[] = instanceData.get(idx).split(",");
                res = new ArrayList<>(Arrays.asList(data));
                break;
        }
        return res;
    }

    /**
     * Get attribute type by index.
     *
     * @param idx
     *          index of the attribute
     * @return  attribute type
     */
    public String getTypeByIndex(int idx) {
        ArffAttribute attr = attrs.get(idx);
        return attr.getType();
    }
}
