package io.github.avantgarde95.balltest.util;

/**
 * Created by Jongmin on 2017-01-10.
 */

public class Debug {
    public static void printArr(float[] vertices){
        int i;
        for(i=0; i<vertices.length; i++){
            if(i%3 == 0){
                System.out.print((i/3) + " : ");
            }
            System.out.print(vertices[i] + " ");
            if(i%3 == 2){
                System.out.println();
            }
        }
    }
    public static void printvert(float[] vertices){
        int i;
        for(i=0; i<vertices.length; i++){
            System.out.print(vertices[i] + ", ");
            if(i%3 == 2){
                System.out.println();
            }
        }
    }
}