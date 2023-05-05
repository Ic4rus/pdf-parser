package com.itextpdf.kernel.pdf;

import com.itextpdf.io.source.*;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PdfParser {

    private final PdfReader reader;
    private final PdfDocument document;
    private static final String DEFAULT_PADDING = "    ";
    private static final String PATH_SEPARATOR = "/";

    public PdfParser(
            String filePath
    ) throws IOException {
        reader = new PdfReader(filePath);
        document = new PdfDocument(reader);
    }

    public void print() {
        printObject(PdfName.Root, document.getPdfObject(1), 0, PATH_SEPARATOR);
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x ", aByte));
        }
        return result.toString();
    }


    public String getPadding(
            int level
    ) {
        return DEFAULT_PADDING.repeat(level);
    }

    public String getPath(
            String path,
            PdfObject obj
    ) {
        return String.format(
                "%s%s%s",
                path,
                Optional.ofNullable(obj.indirectReference)
                        .map(PdfIndirectReference::getObjNumber)
                        .map(String::valueOf)
                        .orElse(""),
                PATH_SEPARATOR
        );
    }

    public boolean isLoop(
            String path,
            PdfObject obj
    ) {
        return obj.indirectReference != null && path.contains(String.valueOf(obj.indirectReference.objNr));
    }

    public void printObject(
            PdfName name,
            PdfObject obj,
            int level,
            String path
    ) {
        String padding = getPadding(level);
        boolean looping = isLoop(path, obj);
        path = getPath(path, obj);
        String label = String.format(
                "%s%s [%s%s]:",
                padding,
                Optional.ofNullable(name).map(PdfName::toString).orElse(""),
                Optional.ofNullable(obj.indirectReference).map(i -> i + " ").orElse(""),
                obj.getClass().getSimpleName()
        );
        if ((obj.isDictionary() || obj.isStream()) && !looping) {
            System.out.println(label);
            for (Map.Entry<PdfName, PdfObject> entry : ((PdfDictionary) obj).entrySet()) {
                PdfName key = entry.getKey();
                PdfObject value = entry.getValue();
                printObject(key, value, level + 1, path);
            }
        } else if (obj.isArray() && !looping) {
            System.out.println(label);
            PdfArray arr = (PdfArray) obj;
            for (int i = 0; i < arr.size(); i++) {
                printObject(null, arr.get(i), level + 1, path);
            }
        } else {
            System.out.printf("%s %s%n", label, obj);
        }
    }

    public void printDictionary(
            PdfDictionary dict
    ) {
        // PdfName - PdfObject
        for (Map.Entry<PdfName, PdfObject> entry : dict.entrySet()) {
            PdfName name = entry.getKey();
            PdfObject object = entry.getValue();
            if (object.isDictionary()) {
                System.out.printf("%s%n", name);
                printDictionary((PdfDictionary) object);
            } else {
                System.out.printf("%s %s%n", name, object);
            }
        }
    }

    public byte[] getPageContentBytes(
            int pageNum
    ) {
        PdfPage page = document.getPage(pageNum);
        return page.getContentBytes();
    }

    public void extractImage(
            PdfStream stream,
            String path
    ) throws IOException {
        PdfImageXObject img = new PdfImageXObject(stream);
        byte[] data = img.getImageBytes();
        Files.write(Paths.get(path), data);
    }

    public void extractCommand() throws IOException {
        for (int i = 1; i <= document.getNumberOfPdfObjects(); i++) {
            PdfObject obj = document.getPdfObject(i);
            if (obj.isStream()) {
                System.out.println("PdfObject:" + i);
                CommandParser cp = new CommandParser((PdfStream) obj);
                cp.getCommands();
            }
        }
    }



    public static void main(String[] args) throws IOException {
        String[] filePaths = new String[] {
//                "/home/icarus/Downloads/pdf/file_so1.pdf",
//                "/home/icarus/Downloads/pdf/file_so2.pdf",
//                "/home/icarus/Downloads/pdf/file_so3.pdf",
//                "/home/icarus/Downloads/pdf/file_so4_xml.pdf",
//                "/home/icarus/Downloads/pdf/file_so5_json.pdf",
                "/home/icarus/Downloads/pdf/file_so6_sign.pdf",
//                "/home/icarus/Downloads/pdf/file_so7.pdf",
//                "/home/icarus/Downloads/pdf/file_so8.pdf"
        };
        for (String path : filePaths) {
            System.out.println(path);
            PdfParser parser = new PdfParser(path);
            parser.extractCommand();
        }
    }
}
