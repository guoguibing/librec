package research.data.reader;

//import net.librec.common.LibrecException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A <tt>ArffInstance</tt> represents an instance
 * of ARFF format input.
 * */

public class ArffInstance{
    /**
     * Attributes of the instance
     */
    public static ArrayList<ArffAttribute> attrs;

    /**
     * Data of the instance
     */
    private ArrayList<String> instanceData;

    /**
     * Initializes a newly created {@code ArffInstance} object
     * with instance data.
     *
     * @param instanceData data of the instance
     */
    public ArffInstance(ArrayList<String> instanceData) {
        this.instanceData = instanceData;
    }

//    /**
//     * Get data value by the attribute name.
//     *
//     * @param attrName name of the attribute
//     * @return data value
//     * @throws LibrecException if attrName is invalid
//     */
//    public Object getValueByAttrName(String attrName) throws LibrecException {
//        Object res = null;
//        boolean isNameValid = false;
//        for (ArffAttribute attr : attrs) {
//            if (attrName.equals(attr.getName())) {
//                res = getValueByIndex(attr.getIndex());
//                isNameValid = true;
//                break;
//            }
//        }
//        if (isNameValid == false)
//            throw new LibrecException("invalid attrName: " + attrName);
//        return res;
//    }
//
//    /**
//     * Get data value by index.
//     *
//     * @param idx index of the data.
//     * @return data value
//     */
//    public Object getValueByIndex(int idx) {
//        Object res = new Object();
//        switch (getTypeByIndex(idx).toUpperCase()) {
//            case "NUMERIC":
//            case "REAL":
//            case "INTEGER":
//                res = Double.parseDouble(instanceData.get(idx));
//                break;
//            case "STRING":
//                res = instanceData.get(idx);
//                break;
//            case "NOMINAL":
//                String data[] = instanceData.get(idx).split(",");
//                res = new ArrayList<>(Arrays.asList(data));
//                break;
//        }
//        return res;
//    }
//
//    /**
//     * Get attribute type by index.
//     *
//     * @param idx index of the attribute
//     * @return attribute type
//     */
//    public String getTypeByIndex(int idx) {
//        ArffAttribute attr = attrs.get(idx);
//        return attr.getType();
//    }
}

