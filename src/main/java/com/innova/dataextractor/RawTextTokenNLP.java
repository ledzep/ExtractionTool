package com.innova.dataextractor;

import java.io.ByteArrayOutputStream;
import java.io.File;
/*import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;*/
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.util.Span;

public class RawTextTokenNLP {

	public class ValDetails {
		public String value;
		public double probability;
	}

	private static final String[] emptyTokens = {};
	Map<String, String> keywordsToDatapointMap = new HashMap<String, String>();
	Document doc = null;

	private OutputStream outputstream;
	private ParseContext context;
	private Detector detector;
	private Parser parser;
	private Metadata metadata;
	private String extractedText;

	public RawTextTokenNLP() {
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

	// returns maximum num tokens starting from position pos
	public static String[] getNextTokens(int pos, int num, String[] allTokens) {
		if (pos >= allTokens.length) {
			return emptyTokens;
		}
		int lastIndex = Math.min(pos + num, allTokens.length);
		return Arrays.copyOfRange(allTokens, pos, lastIndex);
	}

	public static String getSpanValue(Span s, String[] allTokens) {
		List<String> strList = new ArrayList<>();
		System.out.println("Span= " + s.toString() + " tokens= " + String.join(" ", allTokens));
		for (int i = s.getStart(); i < s.getEnd(); i++) {
			strList.add(allTokens[i]);
		}
		return String.join(" ", strList);
	}

	public static Set<Span> getUniqueSpans(Span[] spans) {
		Map<Integer, Span> posToLongestSpan = new HashMap<>();
		for (Span sp : spans) {
			int key = sp.getStart();
			if (posToLongestSpan.containsKey(key)) {
				if (sp.length() > posToLongestSpan.get(key).length()) {
					posToLongestSpan.put(key, sp);
				}
			} else {
				posToLongestSpan.put(key, sp);
			}
		}
		Set<Span> uniqueKeywordResult = new HashSet<>();
		for (Map.Entry<Integer, Span> entry : posToLongestSpan.entrySet()) {
			Span sp = entry.getValue();
			uniqueKeywordResult.add(sp);
		}
		return uniqueKeywordResult;
	}

	public Map<String, Set<ValDetails>> extractKeywordValue() {
		// Get the text into a String object
		extractedText = outputstream.toString();
		RawTextTokenNLP testDemo = new RawTextTokenNLP();

		Map<String, Set<String>> dataPointToKeywordsMap = new HashMap<String, Set<String>>();
		dataPointToKeywordsMap.put("Auditor Name", new HashSet<String>(Arrays.asList("Chartered Accountants", "financial audit", "cost auditing",
				"Public Accountants", "Registered Auditors", "Statutory Auditors", "Secretarial audit")));

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
			Pattern[] patterns = new Pattern[] { keywordPattern };
			regexMap.put(key, patterns);
		}

		// Keyword (String) => list of values (Span)
		Map<String, Set<ValDetails>> keywordToValuesMap = new HashMap<String, Set<ValDetails>>();

		// Reading line by line from the text file
		try {
			TextTokenizer tokenizer = new TextTokenizer("input/en-sent.bin", "input/en-token.bin",
					"input/en-ner-person.bin", extractedText);
			RegexNameFinder keywordRegexFinder = new RegexNameFinder(regexMap);
			String sentence = null;
			while ((sentence = tokenizer.getNextSentence()) != null) {
				//System.out.println("Sentence to be tokenized: " + sentence);
				// Parsing the words from each line
				String[] tokens = tokenizer.getTokensForSentence(sentence);
				Set<Span> keywordResult = getUniqueSpans(keywordRegexFinder.find(tokens));
				for (Span sp : keywordResult) {
					System.out.println("Keyword: " + sp.toString() + " length of span: " + sp.length());
					String keyword = sp.toString();
					String[] forwardTokens = getNextTokens(sp.getEnd(), 20, tokens);
					if (forwardTokens.length == 0) {
						continue;
					}
					for (String fwdToken : forwardTokens) {
						System.out.println("forward token: " + fwdToken);
					}
					Span[] names = tokenizer.getNameFinder().find(forwardTokens);
					if (names.length == 0) {
						continue;
					}
					for (Span dumpName: names) {
						System.out.println("Name: " + dumpName.toString());
					}
					Set<ValDetails> valueNames = new HashSet<ValDetails>();
					for (Span spName : names) {
						RawTextTokenNLP.ValDetails vd = testDemo.new ValDetails();
						vd.value = getSpanValue(spName, forwardTokens);
						vd.probability = spName.getProb();
						valueNames.add(vd);
					}

					Set<ValDetails> valList;
					if (keywordToValuesMap.containsKey(keyword)) {
						valList = keywordToValuesMap.get(keyword);
						for (ValDetails vl : valueNames) {
							valList.add(vl);
						}
					} else {
						valList = new HashSet<ValDetails>();
						for (ValDetails vl : valueNames) {
							valList.add(vl);
						}
						keywordToValuesMap.put(keyword, valList);
					}
				}
			}
			
			// iterate and display values
			System.out.println("Fetching Keys and corresponding [Multiple] Values");
			for (Map.Entry<String, Set<ValDetails>> entry : keywordToValuesMap.entrySet()) {
				String key = entry.getKey();
				Set<ValDetails> values = entry.getValue();
				for (ValDetails vl : values) {
					System.out.println("Keyword = " + key + ", Value = " + vl.value + " Probability: " + vl.probability);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keywordToValuesMap;
	}
	
	public void outputXML(Map<String, Set<ValDetails>> dataExtracted, String filename)
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
		/* dataExtracted.forEach((Key, Val) -> System.out.println(key) ); */
		for (Map.Entry<String, String> entry : keywordsToDatapointMap.entrySet()) {
			datapoints.appendChild(datapoint);
			Attr attr = doc.createAttribute("name");
			attr.setValue(entry.getValue());
			datapoint.setAttributeNode(attr);
		}
		for (Map.Entry<String, Set<ValDetails>> entry : dataExtracted.entrySet()) {
			String key = entry.getKey();
			Element keywordEle = doc.createElement("keyword");
			datapoint.appendChild(keywordEle);
			Attr attr = doc.createAttribute("name");
			attr.setValue(key);
			keywordEle.setAttributeNode(attr);
			Set<ValDetails> values = entry.getValue();
			for (ValDetails vl : values) {
				Element valueEle = doc.createElement("value");
				valueEle.appendChild(doc.createTextNode(vl.value));
				keywordEle.appendChild(valueEle);
			}
		}
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(
				new File("C:\\Users\\Atul.ISPUNLAPDE15009\\Documents\\output.xml"));
		transformer.transform(source,result);
		// Output to console for testing
		StreamResult consoleResult = new StreamResult(System.out);
		transformerFactory.setAttribute("indent-number",4);transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		transformer.transform(source,consoleResult);
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 1) {
			RawTextTokenNLP textExtractor = new RawTextTokenNLP();
			textExtractor.process(args[0]);
			Map<String, Set<ValDetails>> extractedData = textExtractor.extractKeywordValue();
			System.out.println(extractedData);
			for (Map.Entry<String, Set<ValDetails>> entry : extractedData.entrySet()) {
				String key = entry.getKey();
				Set<ValDetails> values = entry.getValue();
				for (ValDetails vl : values) {
					System.out
							.println("Keyword = " + key + ", Value = " + vl.value + " Probability: " + vl.probability);
				}
			}
			textExtractor.outputXML(extractedData, args[0]);
		} else {
			throw new Exception();
		}
	}
}
