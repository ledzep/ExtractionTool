package com.innova.dataextractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static org.hamcrest.Matchers.containsString;


public class StringList {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArrayList<String> strName = new ArrayList<String>();
		Map<String, Set<String>> dataPointToKeywordsMap = new HashMap<String, Set<String>>();
		dataPointToKeywordsMap.put("Auditor Name", new HashSet<String>(Arrays.asList("Chartered Accountants", "financial audit", "cost auditing",
				"Public Accountants", "Registered Auditors", "Statutory Auditors", "Secretarial audit")));
		
		for (Map.Entry<String, Set<String>> entry : dataPointToKeywordsMap.entrySet()) {
			//String key = entry.getKey();
			for (String value : entry.getValue()) {
				strName.add(value);
			}
		}
		
		String [] items = strName.toArray(new String[strName.size()]);
		
		String sentence = "Cummins India Ltd., is one of the pioneers in product development in area of automotive and "
						+ "power generation. Company’s 2015-16 financial audit was done by Price Waterhouse. For "
						+ "same fiscal year cost auditing was undertaken by Anant J. Talaulicar, head of in-house "
						+ "auditing team. Financial Year 2015-16 Secretarial audit of the company for the was done Dr. "
						+ "K. R. Chandratre.";
		
		boolean flag = Arrays.stream(items).parallel().anyMatch(sentence::contains);
		
		boolean match = select(strName, having(on(String.class), containsString(sentence))).size()>0;
		
		System.out.println(flag);
		System.out.println(match);
	}

}
