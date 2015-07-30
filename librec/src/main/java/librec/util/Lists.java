// Copyright (C) 2014-2015 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import librec.util.FileIO.Converter;

/**
 * This class is for the operations of arrays or collections
 * 
 * @author Felix
 * 
 */
public class Lists {

	/**
	 * @return the proper initial size for a target given capacity, based on the default "load factor=0.7"
	 */
	public static int initSize(int capacity) {
		return (int) (Math.ceil(capacity / 0.7));
	}

	public static <E> int initSize(Collection<E> collection) {
		return initSize(collection.size());
	}

	/**
	 * Rearrange the elements of a double array in random order.
	 */
	public static void shaffle(int[] data) {
		int N = data.length;

		if (N <= 1)
			return;
		for (int i = 0; i < N; i++) {
			int j = Randoms.uniform(i, N);

			int swap = data[i];
			data[i] = data[j];
			data[j] = swap;
		}
	}

	public static void shaffle(double[] data) {
		int N = data.length;

		if (N <= 1)
			return;
		for (int i = 0; i < N; i++) {
			int j = Randoms.uniform(i, N);

			double swap = data[i];
			data[i] = data[j];
			data[j] = swap;
		}
	}

	public static <T> void shaffle(List<T> data) {
		int N = data.size();

		if (N <= 1)
			return;
		for (int i = 0; i < N; i++) {
			int j = Randoms.uniform(i, N);

			T swap = data.get(i);
			data.set(i, data.get(j));
			data.set(j, swap);
		}
	}

	/**
	 * @return the top-n subset of list {@code data}
	 */
	public static <T> List<T> subset(List<T> data, int n) {
		List<T> ts = new ArrayList<>();

		for (int i = 0; i < data.size(); i++) {
			ts.add(data.get(i));

			if (ts.size() >= n)
				break;
		}

		return ts;
	}

	/**
	 * @return a new list of the intersection of two lists: list1 and list2
	 */
	public static <T> List<T> intersect(List<T> list1, List<T> list2) {
		List<T> ts = new ArrayList<>();

		for (T t : list1) {
			if (list2.contains(t))
				ts.add(t);
		}

		return ts;
	}

	/**
	 * @return the number of common items of two lists: list1 and list2
	 */
	public static <T> int overlapSize(List<T> list1, List<T> list2) {
		int res = 0;

		for (T t : list1) {
			if (list2.contains(t))
				res++;
		}

		return res;
	}

	/**
	 * Note: if you need to operate on the original list, it's better to use the method "retainAll" or "removeAll"
	 * 
	 * @return a new list with the exception of two lists: list1 and list2
	 */
	public static <T> List<T> except(List<T> list1, List<T> list2) {
		List<T> ts = new ArrayList<>();

		for (T t : list1) {
			if (!list2.contains(t))
				ts.add(t);
		}

		return ts;
	}

	/**
	 * @return the number of elements in the first list but not in the second list
	 */
	public static <T> int exceptSize(List<T> list1, List<T> list2) {
		int res = 0;

		for (T t : list1) {
			if (!list2.contains(t))
				res++;
		}

		return res;
	}

	/**
	 * @return whether list is empty: null or no elements insides
	 */
	public static <T> boolean isEmpty(List<T> ts) {
		if (ts == null || ts.size() < 1)
			return true;

		return false;
	}

	/**
	 * Turn a collection of data into an double array
	 * 
	 * @param data
	 *            a collection of data
	 * @return an double array
	 */
	public static double[] toArray(Collection<? extends Number> data) {
		if (data == null || data.size() < 1)
			return null;
		double da[] = new double[data.size()];
		int i = 0;
		for (Number d : data)
			da[i++] = d.doubleValue();

		return da;
	}

	/**
	 * Turn an double array into a List<Double> object
	 * 
	 * @param data
	 *            an double array
	 * @return a List<Double> object
	 */
	public static List<Double> toList(double[] data) {
		if (data == null || data.length < 1)
			return null;
		List<Double> da = new ArrayList<>();

		for (double d : data)
			da.add(d);

		return da;
	}

	public static <K, T> List<T> toList(K[] data, Converter<K, T> c) throws Exception {
		if (data == null || data.length < 1)
			return null;

		List<T> da = new ArrayList<>();

		for (K d : data)
			da.add(c.transform(d));

		return da;
	}

	/**
	 * Convert int array to int list
	 */
	public static List<Integer> toList(int[] data) {

		List<Integer> da = new ArrayList<>();

		for (Integer d : data)
			da.add(d);

		return da;
	}

	/**
	 * sort an {@code Map<K, V extends Comparable<? extends V>} map object
	 * 
	 * <p>
	 * <strong>Remark: </strong> note that this method may be memory-consuming as it needs to make an ArrayList copy of
	 * input Map data. Instead, we suggest to store original data in List<Map.Entry<K,V>> and use sortList() method to
	 * avoid object copying.
	 * </p>
	 * 
	 * @param data
	 *            map data
	 * @param inverse
	 *            descending if true; otherwise ascending
	 * @return a sorted list
	 * 
	 */
	public static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> sortMap(Map<K, V> data,
			final boolean inverse) {

		// According to tests, LinkedList is slower than ArrayList
		List<Map.Entry<K, V>> pairs = new ArrayList<>(data.entrySet());

		sortList(pairs, inverse);

		return pairs;
	}

	/**
	 * sort a map object: {@code Map<K, V extends Comparable<? extends V>}
	 * 
	 * @param data
	 *            map data
	 * @return an ascending sorted list
	 */
	public static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> sortMap(Map<K, V> data) {
		return sortMap(data, false);
	}

	/**
	 * sort a list of objects: {@code List<Map.Entry<K, V extends Comparable<? extends V>>}
	 * 
	 * @param data
	 *            map data
	 * @param inverse
	 *            descending if true; otherwise ascending
	 * @return a sorted list
	 */
	public static <K, V extends Comparable<? super V>> void sortList(List<Map.Entry<K, V>> data, final boolean inverse) {

		Collections.sort(data, new Comparator<Map.Entry<K, V>>() {

			@Override
			public int compare(Entry<K, V> a, Entry<K, V> b) {

				int res = (a.getValue()).compareTo(b.getValue());

				return inverse ? -res : res;
			}

		});

	}

	/**
	 * sort a map object: {@code List<Map.Entry<K, V extends Comparable<? extends V>>}
	 * 
	 * @param data
	 *            map data
	 * @return an ascending sorted list
	 */
	public static <K, V extends Comparable<? super V>> void sortList(List<Map.Entry<K, V>> data) {

		sortList(data, false);
	}

}
