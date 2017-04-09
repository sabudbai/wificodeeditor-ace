package com.sirajinhoapp.wificodeeditor;

/**
 * Created by siraj on 04.04.17.
 */
public class CCompiler {
    private native void print();

    public static void main(String[] args) {
        new CCompiler().print();
    }

    static {
        System.loadLibrary("cc");
    }
}

