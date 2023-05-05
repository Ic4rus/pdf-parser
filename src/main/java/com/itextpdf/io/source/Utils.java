package com.itextpdf.io.source;

public class Utils {

    public static ArrayRandomAccessSource getArrayRandomAccessSource(byte[] data) {
        return new ArrayRandomAccessSource(data);
    }

}
