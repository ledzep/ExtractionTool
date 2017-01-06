package com.innova.dataextractor;
 
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

public class IntegrationNLPextraction {
	
	Document doc = null;
	
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost/extractor";

	// Database credentials
	static final String USER = "root";
	static final String PASS = "InnovaSOL247";
	
	private OutputStream outputstream;
	private ParseContext context;
	private Detector detector;
	private Parser parser;
	private Metadata metadata;
	private String extractedText;
	
	public IntegrationNLPextraction() {
		context = new ParseContext();
		detector = new DefaultDetector();
		parser = new AutoDetectParser(detector);
		context.set(Parser.class, parser);
		outputstream = new ByteArrayOutputStream();
		metadata = new Metadata();
	}
	
	public void process(String filename) throws Exception {
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
	
	private ArrayList<String> detectSentences(String parsedText, Map<String, Set<String>> dataPointToKeywordsMap) throws InvalidFormatException, IOException {
		String[] sentenceArr = {};
		ArrayList<String> auditorStr = new ArrayList<String>();
		ArrayList<String> strName = new ArrayList<String>();
		SentenceDetector sentenceDetector = null;
		
		// Loading sentence detection model
		final SentenceModel sentenceModel = new SentenceModel(new File("input/en-sent.bin"));
		sentenceDetector = new SentenceDetectorME(sentenceModel);
		sentenceArr = sentenceDetector.sentDetect(parsedText);
		
		for (Map.Entry<String, Set<String>> entry : dataPointToKeywordsMap.entrySet()) {
			//String key = entry.getKey();
			for (String value : entry.getValue()) {
				strName.add(value);
			}
		}
		
		for (String sentence : sentenceArr) {
			int i = 0;
			String [] items = strName.toArray(new String[strName.size()]);
			if (Arrays.stream(items).parallel().anyMatch(sentence::contains)) {
				String combinedStr = "";
				if (i > 0) {
					combinedStr = sentenceArr[i - 1];
				}
				auditorStr.add(combinedStr + " " + sentence + " " + sentenceArr[i + 1]);
			}
			i++;
		}
		return auditorStr;
	}
	
	private String[] extractAuditorName(ArrayList<String> filteredArr) throws InvalidFormatException, IOException {
		String[] nameArr = {};
		TokenNameFinderModel model = new TokenNameFinderModel(new File("input/en-ner-organization.bin"));

		// Create a NameFinder using the model
		NameFinderME finder = new NameFinderME(model);
		Tokenizer tokenizer = SimpleTokenizer.INSTANCE;

		for (String sentence : filteredArr) {
			// Split the sentence into tokens
			String[] tokens = tokenizer.tokenize(sentence);

			// Find the names in the tokens and return Span objects
			Span[] nameSpans = finder.find(tokens);
			nameArr = Span.spansToStrings(nameSpans, tokens);
		}
		return nameArr;
	}
	
	public String[] extractDatapointValue(Map<String, Set<String>> dataPointToKeywordsMap) throws InvalidFormatException, IOException {
		extractedText = outputstream.toString();
		ArrayList<String> filteredArr = new ArrayList<String>();
		filteredArr = detectSentences(extractedText, dataPointToKeywordsMap);
		return extractAuditorName(filteredArr);
	}
	
	public void outputXML(String[] dataExtracted, String filename)
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
		datapoints.appendChild(datapoint);
		Attr attr = doc.createAttribute("name");
		attr.setValue("Auditor Name");
		datapoint.setAttributeNode(attr);
		
		for (String value: dataExtracted) {
			Element valueEle = doc.createElement("value");
			valueEle.appendChild(doc.createTextNode(value));
			datapoint.appendChild(valueEle);
		}
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		/*StreamResult result = new StreamResult(
				new File("/tmp/dataextract/pdf/" + filename +".xml"));*/
		StreamResult result = new StreamResult(
				new File("C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\output.xml"));
		transformer.transform(source,result);
		// Output to console for testing
		StreamResult consoleResult = new StreamResult(System.out);
		transformerFactory.setAttribute("indent-number",4);transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		transformer.transform(source,consoleResult);
	}
	
	public static void main(String[] args) throws IOException, Exception {
		if (args.length == 1) {
			IntegrationNLPextraction textExtractor = new IntegrationNLPextraction();
			textExtractor.process(args[0]);
			Map<String, Set<String>> dataPointToKeywordsMap = new HashMap<String, Set<String>>();
			dataPointToKeywordsMap.put("Auditor Name", new HashSet<String>(Arrays.asList("Chartered Accountants",
					"Public Accountants", "Registered Auditors", "Statutory Auditors")));
			String[] extractedData = textExtractor.extractDatapointValue(dataPointToKeywordsMap);
			textExtractor.outputXML(extractedData, args[0]);
			System.out.println(extractedData);
			ArrayList<String> auditorList = new ArrayList<String>(Arrays.asList(extractedData));
			
			Set<String> auditorFirmresults = new HashSet<String>();
			Connection conn = null;
			Statement stmt = null;
			
			try {
				
				// STEP 2: Register JDBC driver
				Class.forName("com.mysql.jdbc.Driver");

				// STEP 3: Open a connection
				System.out.println("Connecting to database...");
				conn = DriverManager.getConnection(DB_URL, USER, PASS);

				// STEP 4: Execute a query
				System.out.println("Creating statement...");
				stmt = conn.createStatement();
				String sql;
				sql = "select name from auditors";
				ResultSet rs = stmt.executeQuery(sql);

				// STEP 5: Extract data from result set
				while (rs.next()) {
					auditorFirmresults.add(rs.getString("name"));
				}
				// STEP 6: Clean-up environment
				rs.close();
				stmt.close();
				conn.close();
			} catch (SQLException se) {
				// Handle errors for JDBC
				se.printStackTrace();
			} catch (Exception e) { 
				// Handle errors for Class.forName
				e.printStackTrace();
			} finally {
				// finally block used to close resources
				try {
					if (stmt != null)
						stmt.close();
				} catch (SQLException se2) {
				} // nothing we can do
				try {
					if (conn != null)
						conn.close();
				} catch (SQLException se) {
					se.printStackTrace();
				} // end finally try
			} // end try

			for (String auditor : auditorList) {
				int occurrences = Collections.frequency(auditorList, auditor);
				if (auditorFirmresults.contains(auditor)) {
					System.out.println("Auditor Name: " + auditor + " occurrences: " + occurrences);
					continue;
				} else {
					//System.out.println("No auditors found!!"); 
				}
			}
		} else {
			throw new Exception();
		}
		System.out.println("Done\n********************");
	}
}
