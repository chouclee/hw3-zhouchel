package edu.cmu.lti.f14.hw3.hw3_zhouchel.casconsumers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_zhouchel.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_zhouchel.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_zhouchel.utils.Doc;
import edu.cmu.lti.f14.hw3.hw3_zhouchel.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  private HashMap<Integer, HashMap<String, Integer>> queryMap;

  private HashMap<Integer, ArrayList<Doc>> docMap;

  private BufferedWriter writer;

  /**
   * Name of configuration parameter that be set to the path of output file(optional)
   */
  public static final String PARAM_OUTPUT = "OutputFile";

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    // relList = new ArrayList<Integer>();

    // scoreList = new ArrayList<Double>();

    // rankList = new ArrayList<Integer>();
    queryMap = new HashMap<Integer, HashMap<String, Integer>>();
    docMap = new HashMap<Integer, ArrayList<Doc>>();

    String output = ((String) getConfigParameterValue(PARAM_OUTPUT)).trim();
    try {
      writer = new BufferedWriter(new FileWriter(output, false));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator<Annotation> it = jcas.getAnnotationIndex(Document.type).iterator();
    HashMap<String, Integer> queryVector = null;
    HashMap<String, Integer> docVector = null;

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // relList.add(doc.getRelevanceValue());
      int queryId = doc.getQueryID();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      if (doc.getRelevanceValue() == 99) {
        qIdList.add(queryId);
        queryVector = new HashMap<String, Integer>();
        for (Token token : tokenList) {
          queryVector.put(token.getText(), token.getFrequency());
        }
        queryMap.put(queryId, queryVector); // add query to queryMap
      } else {
        docVector = new HashMap<String, Integer>();
        for (Token token : tokenList) {
          docVector.put(token.getText(), token.getFrequency());
        }

        ArrayList<Doc> docList = null;
        if (!docMap.containsKey(queryId)) {
          docList = new ArrayList<Doc>();
        } else {
          docList = docMap.get(queryId);
        }
        docList.add(new Doc(queryId, doc.getRelevanceValue(), doc.getText(), docVector));
        docMap.put(queryId, docList);
      }
      // Do something useful here

    }

  }
  @Override
  public void destroy() {
    try {
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    // TODO :: compute the cosine similarity measure
    int queryId;
    ArrayList<Doc> docList;
    HashMap<String, Integer> queryVector;
    ArrayList<Double> rrList = new ArrayList<Double>();
    String output;
    
    for (int i = 0; i < qIdList.size(); i++) {
      queryId = qIdList.get(i);
      docList = docMap.get(queryId);
      queryVector = queryMap.get(queryId);
      for (Doc doc : docList) {
        doc.score = computeCosineSimilarity(queryVector, doc.docVector);
      }
      Collections.sort(docList);
      for (int j = 0; j < docList.size(); j++) {
        Doc doc = docList.get(j);
        if (doc.relevance == 1) {
          output = String.format("coisne=%.4f\trank=%d\tqid=%d\trel=%d\t%s%n",
                  doc.score, (j+1), queryId, doc.relevance, doc.text);
          System.out.print(output);
          writer.write(output);
          // System.out.println("cosine:" + doc.score+"\t" + "rank=" + (j+1) + "\t" +
          // "qId:"+doc.queryId +
          // "\t"+"relevance:"+doc.relevance);
          rrList.add((double) 1 / (j + 1));
          break;
        }
      }
    }

    // TODO :: compute the rank of retrieved sentences

    // TODO :: compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr(rrList);
    output = String.format("MRR=%.4f", metric_mrr);
    writer.write(output);
    System.out.println(output);
  }

  /**
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;

    // TODO :: compute cosine similarity between two sentences
    double overlap = 0.0, normQuery = 0.0, normDoc = 0.0;
    for (String token : queryVector.keySet()) {
      normQuery += Math.pow(queryVector.get(token), 2);
      if (docVector.containsKey(token))
        overlap += queryVector.get(token) * docVector.get(token);
    }

    for (String token : docVector.keySet()) {
      normDoc += Math.pow(docVector.get(token), 2);
    }

    cosine_similarity = overlap / (Math.sqrt(normDoc) * Math.sqrt(normQuery));

    return cosine_similarity;
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr(ArrayList<Double> rrList) {
    double metric_mrr = 0.0;

    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection

    for (Double rr : rrList)
      metric_mrr += rr;
    return metric_mrr / rrList.size();
  }

}
