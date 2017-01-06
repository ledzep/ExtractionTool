package com.innova.dataextractor;

import java.io.File;
import java.io.IOException;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

public class TextTokenizer {
	
	private SentenceModel sentenceModel = null;
	private TokenizerModel tokenModel = null;
	private TokenNameFinderModel tokenNameFinderModel = null;
	
	private SentenceDetector sentenceDetector = null;
	private Tokenizer tokenizer = null;
	private NameFinderME nameFinder = null;
	//private NameFinderME nameFinderByPerson = null;
	private String[] sentencesArr = {};
	private int currentSentence = 0; 
	
	public TextTokenizer(String sentModelFilename, String tokenModelFilename, String tokenNameFinderModelFilename, String content) throws InvalidFormatException, IOException {
		sentenceModel = new SentenceModel(new File(sentModelFilename));
		tokenModel = new TokenizerModel(new File(tokenModelFilename));
		tokenNameFinderModel = new TokenNameFinderModel(new File(tokenNameFinderModelFilename));
		sentenceDetector = new SentenceDetectorME(sentenceModel);
		tokenizer = new TokenizerME(tokenModel);
		nameFinder = new NameFinderME(tokenNameFinderModel);
		//nameFinderByPerson = new NameFinderME(model)
		sentencesArr = sentenceDetector.sentDetect(content);
		/*for (String str: sentencesArr) {
			System.out.println(str);
		}*/
	}
	
	// returns next sentence in the content or null, if no sentence remaining
	public String getNextSentence() {
		if (currentSentence < sentencesArr.length) {
			return sentencesArr[currentSentence++];
		} else {
			return null;
		}
	}
	
	// get tokens for sentence
	public String[] getTokensForSentence(String sentence) { 
		return tokenizer.tokenize(sentence);
	}
	
	public NameFinderME getNameFinder() {
		return nameFinder;
	}
}

