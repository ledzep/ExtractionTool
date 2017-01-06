package com.innova.dataextractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

public class NLPextraction {
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost/extractor";

	// Database credentials
	static final String USER = "root";
	static final String PASS = "InnovaSOL247";
	
	ArrayList<String> strName = new ArrayList<String>();

	public ArrayList<String> detectSentences(String parsedText) throws InvalidFormatException, IOException {
		String[] sentenceArr = {};
		ArrayList<String> auditorStr = new ArrayList<String>();
		SentenceDetector sentenceDetector = null;
		// Loading sentence detection model
		final SentenceModel sentenceModel = new SentenceModel(new File("input/en-sent.bin"));
		BufferedWriter buffWriter = new BufferedWriter(
				new FileWriter("C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\pdfkeyvalue\\sample.txt"));
		BufferedWriter buffWriter1 = new BufferedWriter(
				new FileWriter("C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\pdfkeyvalue\\filteredSentences.txt"));
		int i = 0;

		sentenceDetector = new SentenceDetectorME(sentenceModel);
		sentenceArr = sentenceDetector.sentDetect(parsedText);
		Map<String, Set<String>> dataPointToKeywordsMap = new HashMap<String, Set<String>>();
		dataPointToKeywordsMap.put("Auditor Name", new HashSet<String>(Arrays.asList("Chartered Accountants",
				"Public Accountants", "Registered Auditors", "Statutory Auditors")));
		
		
		for (Map.Entry<String, Set<String>> entry : dataPointToKeywordsMap.entrySet()) {
			//String key = entry.getKey();
			for (String value : entry.getValue()) {
				strName.add(value);
			}
		}
		
		
		for (String sentence : sentenceArr) {
			buffWriter.append("\n\nSentence " + i + ": " + sentence);
			String [] items = strName.toArray(new String[strName.size()]);
			
			if (Arrays.stream(items).parallel().anyMatch(sentence::contains)) {
				String combinedStr = "";
				buffWriter1.append("\n-----------------------Block " + i + " Start-------------------------");
				if (i > 0) {
					buffWriter1.append("\n\n**********************************" + "\nSentence " + (i - 1) + ": "
							+ sentenceArr[i - 1]);
					combinedStr = sentenceArr[i - 1];
				}

				buffWriter1.append("\n\n**************************************" + "\nSentence " + i + ": " + sentence);

				buffWriter1.append("\n\n**************************************" + "\nSentence " + (i + 1) + ": "
						+ sentenceArr[i + 1]);

				buffWriter1.append("\n-----------------------Block " + i + " End-------------------------");
				auditorStr.add(combinedStr + " " + sentence + " " + sentenceArr[i + 1]);
			}
			i++;
		}
		buffWriter.close();
		buffWriter1.close();
		return auditorStr;
	}

	public String[] extractAuditorName(ArrayList<String> filteredArr) throws InvalidFormatException, IOException {
		String[] nameArr = {};
		TokenNameFinderModel model = new TokenNameFinderModel(new File("input/en-ner-organization.bin"));

		// Create a NameFinder using the model
		NameFinderME finder = new NameFinderME(model);
		Tokenizer tokenizer = SimpleTokenizer.INSTANCE;

		for (String sentence : filteredArr) {
			// Split the sentence into tokens
			System.out.println("\n***********************************************" + "\nSentence: " + sentence);
			String[] tokens = tokenizer.tokenize(sentence);

			// Find the names in the tokens and return Span objects
			Span[] nameSpans = finder.find(tokens);
			nameArr = Span.spansToStrings(nameSpans, tokens);
		}
		return nameArr;
	}

	public static void main(String[] args) throws IOException, SAXException, TikaException {
		String sourceDir = "C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\pdfkeyvalue\\2014-2015 - Annual Report - Cummins India Ltd.pdf";

		String[] nameArr = {};
		ArrayList<String> filteredArr = new ArrayList<String>();

		Parser parser = new AutoDetectParser();
		ParseContext parseContext = new ParseContext();
		NLPextraction extract = new NLPextraction();

		// need to add this to make sure recursive parsing happens!
		parseContext.set(Parser.class, parser);

		FileInputStream stream = new FileInputStream(sourceDir);
		Metadata metadata = new Metadata();
		BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);
		parser.parse(stream, handler, metadata, parseContext);

		filteredArr = extract.detectSentences(handler.toString());
		nameArr = extract.extractAuditorName(filteredArr);
		System.out.println("========dumping extracted auditor name array==========");
		ArrayList<String> auditorList = new ArrayList<String>(Arrays.asList(nameArr));
		for (String auditor : auditorList) {
			System.out.println(auditor);
		}
		System.out.println();
		stream.close();

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

		// int wordRank = 0;
		for (String auditor : auditorList) {
			int occurrences = Collections.frequency(auditorList, auditor);
			if (auditorFirmresults.contains(auditor)) {
				System.out.println("Auditor Name: " + auditor + " occurrences: " + occurrences);
				continue;
			} else {
				System.out.println("No auditors found!!"); 
			}
		}
		

		System.out.println("Done\n********************");
	}
}
