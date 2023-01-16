package Exp;

import java.io.Serializable;

public class ArraysManagement implements Serializable {
    public ArraysManagement() {
    }

    public void ConstantThis(int[][] array, int constant) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                array[i][j] = constant;
            }
        }
    }

    public void Print(float[][] array) {
        System.out.print("[ ");
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = 0; j < array[0].length - 1; j++) {
                System.out.print(array[i][j]);
                System.out.print(" , ");
            }
            System.out.println(array[i][array[0].length - 1] + " ;");
        }
        for (int j = 0; j < array[0].length - 1; j++) {
            System.out.print(array[array.length - 1][j]);
            System.out.print(" , ");
        }
        System.out.println(array[array.length - 1][array[0].length - 1] + " ]");
    }

    public int getIndexOfLargest(float[] array) {
        if (array == null || array.length == 0) return -1; // null or empty

        int largest = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[largest]) largest = i;
        }
        return largest; // position of the first largest found
    }

    public void Print(float[] array) {
        System.out.print("[ ");
        for (int i = 0; i < array.length - 1; i++) {
            System.out.println(array[i] + " ;");
        }
        System.out.println(array[array.length - 1] + " ]");
    }

    public float[] Multiply(float[][] matrix, float[] input) {
        float[] output = new float[matrix.length];
        float sum;
        for (int row = 0; row < matrix.length; row++) {
            sum = 0;
            for (int col = 0; col < matrix[0].length; col++) {
                sum += matrix[row][col] * input[col];
            }
            output[row] = sum;
        }
        return output;
    }

    public float[] Subtract(float[] first, float[] second) {
        float[] output = new float[first.length];
        if (first.length == second.length) {

            for (int i = 0; i < first.length; i++) {
                output[i] = first[i] - second[i];
            }
        } else {
            System.out.print("Vectores no tienen las mismas dimensiones");
            System.exit(-2);
        }
        return output;
    }

    public float[] Sigmoid(float[] input) {
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (float) (2 / (1 + Math.exp(-input[i])) - 1);
        }
        return output;
    }

    public float Norm(float[] vector) {
        float result = 0;
        for (float v : vector) {
            result += v * v;
        }
        return result;
    }
}
