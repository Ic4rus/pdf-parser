package com.itextpdf.kernel.pdf;

import com.itextpdf.commons.utils.MessageFormatUtil;
import com.itextpdf.io.source.IRandomAccessSource;
import com.itextpdf.io.source.PdfTokenizer;
import com.itextpdf.io.source.RandomAccessFileOrArray;
import com.itextpdf.io.source.RandomAccessSourceFactory;
import com.itextpdf.kernel.exceptions.KernelExceptionMessageConstant;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.pdf.canvas.parser.util.InlineImageParsingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandParser {

    private PdfTokenizer tokens;
    private boolean parseable;

    public CommandParser(
            PdfStream stream
    ) {
        byte[] bytes = stream.getBytes();
        IRandomAccessSource source = new RandomAccessSourceFactory().createSource(bytes);
        RandomAccessFileOrArray array = new RandomAccessFileOrArray(source);
        tokens = new PdfTokenizer(array);
        parseable = !PdfName.Image.equals(stream.getAsName(PdfName.Subtype))
                && !PdfName.ObjStm.equals(stream.getAsName(PdfName.Type))
                && !PdfName.Metadata.equals(stream.getAsName(PdfName.Type))
                && !PdfName.XRef.equals(stream.getAsName(PdfName.Type))
                && stream.get(PdfName.Length1) == null;
    }

    public List<List<PdfObject>> getCommands() throws IOException {
        List<List<PdfObject>> commands = new ArrayList<>();
        if (!parseable) {
            return commands;
        }
        while (true) {
            List<PdfObject> command = getCommand();
            if (!command.isEmpty()) {
                String keyword = command.get(command.size() - 1).toString();
                if (keyword.equals("ri")) {
                    System.out.println(command.size());
                }
                commands.add(command);
            } else {
                break;
            }
        }
        return commands;
    }

    public List<PdfObject> getCommand() throws IOException {
        List<PdfObject> list = new ArrayList<>();
        PdfObject obj;
        while ((obj = readObject()) != null) {
            list.add(obj);
            if (tokens.getTokenType() == PdfTokenizer.TokenType.Other) {
                if ("BI".equals(obj.toString())) {
//                    PdfStream inlineImageAsStream = InlineImageParsingUtils.parse(this, currentResources.getResource(PdfName.ColorSpace));
//                    list.clear();
//                    list.add(inlineImageAsStream);
//                    list.add(new PdfLiteral("EI"));
                }
                break;
            }
        }
        return list;
    }

    public PdfObject readObject() throws IOException {
        if (!nextValidToken())
            return null;
        final PdfTokenizer.TokenType type = tokens.getTokenType();
        switch (type) {
            case StartDic: {
                return readDictionary();
            }
            case StartArray:
                return readArray();
            case String:
                PdfString str = new PdfString(tokens.getDecodedStringContent()).setHexWriting(tokens.isHexString());
                return str;
            case Name:
                return new PdfName(tokens.getByteContent());
            case Number:
                //use PdfNumber(byte[]) here, as in this case number parsing won't happen until it's needed.
                return new PdfNumber(tokens.getByteContent());
            default:
                return new PdfLiteral(tokens.getByteContent());
        }
    }

    /**
     * Reads the next token skipping over the comments.
     * @return <CODE>true</CODE> if a token was read, <CODE>false</CODE> if the end of content was reached
     * @throws IOException on error
     */
    public boolean nextValidToken() throws IOException {
        while (tokens.nextToken()) {
            if (tokens.getTokenType() == PdfTokenizer.TokenType.Comment)
                continue;
            return true;
        }
        return false;
    }

    public PdfArray readArray() throws IOException {
        PdfArray array = new PdfArray();
        while (true) {
            PdfObject obj = readObject();
            if (!obj.isArray() && tokens.getTokenType() == PdfTokenizer.TokenType.EndArray) {
                break;
            }
            if (tokens.getTokenType() == PdfTokenizer.TokenType.EndDic && obj.getType() != PdfObject.DICTIONARY) {
                tokens.throwError(MessageFormatUtil.format(KernelExceptionMessageConstant.UNEXPECTED_TOKEN, ">>"));
            }
            array.add(obj);
        }
        return array;
    }

    public PdfDictionary readDictionary() throws IOException {
        PdfDictionary dic = new PdfDictionary();
        while (true) {
            if (!nextValidToken())
                throw new PdfException(KernelExceptionMessageConstant.UNEXPECTED_END_OF_FILE);
            if (tokens.getTokenType() == PdfTokenizer.TokenType.EndDic)
                break;
            if (tokens.getTokenType() != PdfTokenizer.TokenType.Name)
                tokens.throwError(
                        KernelExceptionMessageConstant.THIS_DICTIONARY_KEY_IS_NOT_A_NAME, tokens.getStringValue()
                );
            PdfName name = new PdfName(tokens.getStringValue());
            PdfObject obj = readObject();
            dic.put(name, obj);
        }
        return dic;
    }


}
