package edu.ncsu.hmalipa.csc570.project1;

import java.util.Random;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        Random rand = new Random();
        long experiments = 100000;
        long i = 0;
        double sum = 0d;
        
        while (i++ < experiments) {
            sum = sum + rand.nextDouble();
        }
        System.out.println((sum/experiments));
        
    }
}
