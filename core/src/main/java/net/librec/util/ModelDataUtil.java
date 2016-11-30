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
package net.librec.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import net.librec.annotation.ModelData;
import net.librec.recommender.Recommender;

/**
 * Model Data Util
 * 
 * @author YuFeng Wang
 */
public class ModelDataUtil {

//	/**
//	 * load Recommender Model
//	 * 
//	 * @param recommender
//	 * @param filePath
//	 */
//	public static void loadRecommenderModel(Recommender recommender, String filePath) {
//		ModelData modelData = recommender.getClass().getAnnotation(ModelData.class);
//		String[] fieldNames = modelData.value();
//		try {
//			ModelFile.Reader writer = new ModelFile.Reader();
//			FileInputStream fis = new FileInputStream(filePath);
//			DataInputStream in = new DataInputStream(fis);
//			for (String fieldName : fieldNames) {
//				Field field = recommender.getClass().getDeclaredField(fieldName);
//				field.setAccessible(true);
//				Writable writable = writer.readData(in);
//				if (writable != null) {
//					field.set(recommender, writable.getValue());
//				}
//			}
//			in.close();
//			fis.close();
//		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 * save Recommender Model
//	 * 
//	 * @param recommender
//	 * @param filePath
//	 */
//	public static void saveRecommenderModel(Recommender recommender, String filePath) {
//		ModelData modelData = recommender.getClass().getAnnotation(ModelData.class);
//		String[] fieldNames = modelData.value();
//		try {
//			ModelFile.Writer writer = new ModelFile.Writer();
//			FileOutputStream fos = new FileOutputStream(filePath);
//			DataOutputStream out = new DataOutputStream(fos);
//			for (String fieldName : fieldNames) {
//				Field field = recommender.getClass().getDeclaredField(fieldName);
//				field.setAccessible(true);
//				Object fieldValue = field.get(recommender);
//				if (fieldValue != null) {
//					WritableEnum writableEnum = WritableEnum.getWritableEnum(fieldValue);
//					Writable writable = (Writable) ReflectionUtil.newInstance(writableEnum.getClazz());
//					writable.setValue(fieldValue);
//					writer.writeData(out, writable);
//				}
//			}
//			out.close();
//			fos.close();
//		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
