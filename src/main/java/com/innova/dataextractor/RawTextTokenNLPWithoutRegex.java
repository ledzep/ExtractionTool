package com.innova.dataextractor;


/*import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;*/
import java.io.IOException;
//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.Span;


public class RawTextTokenNLPWithoutRegex {

	public static void main(String[] args) {
		/*String content = "Cloud Computing is one of the Price Waterhouse fastest-growing segments in IT, forecasted to reach US$230 globally by 2017. "
				+ "There are hundreds of companies worldwide seeking to control supremacy by offering many “…as-a-service” products and services, "
				+ "often with very few differentiators except pricing which is in a downward spiral. "
				+ "C3DNA, a Silicon Valley startup, founded by professionals with deep industry experience and extensive academic background, "
				+ "offers an infrastructure-agnostic cloud product that could render many of today’s ‘leading’ cloud vendors irrelevant in the not-too-distant future.";*/
		
		String content = "The price water, chartered accountant did finnancial accounting";
		Map<String, Set<String>> dataPointToKeywordsMap = new HashMap<String, Set<String>>();
		dataPointToKeywordsMap.put("Auditor Name", new HashSet<String>(Arrays.asList("Chartered", "Cloud Computing", "Silicon Valley", "cloud",
				"Chartered Accountant", "Public Accountant", "Registered Auditors", "Satutory Auditors")));
		
		Map<String, String> keywordsToDatapointMap = new HashMap<String, String>();
		for (Map.Entry<String, Set<String>> entry : dataPointToKeywordsMap.entrySet()) {
			String key = entry.getKey();
			for (String value : entry.getValue()) {
				keywordsToDatapointMap.put(value, key);
			}
		}
		
		// map of keyword to keyword's regex
		Map<String, Pattern[]> regexMap = new HashMap<>();
		for (Map.Entry<String, String> entry : keywordsToDatapointMap.entrySet()) {
			String key = entry.getKey();
			Pattern keywordPattern = Pattern.compile(key);
			Pattern[] patterns = new Pattern[]{keywordPattern};
			regexMap.put(key, patterns);
		}
		
		// datapoint (String) => list of values (String)
		Map<String, Set<String>> datapointToValuesMap = new HashMap<String, Set<String>>();
		Set<String> valList;
		
		
		// Reading line by line from the text file
		try {
			TextTokenizer tokenizer = new TextTokenizer("input/en-sent.bin", "input/en-token.bin", "input/en-ner-organization.bin", content);
			RegexNameFinder keywordRegexFinder = new RegexNameFinder(regexMap);
			String keywordPart1 = null;
			String keywordPart2 = null;
			String currentDataPoint = null;
			String sentence = null;
			while ((sentence = tokenizer.getNextSentence()) != null) {
				System.out.println("Sentence to be tokenized: " + sentence);
				// Parsing the words from each line
				String[] tokens = tokenizer.getTokensForSentence(sentence);
				System.out.println("====Tokens====");
				for (String strToken: tokens) {
					System.out.println(strToken);
				}
				Span[] names = tokenizer.getNameFinder().find(tokens);
				System.out.println("====Names====");
				for (Span sp: names) {
					System.out.println(sp.toString() + " Probability: " + sp.getProb());
				}
				int currentTokenIndex = 0;
				Span[] keywordResult = keywordRegexFinder.find(tokens);
				while (currentTokenIndex < tokens.length) {
					String currentWord = tokens[currentTokenIndex++];
					//System.out.println("Current word from token array: " + currentWord);
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
						currentDataPoint = keywordsToDatapointMap.get(kBoth);
					} else if (keywordsToDatapointMap.containsKey(keywordPart1)) {
						currentDataPoint = keywordsToDatapointMap.get(keywordPart1);
						keywordValue = keywordPart2;
					}

					keywordPart1 = keywordPart2;
					keywordPart2 = currentWord;
					if (currentDataPoint != null) {
						//System.out.println("currentDataPoint value: " + currentDataPoint);
						if (datapointToValuesMap.containsKey(currentDataPoint)) {
							valList = datapointToValuesMap.get(currentDataPoint);
							valList.add(keywordValue);
						} else {
							valList = new HashSet<String>();
							valList.add(keywordValue);
							datapointToValuesMap.put(currentDataPoint, valList);
						}
						currentDataPoint = null;
					}
				}
			}
			// iterate and display values
			System.out.println("Fetching Keys and corresponding [Multiple] Values");
			for (Map.Entry<String, Set<String>> entry : datapointToValuesMap.entrySet()) {
				String key = entry.getKey();
				Set<String> values = entry.getValue();
				System.out.println("Datapoint = " + key + ", Values = " + values);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
