package Exp;

import java.io.Serializable;
/*
Esta clase se encarga de trabajar con vectores de 1 y 2 dimensiones
 */
public class ArraysManagement implements Serializable {
    public ArraysManagement() {
        /*
        Esta clase esta vacia
         */
    }

    public void ConstantThis(int[][] array, int constant) {
        /*
        convierte los valores de un vector de dos dimenciones en una constante
         */
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                array[i][j] = constant;
            }
        }
    }

    public void Print(float[][] array) {
        /*
        Escribe en la consola los valores de un vector de dos dimensiones
         */
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

    public int getIndexOfLargest(float[] array,int upTo) {
        /*
        Entrega el valor de la entrada más grande de un vector de una dimensión hasta
        upTo
         */
        if (array == null || array.length == 0) return -1; //Si está vacia o de largo 0 se entrega un -1
        int largest = 0;
        for (int i = 1; i < Math.min(array.length,upTo); i++) {
            if (array[i] > array[largest]) largest = i;
        }
        return largest;
    }

    public void Print(float[] array) {
        /*
        Escribe en la consola los valores de un vector de una dimensión
         */
        System.out.print("[ ");
        for (int i = 0; i < array.length - 1; i++) {
            System.out.println(array[i] + " ;");
        }
        System.out.println(array[array.length - 1] + " ]");
    }

    public float[] Multiply(float[][] matrix, float[] input) {
        /*Multiplica un vector de dos dimensiones con uno e una dimension como
        Una matriz con un vector
         */
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

    public String ToString(float[] vector){
        /*
         Una funcion que convierte vectores (1D) en un string separado por ";"
         */
        StringBuilder output= new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if(i!=0){
                output.append(";");
            }
            output.append(vector[i]);
        }
        return output.toString();
    }

    public float[] Subtract(float[] first, float[] second) {
        /*
        Sustrae el vector (1D) "second" al vector (1d) "first"
         */
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
        /*
        Calcula el sigmoide de las entradas de un vector (1D)
        */
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (float) (2 / (1 + Math.exp(-input[i])) - 1);
        }
        return output;
    }

    public float Norm(float[] vector) {
        /*
        Calcula la norma de un vector (1D)
         */
        float result = 0;
        for (float v : vector) {
            result += v * v;
        }
        return result;
    }
}
