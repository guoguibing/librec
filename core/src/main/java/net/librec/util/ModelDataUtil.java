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
package net.librec.util;

import net.librec.annotation.ModelData;
import net.librec.io.ModelFile;
import net.librec.io.Writable;
import net.librec.io.WritableEnum;
import net.librec.recommender.Recommender;

import java.io.*;
import java.lang.reflect.Field;

/**
 * Model Data Util
 *
 * @author YuFeng Wang
 */
public class ModelDataUtil {

    /**
     * load Recommender Model
     *
     * @param recommender  recommender
     * @param filePath     file path
     */
    public static void loadRecommenderModel(Recommender recommender, String filePath) {
        ModelData modelData = recommender.getClass().getAnnotation(ModelData.class);
        String[] fieldNames = modelData.value();
        try {
            FileInputStream fis = new FileInputStream(filePath);
            DataInputStream in = new DataInputStream(fis);
            ModelFile.Reader reader = new ModelFile.Reader(in);
            for (String fieldName : fieldNames) {
                Field field = getDeclaredField(recommender, fieldName);
                field.setAccessible(true);
                Writable writable = reader.readData(in);
                if (writable != null) {
                    field.set(recommender, writable.getValue());
                }
            }
            in.close();
            fis.close();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * save Recommender Model
     *
     * @param recommender  recommender
     * @param filePath     file path
     */
    public static void saveRecommenderModel(Recommender recommender, String filePath) {
        ModelData modelData = recommender.getClass().getAnnotation(ModelData.class);
        String[] fieldNames = modelData.value();
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            DataOutputStream out = new DataOutputStream(fos);
            ModelFile.Writer writer = new ModelFile.Writer(out);
            for (String fieldName : fieldNames) {
                Field field = getDeclaredField(recommender, fieldName);
                field.setAccessible(true);
                Object fieldValue = field.get(recommender);
                if (fieldValue != null) {
                    WritableEnum writableEnum = WritableEnum.getWritableEnum(fieldValue);
                    Writable writable = (Writable) ReflectionUtil.newInstance(writableEnum.getClazz());
                    writable.setValue(fieldValue);
                    writer.writeData(out, writable);
                }
            }
            out.close();
            fos.close();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * get Declared Field from self class or super class.
     *
     * @param object     a given object
     * @param fieldName  field name
     * @return  Field object
     */
    public static Field getDeclaredField(Object object, String fieldName) {
        Field field = null;
        Class<?> clazz = object.getClass();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                return field;
            } catch (NoSuchFieldException e) {

            }
        }
        return null;
    }
}
