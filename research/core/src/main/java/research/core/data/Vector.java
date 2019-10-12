package research.core.data;
import java.util.*;
/**
 * Vector
 *
 * @author dayang
 *
 */

public class Vector{

    private java.util.Vector<Object> data;

    /**
     * Default constructor, construct an empty row.
     */
    public Vector() {
        data=new java.util.Vector<Object>();
    }
    /**
     * Construct an empty row with the specified initial capacity.
     * @param n
     */
    public Vector(int initialCapacity) {
        data=new java.util.Vector<Object>(initialCapacity, 0);
    }

    public Vector(int initialCapacity, int capacityIncrement) {
        data=new java.util.Vector<>(initialCapacity, capacityIncrement);
    }

    /**
     * Gets the value of the ith element.
     */
    public Object apply(int n) {
        if(data == null || data.get(n) == null) {
            return null;
        }else {
            return data.get(n);
        }
    }
    /**
     * Compare the specified object with this vector for equality.
     */
    public boolean equals(Object obj){
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Vector)) {
            return false;
        }

        Vector object = (Vector) obj;
        if (object.size()!= this.size()) {
            return false;
        }

        for (int i = 0; i < this.size(); i++) {
            if (!(this.getString(i) == null ? object.getString(i) == null
                    : this.getString(i).equals(object.getString(i)))) {
                return false;
            }
        }

        return true;
    }
    /**
     * Return the value at position i as a string object.
     */
    private Object getString(int i) {
        if(apply(i)!=null) {
            return apply(i).toString();
        }else {
            return null;
        }
    }
    /**
     * Size of the vector
     */
    public int size() {
        return data.size();
    }
    /**
     * Converts the instance to a double array.
     */
    public double [] toArray(){
        String []str=(String[]) data.toArray();
        double[] doubleArray = new double[str.length];
        for(int i=0;i<doubleArray.length;i++){
            doubleArray[i] = Double.parseDouble(str[i]);
        }
        return doubleArray;
    }
    /**
     * Returns a hash code value for the vector.
     */
    public int hashCode() {
        return data.hashCode();
    }
    /**
     * Makes a deep copy of this vector.
     */
//    public Vector copy() {
//
//    }
    /**
     * Converts this vector to a dense vector.
     */
//    public static  Vector  toDense(){
//
//    }
    /**
     * Converts this vector to a sparse vector with all explicit zeros removed.
     */
//    public  Vector  toSparse() {
//
//    }
    /**
     * Returns a vector in either dense or sparse format, whichever uses less storage.
     */
//    public static  Vector  compressed() {
//
//    }
}