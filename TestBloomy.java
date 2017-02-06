package com.test.me;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class TestBloomy {
	static int elementCount = 1000000; // Number of elements to test
 
    public static void printStat(long start, long end) {
        double diff = (end - start) / 1000.0;
        System.out
                .println(diff + "s, " + (elementCount / diff) + " elements/s");
    }
 
    public void execute() {
        final Random r = new Random(System.currentTimeMillis());
 
        // Generate elements first
        List<MString> existingElements = new ArrayList<MString>(elementCount);
        byte[] lastRef=null;
        for (int i = 0; i < elementCount; i++) {
            byte[] b = new byte[100];
            r.nextBytes(b);
            existingElements.add(new MString(b));
            lastRef = b;
        }
 
        
        List<MString> nonExistingElements = new ArrayList<MString>(elementCount);
        for (int i = 0; i < elementCount; i++) {
            byte[] b = new byte[100];
            r.nextBytes(b);
            nonExistingElements.add(new MString(b));
        }
        long start_add = System.currentTimeMillis();
        long end_add =0;
        long start_contains =0,end_contains;
      
        // Another blooom filter
        System.out.println("Testing BloomFilter  " + elementCount + " elements");
        
        // Add elements
        System.out.print("add(): ");
        com.test.me.BloomFilter<MString> bm = new com.test.me.BloomFilter<MString>(0.01f, elementCount);
        start_add = System.currentTimeMillis();
        for (int i = 0; i < elementCount; i++) {
            bm.add(existingElements.get(i).getBytes());
        }
        end_add = System.currentTimeMillis();
        printStat(start_add, end_add);
 
        // Check for existing elements with contains()
        System.out.print("contains(), existing: ");
        start_contains = System.currentTimeMillis();
        for (int i = 0; i < elementCount; i++) {
        	bm.contains(existingElements.get(i).getBytes());
        }
        end_contains = System.currentTimeMillis();
        printStat(start_contains, end_contains);
 
        //
        // HashMap
        HashSet<MString> hm = new HashSet<MString>(elementCount);
        System.out.println("Testing HashSet  " + elementCount + " elements");
        // Add elements
        System.out.print("add(): ");
        start_add = System.currentTimeMillis();
        for (int i = 0; i < elementCount; i++) {
            hm.add(existingElements.get(i));
        }
        end_add = System.currentTimeMillis();
        printStat(start_add, end_add);
        // Check for existing elements with contains()
        System.out.print("contains(), existing: ");
        start_contains = System.currentTimeMillis();
        for (int i = 0; i < elementCount; i++) {
            hm.contains(existingElements.get(i));
        }
        end_contains = System.currentTimeMillis();
        printStat(start_contains, end_contains);
      
    }
 
    public static void main(String[] argv) {
        TestBloomy bm = new TestBloomy();
        bm.execute();
 
    }
 
}

class MString {
	byte[] arr;
	MString(byte[] arr)
	{
		this.arr = arr;
	}
	public byte[] getBytes() {
		return arr;
	}
}
