package com.ubnt.xml; //@date 11.12.2022

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Simple writer delegate that transforms the {@link Document} created by an
 * {@link UbntServiceXMLBuilder} into an XML-File.
 *
 * @see UbntServiceXMLBuilder
 * @see UbntServiceXMLHandler
 */
public class UbntServiceXMLWriter {

    /**
     * Writes the provided document to the given path.
     *
     * @param path the destination
     * @param document the document to write
     */
    public static void writeXML(String path, Document document) {
        writeXML(new File(path), document);
    }

    /**
     * Writes the provided document to the given file.
     *
     * @param file the destination file
     * @param document the document to write
     */
    public static boolean writeXML(File file, Document document) {
        try {
            TransformerFactory factory     = TransformerFactory.newInstance();
            Transformer        transformer = factory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "iubntservice.dtd");

            try (OutputStream stream = new FileOutputStream(file)) {
                transformer.transform(
                        new DOMSource(document),
                        new StreamResult(stream));
            }
            return true;
        } catch (Exception e) {
            // maybe add a logger here
            return false;
        }
    }
}
