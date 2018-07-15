package net.librec.util;

public class ArrayUtils {
    public static int[][] copy(int[][] values){
        int[][] results = new int[values.length][];
        for(int index=0; index < values.length; index++){
            results[index] = values[index].clone();
        }
        return results;
    }
}
