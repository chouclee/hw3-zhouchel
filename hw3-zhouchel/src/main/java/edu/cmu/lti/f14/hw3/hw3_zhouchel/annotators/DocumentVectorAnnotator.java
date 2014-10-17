package edu.cmu.lti.f14.hw3.hw3_zhouchel.annotators;

import java.util.*;
import java.util.Map.Entry;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.cas.FSList;

import edu.cmu.lti.f14.hw3.hw3_zhouchel.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_zhouchel.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_zhouchel.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex(Document.type).iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}

	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		//TO DO: construct a vector of tokens and update the tokenList in CAS
    //TO DO: use tokenize0 from above 
		List<String> tokens = tokenize0(docText);
	  HashMap<String, Integer> termFreq = new HashMap<String, Integer>();
		
		for (String token : tokens) {
		  if (termFreq.containsKey(token))
		    termFreq.put(token, termFreq.get(token) + 1);
		  else
		    termFreq.put(token, 1);
		}
		
		
		ArrayList<Token> tokenArrayList = new ArrayList<Token>();
		for (Entry<String, Integer> entry : termFreq.entrySet()) {
		  Token token = new Token(jcas);
		  token.setText(entry.getKey());
		  token.setFrequency(entry.getValue());
		  tokenArrayList.add(token);
		}
		FSList tokenList = Utils.fromCollectionToFSList(jcas, tokenArrayList);
		doc.setTokenList(tokenList);
	}

}
