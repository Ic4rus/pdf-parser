package com.itextpdf.kernel.pdf;

import java.io.IOException;

public class Parser {

    private final PdfReader reader;
    private final PdfDocument document;

    public Parser(
            String filePath
    ) throws IOException {
        reader = new PdfReader(filePath);
        document = new PdfDocument(reader);
    }

    public void test() {
        System.out.println("TEST");
        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            PdfPage page = document.getPage(i);
            page.getContentBytes();
            System.out.printf("Page %d: %d%n", i, page.getContentStreamCount());
        }
    }

    public byte[] getPageContentBytes(
            int pageNum
    ) {
        PdfPage page = document.getPage(pageNum);
        return page.getContentBytes();
    }


    public static void main(String[] args) throws IOException {
        String filePath = "/home/icarus/Downloads/pdf/file_so1.pdf";
//        String filePath = "/home/icarus/Downloads/mau.pdf";
        // Page 1: Content co 8 object
        Parser parser = new Parser(filePath);
        parser.test();
        System.out.println("TEST");
    }

}
