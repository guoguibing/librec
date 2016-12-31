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
package net.librec.recommender.item;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.BaseTestCase;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import java.util.Iterator;

/**
 * 
 * 
 */
public class RecommendedItemListTestCase extends BaseTestCase{

	private RecommendedItemList recommendedItemList;

	public void setUp() throws Exception {
		recommendedItemList = new RecommendedItemList(10000);
	}

	@Test
	public void test() {
		testSparseMatrixAndRecommender();

		//System.exit(0);
//		RecommendedItemList x = new RecommendedItemList();
//		ArrayList x = new ArrayList<>();
		long time = System.currentTimeMillis();
		DenseMatrix x = new DenseMatrix(10000, 10000);
//		double[][] data = new double[10000][10000];
//		System.out.println(time);
		int k = 0;
		for (int i = 0; i < 10000; i++) {
//			ArrayList<ItemEntry<Integer, Double>> y = new ArrayList<>();
			for (int j = 0; j < 10000; j++) {
//				ItemEntry<Integer, Double> itemEntry = new ItemEntry<Integer, Double>(j, RandomUtils.nextDouble());
				if (k%10000 == 0) {
					System.out.println(k);
				}
				x.set(i, j, RandomUtils.nextDouble());
//				recommendedItemList.addUserItemIdx(i,j,RandomUtils.nextDouble());
				k++;
			}
//			x.addItemIdxList(i, y);
		}
		System.out.println(System.currentTimeMillis()-time);
	}

	public static void testSparseMatrixAndRecommender() {
		Table<Integer, Integer, Double> table = HashBasedTable.create();
		table.put(1,2,3.0);
		table.put(3,4,3.0);
		table.put(5,2,3.0);
		table.put(4,6,3.0);
		SparseMatrix testMatrix = new SparseMatrix(6,7,table);
		for(MatrixEntry matrixEntry:testMatrix){
			System.out.println(matrixEntry.row()+" "+matrixEntry.column()+" "+matrixEntry.get());
		}
		System.out.println();

		int numUsers = 6;
		RecommendedItemList recommendedList = new RecommendedItemList(numUsers - 1, numUsers);

		for (MatrixEntry matrixEntry : testMatrix) {
			int userIdx = matrixEntry.row();
			int itemIdx = matrixEntry.column();
			double predictRating = 3.0;
			if (Double.isNaN(predictRating)) {
				predictRating = 2.0;
			}
			recommendedList.addUserItemIdx(userIdx, itemIdx, predictRating);
		}

		Iterator<MatrixEntry> testMatrixIter = testMatrix.iterator();
		Iterator<UserItemRatingEntry> recommendedEntryIter = recommendedList.entryIterator();

		while (testMatrixIter.hasNext()) {

			if (recommendedEntryIter.hasNext()) {

				MatrixEntry testMatrixEntry = testMatrixIter.next();
				UserItemRatingEntry userItemRatingEntry = recommendedEntryIter.next();

				System.out.println(testMatrixEntry.row()+" "+testMatrixEntry.column()+" "+testMatrixEntry.get());
				System.out.println(userItemRatingEntry.getUserIdx()+" "+userItemRatingEntry.getItemIdx()+" "+userItemRatingEntry.getValue());

				if (testMatrixEntry.row() == userItemRatingEntry.getUserIdx()
						&& testMatrixEntry.column() == userItemRatingEntry.getItemIdx()) {
				} else {
					throw new IndexOutOfBoundsException("index of recommendedList does not equal testMatrix index");
				}

			} else {
				throw new IndexOutOfBoundsException("index size of recommendedList does not equal testMatrix index size");
			}
		}
	}
}
