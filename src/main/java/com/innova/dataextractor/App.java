package com.innova.dataextractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
//import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class App {
	public static void main(String[] args) throws IOException, TikaException, SAXException {
		Parser parser = new AutoDetectParser();
		BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);

		PDFParserConfig pdfConfig = new PDFParserConfig();
		pdfConfig.setExtractInlineImages(true);

		ParseContext parseContext = new ParseContext();
		parseContext.set(PDFParserConfig.class, pdfConfig);
		// need to add this to make sure recursive parsing happens!
		parseContext.set(Parser.class, parser);

		FileInputStream stream = new FileInputStream(
				"C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\2014-2015 - Annual Report - Cummins India Ltd.pdf");
		Metadata metadata = new Metadata();
		parser.parse(stream, handler, metadata, parseContext);

		File file = new File("C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\Hello.txt");

		// creates the file
		file.createNewFile();

		// creates a FileWriter Object
		FileWriter writer = new FileWriter(file);

		// Writes the content to the file
		writer.write(handler.toString());
		writer.flush();
		writer.close();

		System.out.println("writing to file complete.");

		// getting metadata of the document
		System.out.println("Metadata of the PDF:");
		String[] metadataNames = metadata.names();

		for (String name : metadataNames) {
			System.out.println(name + " : " + metadata.get(name));
		}

	}
}
