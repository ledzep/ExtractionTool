package com.innova.dataextractor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
//import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;

public class TextExtractorXMLkeywordMultipleFile {

	Map<String, String> keywordsToDatapointMap = new HashMap<String, String>();
	Document doc = null;

	private OutputStream outputstream;
	private ParseContext context;
	private Detector detector;
	private Parser parser;
	private Metadata metadata;
	private String extractedText;

	public TextExtractorXMLkeywordMultipleFile() {
		context = new ParseContext();
		detector = new DefaultDetector();
		parser = new AutoDetectParser(detector);
		context.set(Parser.class, parser);
		outputstream = new ByteArrayOutputStream();
		metadata = new Metadata();
	}

	public void process(String filename) throws Exception {
		System.out.println(filename);
		URL url;
		File file = new File(filename);
		if (file.isFile()) {
			url = file.toURI().toURL();
		} else {
			url = new URL(filename);
		}
		InputStream input = TikaInputStream.get(url, metadata);
		ContentHandler handler = new BodyContentHandler(outputstream);
		parser.parse(input, handler, metadata, context);
		input.close();
	}

	public Map<String, Set<String>> extractKeywordValue() {
		// Get the text into a String object
		extractedText = outputstream.toString();

		InputStream inStream = new ByteArrayInputStream(extractedText.getBytes(StandardCharsets.UTF_8));
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));

		Map<String, Set<String>> dataPointToKeywordsMap = new HashMap<String, Set<String>>();
		dataPointToKeywordsMap.put("Auditor Name", new HashSet<String>(Arrays.asList("Chartered Accountants",
				"Public Accountants", "Registered Auditors", "Statutory Auditors")));

		for (Map.Entry<String, Set<String>> entry : dataPointToKeywordsMap.entrySet()) {
			String key = entry.getKey();
			for (String value : entry.getValue()) {
				keywordsToDatapointMap.put(value, key);
			}
		}

		// datapoint (String) => list of values (String)
		Map<String, Set<String>> keywordToValuesMap = new HashMap<String, Set<String>>();
		Set<String> valList;
		String currentLine = null;

		// Reading line by line from the text file
		try {
			String keywordPart1 = null;
			String keywordPart2 = null;
			String currentKeyword = null;
			while ((currentLine = br.readLine()) != null) {
				// Parsing the words from each line
				StringTokenizer st = new StringTokenizer(currentLine, " \t\n\r\f,;:");
				while (st.hasMoreTokens()) {
					String currentWord = st.nextToken();
					if (keywordPart1 == null) {
						keywordPart1 = currentWord;
						continue;
					}
					if (keywordPart2 == null) {
						keywordPart2 = currentWord;
						continue;
					}
					String kBoth = keywordPart1 + ' ' + keywordPart2;
					String keywordValue = currentWord;
					if (keywordsToDatapointMap.containsKey(kBoth)) {
						currentKeyword = kBoth;
					} else if (keywordsToDatapointMap.containsKey(keywordPart1)) {
						currentKeyword = keywordPart1;
						keywordValue = keywordPart2;
					}
					keywordPart1 = keywordPart2;
					keywordPart2 = currentWord;
					if (currentKeyword != null) {
						if (keywordToValuesMap.containsKey(currentKeyword)) {
							valList = keywordToValuesMap.get(currentKeyword);
							valList.add(keywordValue);
						} else {
							valList = new HashSet<String>();
							valList.add(keywordValue);
							keywordToValuesMap.put(currentKeyword, valList);
						}
						currentKeyword = null;
					}
				}
			}
			// iterate and display values
			System.out.println("Fetching Keys and corresponding [Multiple] Values");
			for (Map.Entry<String, Set<String>> entry : keywordToValuesMap.entrySet()) {
				String key = entry.getKey();
				Set<String> values = entry.getValue();
				System.out.println("Datapoint = " + keywordsToDatapointMap.get(key) + " Keyword = " + key
						+ ", Values = " + values);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keywordToValuesMap;
	}

	public void outputXML(Map<String, Set<String>> dataExtracted, String filename)
			throws ParserConfigurationException, TransformerException {
		System.out.println(filename);
		DocumentBuilderFactory docBuildFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuildFactory.newDocumentBuilder();
		doc = docBuilder.newDocument();

		// root element - autoDataExt
		Element autoDataExt = doc.createElement("autoDataExt");
		doc.appendChild(autoDataExt);

		// inputFile elements
		Element inputFile = doc.createElement("inputFile");
		autoDataExt.appendChild(inputFile);

		Element fileNameEle = doc.createElement("name");
		fileNameEle.appendChild(doc.createTextNode(filename));
		inputFile.appendChild(fileNameEle);

		Element datapoints = doc.createElement("datapoints");
		autoDataExt.appendChild(datapoints);

		Element datapoint = doc.createElement("datapoint");
		
		for (Map.Entry<String, String> entry : keywordsToDatapointMap.entrySet()) {
			datapoints.appendChild(datapoint);
			Attr attr = doc.createAttribute("name");
			attr.setValue(entry.getValue());
			datapoint.setAttributeNode(attr);
		}
		dataExtracted.forEach((keyword, keywordValue) -> {
			Element keywordEle = doc.createElement("keyword");
			datapoint.appendChild(keywordEle);
			Attr attr = doc.createAttribute("name");
			attr.setValue(keyword);
			keywordEle.setAttributeNode(attr);
			keywordValue.forEach((value) -> {
				// System.out.println("Value: " + value);
				// set attribute to datapoint element
				Element valueEle = doc.createElement("value");
				valueEle.appendChild(doc.createTextNode(value));
				keywordEle.appendChild(valueEle);
			});
		});
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(
				new File("C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\" + filename +".xml"));
		transformer.transform(source,result);
		// Output to console for testing
		StreamResult consoleResult = new StreamResult(System.out);
		transformerFactory.setAttribute("indent-number",4);transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		transformer.transform(source,consoleResult);
	}
	

	public static void main(String[] args) throws Exception {
		File dir = new File("C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\pdfkeyvalue");
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
		    for (File child : directoryListing) {
		    	//System.out.println("file: " + FilenameUtils.removeExtension(child.getName()));
		    	TextExtractorXMLkeywordMultipleFile textExtractor = new TextExtractorXMLkeywordMultipleFile();
				textExtractor.process(child.getCanonicalPath());
				Map<String, Set<String>> extractedData = textExtractor.extractKeywordValue();
				System.out.println(extractedData);
				textExtractor.outputXML(extractedData, FilenameUtils.removeExtension(child.getName()));
		    }
		  } else {
		    // Handle the case where dir is not really a directory.
		    // Checking dir.isDirectory() above would not be sufficient
		    // to avoid race conditions with another process that deletes
		    // directories.
		  }
	}
}
