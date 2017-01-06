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

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

public class TextExtractor {

	private OutputStream outputstream;
	private ParseContext context;
	private Detector detector;
	private Parser parser;
	private Metadata metadata;
	private String extractedText;

	public TextExtractor() {
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

	public void extractKeywordValue() {
		// Get the text into a String object
		extractedText = outputstream.toString();

		InputStream inStream = new ByteArrayInputStream(extractedText.getBytes(StandardCharsets.UTF_8));
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
		
		Map<String, Set<String>> dataPointToKeywordsMap = new HashMap<String, Set<String>>();
		dataPointToKeywordsMap.put("Auditor Name", new HashSet<String>(Arrays.asList(
				"Chartered Accountants", "Public Accountants", "Registered Auditors", "Statutory Auditors")));

		Map<String, String> keywordsToDatapointMap = new HashMap<String, String>();
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
			//String currentDataPoint = null;
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
				System.out.println("Datapoint = " + keywordsToDatapointMap.get(key) + " Keyword = " + key + ", Values = " + values);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 1) {
			TextExtractor textExtractor = new TextExtractor();
			textExtractor.process(args[0]);
			textExtractor.extractKeywordValue();
		} else {
			throw new Exception();
		}
	}
}
