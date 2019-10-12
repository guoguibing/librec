/**
 * 
 */
package research.core.data;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * Represents one row of the data.
 *
 * @author dayang
 * @param <Seq>
 *
 */
public class Row {

	/**
	 * The list into witch the elements of the row are stored.
	 */
	private List<Object> data;

	// TODO
	private List<Class> schema;

	/**
	 * Default constructor, construct an empty row.
	 */
	public Row() {
		data = new ArrayList<Object>();
	}

	/**
	 * Construct an empty row with the specified initial capacity.
	 *
	 * @param n
	 */
	public Row(int n) {
		data = new ArrayList<Object>();
	}

	/**
	 * Construct a row containing the elements of the specified values.
	 *
	 * @param values
	 */
	public Row(Object... values) {
		data = Arrays.asList(values);
	}
	
	public void add(Object object) {
		if(data == null) {
			data = new ArrayList<Object>();
		}
		data.add(object);
	}

	/**
	 * Return the value at position i as a primitive double
	 *
	 * @param i
	 * @return
	 */
	public double getDouble(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return Double.parseDouble(data.get(i).toString());
		}
	}

	/**
	 * Return the value at position i as a string object.
	 *
	 * @param i
	 * @return
	 */
	public String getString(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return data.get(i).toString();
		}
	}

	/**
	 * Number of elements in the row.
	 *
	 * @return
	 */
	public int length() {
		return data.size();
	}

	/**
	 * Number of elements in the row.
	 *
	 * @return
	 */
	public int size() {
		return data.size();
	}

	// TODO
	public String schema() {
		return "";
	}

	/**
	 * Check whether value at position i is null.
	 *
	 * @param i
	 * @return
	 */
	public boolean isNull(int i) {
		return data == null || data.get(i) == null;
	}

	/**
	 * Return a string representation of the row.
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; i < data.size() - 1; i++) {
			sb.append(data.get(i).toString()).append(", ");
		}
		if (data.size() >= 1) {
			sb.append(data.get(data.size() - 1));
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Return the hash code value for this row.
	 */
	@Override
	public int hashCode() {
		return data.hashCode();
	}

	/**
	 * Compare the specified object with this row for equality.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof Row)) {
			return false;
		}

		Row objectRow = (Row) obj;
		if (objectRow.length() != this.length()) {
			return false;
		}

		for (int i = 0; i < this.length(); i++) {
			if (!(this.getString(i) == null ? objectRow.getString(i) == null
					: this.getString(i).equals(objectRow.getString(i)))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the value at position i.
	 *
	 * @param i
	 * @return
	 */
	public Object apply(int i) {
		if (isNull(i)) {
			return null;
		} else {
			return data.get(i);
		}
	}

	/**
	 * Returns the value at position i as a primitive boolean.
	 *
	 * @param i
	 * @return
	 */
	public boolean getBoolean(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return Boolean.parseBoolean(data.get(i).toString());
		}
	}

	/**
	 * Returns the value at position i as a primitive byte.
	 *
	 * @param i
	 * @return
	 */
	public byte getByte(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return Byte.parseByte(data.get(i).toString());
		}
	}

	/**
	 * Returns the value at position i as a primitive short.
	 *
	 * @param i
	 * @return
	 */
	public short getShort(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return Short.parseShort(data.get(i).toString());
		}
	}

	/**
	 * Returns the value at position i as a primitive int.
	 *
	 * @param i
	 * @return
	 */
	public int getInt(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return Integer.parseInt(data.get(i).toString());
		}
	}

	/**
	 * Returns the value at position i as a primitive float.
	 *
	 * @param i
	 * @return
	 */
	public float getFloat(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return Float.parseFloat(data.get(i).toString());
		}
	}

	/**
	 * Returns the value at position i as a primitive long.
	 *
	 * @param i
	 * @return
	 */
	public long getLong(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return Long.parseLong(data.get(i).toString());
		}
	}

	/**
	 * Returns the value at position i of decimal type as java.math.BigDecimal.
	 *
	 * @param i
	 * @return
	 */
	public BigDecimal getDecimal(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return new BigDecimal(data.get(i).toString());
		}
	}

	/**
	 * Returns the value at position i of date type as java.sql.Timestamp.
	 *
	 * @param i
	 * @return
	 */
	public Timestamp getTimestamp(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			Timestamp ts = null;
			ts = Timestamp.valueOf(data.get(i).toString());
			return ts;
		}
	}

	/**
	 * Returns the value at position i.
	 *
	 */
	public Object getAnyValAs(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return data.get(i);
		}
	}

	/**
	 * Returns the value at position i.
	 *
	 */
	public Object getAs(int i) {
		if (isNull(i)) {
			return 0;
		} else {
			return data.get(i);
		}
	}

	/**
	 * Returns the value at position i of date type as java.sql.Date.
	 *
	 * @param i
	 * @return
	 */
	public Date getDate(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			Date date = null;
			date = Date.valueOf(data.get(i).toString());
			return date;
		}
	}

	/**
	 * Make a copy of the current Row object.
	 * 
	 * @return
	 */
	public void copy() {
		try {
			super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the value at position i of array type as java.util.List.
	 *
	 * @param i
	 * @return
	 */
	public List<Object> getList(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			List<Object> list = Arrays.asList(data.get(i).toString());
			return list;
		}
	}

	/**
	 * Returns the value at position i of array type as a java.util.Map.
	 *
	 * @param i
	 * @return
	 */
	public Map getJavaMap(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			Map list = (Map) Arrays.asList(data.get(i).toString());
			return list;
		}
	}

	/**
	 * Returns the value at position i of struct type as a Row object.
	 *
	 * @param i
	 * @return
	 */
	public Row getStruct(int i) {
		if (isNull(i)) {
			throw new NullPointerException(String.format("Value at position %s is null.", i));
		} else {
			return Row.class.cast(data.get(i));
		}
	}

	/**
	 * Displays all elements of this sequence in a string (without a separator).
	 * 
	 * @param i
	 * @return
	 */
	public String mkString() {
		String str = null;
		for (int i = 0; i < data.size(); i++) {
			str += data.get(i).toString();
		}
		return str;
	}

	public String mkString(String sep) {
		String str = null;
		for (int i = 0; i < data.size(); i++) {
			str += (data.get(i).toString() + sep);
		}
		return str;
	}

	// public String mkString(String start,
	// String sep,
	// String end){
	//
	// return null;
	// }

	/**
	 * Returns the index of a given field name.
	 * 
	 * @param name
	 * @return
	 */
	public int fieldIndex(String name) {
		int i;
		boolean n = false;
		for (i = 0; i < data.size(); i++) {
			if (data.get(i).equals(name)) {
				n = true;
				break;
			}
		}
		if (n) {
			return i;
		} else
			throw new IllegalArgumentException(String.format("name at position %s is null.", name));
	}

	/**
	 * Returns the value at position i.
	 * 
	 * @param i
	 */
	public Object get(int i) {
		return data.get(i);
	}

	/**
	 * Returns the value at position i of map type as a Scala Map.
	 *
	 * @param i
	 * @return
	 */
	// public scala.collection.Map getMap(int i) {
	// if(isNull(i)) {
	// throw new NullPointerException(String.format("Value at position %s is null.",
	// i));
	// }else {
	// scala.collection.Map<Object> list= Arrays.asList(data.get(i));
	// return list;
	// }
	// }

	/**
	 * Returns the value at position i of array type as a Scala Seq.
	 *
	 * @param i
	 * @return
	 */
	// public scala.collection.Seq getSeq(int i) {
	// if(isNull(i)) {
	// throw new NullPointerException(String.format("Value at position %s is null.",
	// i));
	// }else {
	// scala.collection.Seq<Object> list= Arrays.asList(data.get(i));
	// return list;
	// }
	// }
	//
	/**
	 * Return a Scala Seq representing the row.
	 * 
	 * @param i
	 * @return
	 */
	// public scala.collection.Seq <Object> toSeq(int i){
	//
	// return null;
	// }
	//
	/**
	 * Returns a Map consisting of names and values for the requested fieldNames For
	 * primitive types if value is null it returns 'zero value' specific for
	 * primitive ie.
	 *
	 */
	// public scala.collection.immutable.Map <String,T>
	// getValuesMap(scala.collection.Seq <String> fieldNames){
	//
	// return;
	// }
}