package com.innova.dataextractor;

import java.io.File;
import java.io.IOException;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Span;

public class NameDetector {
	
	

	public static void main(String[] args) throws IOException {
		POSModel posModel = new POSModel(new File("input/en-pos-maxent.bin"));
		TokenNameFinderModel tokenNameFinderModel = new TokenNameFinderModel(new File("input/en-ner-percentage.bin"));
		NameFinderME nameFinder = new NameFinderME(tokenNameFinderModel);
		POSTaggerME posTagger = new POSTaggerME(posModel);
		String[] forwardTokens = {"Ananth", "J.", "Talaulicar"};
		String[] posToken = posTagger.tag(forwardTokens);
		for (String fwdToken : forwardTokens) {
			System.out.println("forward token: " + fwdToken);
		}
		Span[] names = nameFinder.find(forwardTokens);
		
		for (Span dumpName: names) {
			System.out.println("Name: " + dumpName.toString());
		}
		
		for (String posLabel: posToken) {
			System.out.println(posLabel);
		}
	}
}
