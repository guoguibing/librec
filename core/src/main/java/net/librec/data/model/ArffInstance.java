/**
 * Copyright (C) 2016 LibRec
 *
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.data.model;

import java.util.ArrayList;
import java.util.Arrays;

public class ArffInstance {
    public static ArrayList<ArffAttribute> attrs;
    private ArrayList<String> instanceData;

    public ArffInstance(ArrayList<String> instanceData) {
        this.instanceData = instanceData;
    }

    public Object getValueByAttrName(String attrName) throws Exception{
        Object res = null;
        boolean isNameValid = false;
        for (ArffAttribute attr: attrs) {
            if (attrName.equals(attr.getName())) {
                res = getValueByIndex(attr.getIndex());
                isNameValid = true;
                break;
            }
        }
        if (isNameValid == false)
            throw new Exception("invalid attrName: " + attrName);
        return res;
    }

    public Object getValueByIndex(int idx) {
        Object res = new Object();
        switch (getTypeByIndex(idx).toUpperCase()) {
            case "NUMERIC":case "REAL":case "INTEGER":
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

    public String getTypeByIndex(int idx) {
        ArffAttribute attr = attrs.get(idx);
        return attr.getType();
    }
}
