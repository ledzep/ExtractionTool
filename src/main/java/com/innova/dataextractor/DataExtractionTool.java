package com.innova.dataextractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class DataExtractionTool {

	public static void main(String[] args) {		
		String pdfFilePath = "D:\\AutoDataExtraction\\MSCI PDF Doc\\SamplePDFFile\\2014-2015 - Annual Report - Cummins India Ltd.pdf";
		Map<String, ArrayList<String>> datapoints = new HashMap<String, ArrayList<String>>();
		ArrayList<String> synonyms = new ArrayList<String>();
		synonyms.add(" ");
		synonyms.add("Statutory Auditor");
		synonyms.add("Public Accountant");
		synonyms.add("Registered Auditors");
		datapoints.put("Auditor Name", synonyms);
		System.out.println("Completed");
		startExtract(pdfFilePath, datapoints);
	}
	
	public static Map<String, Map<String, List<String>>> startExtract(String filePath, Map<String, ArrayList<String>> dpData){
		String parsedText = "";
		String[] sentencesArr = {};
		Map<String, Map<String, ArrayList<String>>> filteredSentences = new HashMap<String, Map<String,ArrayList<String>>>();
		Map<String, Map<String, List<String>>> outputVal = new HashMap<String, Map<String,List<String>>>();
		try {
			DataExtractionTool dataExt = new DataExtractionTool();
			parsedText = dataExt.parsePDFUsingTika(filePath);
			sentencesArr = dataExt.detectSentences(parsedText);
			filteredSentences = dataExt.filterSentenceUsingDataPoint(sentencesArr, dpData);	
			outputVal = dataExt.extractUsingOrgModel(filteredSentences);
			System.out.println("Completed");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return outputVal;
	}
	
	public String parsePDFUsingTika(String pdfFilePath){
		String parsedText = "";
		
		Parser parser = new AutoDetectParser();
		ParseContext parseContext = new ParseContext();
		FileInputStream stream = null;
		BufferedWriter out = null;
		
		//need to add this to make sure recursive parsing happens!
        parseContext.set(Parser.class, parser);
        
        System.out.println("*****************\nFile to be parsed: "+pdfFilePath);		
		try {
			stream = new FileInputStream(pdfFilePath);
						       	
	    	out = new BufferedWriter(new FileWriter(pdfFilePath+".txt"));
	                	
	        Metadata metadata = new Metadata();
	        BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);
	                    
	        parser.parse(stream, handler, metadata, parseContext);
	        System.out.println(pdfFilePath+"\n Parsed successfully.");
	        
	        System.out.println("Copy parsed text to "+pdfFilePath+".txt");
	        parsedText = handler.toString();
	        out.write(handler.toString());                       
	        System.out.println("Parsed text copied to "+pdfFilePath+".txt");
	        
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
			System.out.println("File not found!!"+e);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (TikaException e) {
			e.printStackTrace();
		}
		finally{
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
		return parsedText;
	}
	
	public String[] detectSentences(String parsedText){
		String[] sentenceArr = {};		
		SentenceDetector sentenceDetector = null;	    
	    SentenceModel sentenceModel;
	    
		try {
			if(parsedText.trim().length() > 0){
				sentenceModel = new SentenceModel(new File("input/en-sent.bin"));	    	  	
				sentenceDetector = new SentenceDetectorME(sentenceModel);		
				sentenceArr = sentenceDetector.sentDetect(parsedText.trim());				
			}
		} catch (InvalidFormatException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}
	    return sentenceArr;
	}
	
	public Map<String, Map<String, ArrayList<String>>> filterSentenceUsingDataPoint(String[] sentencesArr, Map<String, ArrayList<String>> datapoints){
		Map<String, Map<String, ArrayList<String>>> filteredSentencesDP = new HashMap<String, Map<String,ArrayList<String>>>(); 
				
		for (Map.Entry<String, ArrayList<String>> datapoint : datapoints.entrySet()) {				
		    System.out.println("Key = " + datapoint.getKey() + ", Value = " + datapoint.getValue());
		    Map<String, ArrayList<String>> filteredSentencesSyn = new HashMap<String, ArrayList<String>>();
		    
		    for (String synonym : datapoint.getValue()) {
		    	System.out.println("Value = " + synonym);
		    	ArrayList<String> filteredSentences = new ArrayList<String>();
		    			    	
		    	for (int i = 0; i < sentencesArr.length; i++) {
		    		String combinedString = "";
				    if(sentencesArr[i].trim().toLowerCase().contains(synonym.trim().toLowerCase())){			    		
			    		if(i > 0){	    			
			    			combinedString = sentencesArr[i-1].trim();
			    		}
			    		combinedString = combinedString + sentencesArr[i].trim();
			    		
			    		if(i < sentencesArr.length - 1){			    			
			    			combinedString = combinedString + sentencesArr[i+1].trim();
			    		}
			    		filteredSentences.add(combinedString);
			    	}//end of if				    				    
		    	}//end of "for sentencesArr"
		    	filteredSentencesSyn.put(synonym, filteredSentences);
			}//end of "for datapoint/synonym"
		    filteredSentencesDP.put(datapoint.getKey(), filteredSentencesSyn);
		}//end of "for datapoints/keyword"
		return filteredSentencesDP;
	}
	
	public Map<String, Map<String, List<String>>> extractUsingOrgModel(Map<String, Map<String, ArrayList<String>>> filteredSentencesDP){
		Map<String, Map<String, List<String>>> filteredValForDataPoint = new HashMap<String, Map<String,List<String>>>(); 
		TokenNameFinderModel model = null;
		
		try {
			model = new TokenNameFinderModel(new File("input/en-ner-organization.bin"));
		
		    NameFinderME finder = new NameFinderME(model);
		    Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
		    
		    for (String datapoint : filteredSentencesDP.keySet()) {
		    	Map<String, ArrayList<String>> datapointMap = filteredSentencesDP.get(datapoint);
		    	Map<String, List<String>> filteredValForSynonym = new HashMap<String, List<String>>();
		    	
		    	for (Map.Entry<String, ArrayList<String>> entry : datapointMap.entrySet()) {
		    		System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		    		List<String> filteredValues = new ArrayList<String>();
		    		
		    		for(String sentence : entry.getValue()){
		    			// Split the sentence into tokens
		    	    	String[] tokens = tokenizer.tokenize(sentence);            
		    	        
		    			// Find the names in the tokens and return Span objects
		    			Span[] nameSpans = finder.find(tokens);
		    			String[] orgArr = Span.spansToStrings(nameSpans, tokens);		    			
		    			filteredValues.addAll(Arrays.asList(Span.spansToStrings(nameSpans, tokens)));
		    			
		    			// = Arrays.asList(orgArr);
		    			
		    			// Print the names extracted from the tokens using the Span data
		    			System.out.println("Data List: "+Arrays.toString(Span.spansToStrings(nameSpans, tokens)));
		    			
		    		}//end of "for sentences"
		    		System.out.println(filteredValues);
		    		filteredValForSynonym.put(entry.getKey(), filteredValues);
				}//end of "for synonym"
		    	filteredValForDataPoint.put(datapoint, filteredValForSynonym);
		    }//end of "for datapoint"
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return filteredValForDataPoint;
	}
	
	public Map<String, Map<String, List<String>>> dbMatch (Map<String, Map<String, List<String>>> outputVal) {
		// JDBC driver name and database URL
		final String DB_URL = "jdbc:mysql://localhost/extractor";
		
		// Database credentials
		final String USER = "";
		final String PASS = "";
		
		Connection conn = null;
		Statement stmt = null;
		
		ArrayList<String> auditorResultsFromDB = new ArrayList<String>();
		Map<String, Map<String, List<String>>> dbMatchofAuditor = new HashMap<String, Map<String,List<String>>>();
		Map<String, List<String>> filteredValForSynonymMatchingInDB = new HashMap<String, List<String>>();
		List<String> filteredValues = new ArrayList<String>();
		
		for (Map.Entry<String, Map<String, List<String>>> entry : outputVal.entrySet()) {
			String dataPoint = entry.getKey();
			Map<String, List<String>> InnerMap = entry.getValue();
			for (Map.Entry<String, List<String>> innerMapEntry : InnerMap.entrySet()) {
				String keyword = innerMapEntry.getKey();
				for (String value : innerMapEntry.getValue()) {
					if (auditorResultsFromDB.contains(value)) {
						filteredValues.add(value);
					}
				}
				filteredValForSynonymMatchingInDB.put(keyword, filteredValues);
			}
			dbMatchofAuditor.put(dataPoint, filteredValForSynonymMatchingInDB);
		 }
		
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
				auditorResultsFromDB.add(rs.getString("name"));
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
		
		return dbMatchofAuditor;
	}
}
