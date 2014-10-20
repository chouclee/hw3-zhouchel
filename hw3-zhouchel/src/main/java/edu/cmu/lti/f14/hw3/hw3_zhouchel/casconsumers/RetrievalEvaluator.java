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
 * Process each sentence and classify them as query or document, put them into corresponding dictionary
 * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
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
      
      // convert the FSList to Collection
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      
      // if this document is a query
      if (doc.getRelevanceValue() == 99) {
        qIdList.add(queryId); // add the query id to qIdList
        
        // re-construct the query vector using HashMap
        queryVector = new HashMap<String, Integer>();
        for (Token token : tokenList) {
          queryVector.put(token.getText(), token.getFrequency());
        }
        queryMap.put(queryId, queryVector); // add query to queryMap
      }
      // if this document is to be evaluated
      else { 
        // re-construct the query vector using HashMap
        docVector = new HashMap<String, Integer>();
        for (Token token : tokenList) {
          docVector.put(token.getText(), token.getFrequency());
        }

        ArrayList<Doc> docList = null;
        if (!docMap.containsKey(queryId)) { // if this is the first document for this query id
          docList = new ArrayList<Doc>();
        } else {
          docList = docMap.get(queryId);  // we have handled other documents with the same query id
        }
        // construct new Doc object, add it to the document list
        docList.add(new Doc(queryId, doc.getRelevanceValue(), doc.getText(), docVector));
        docMap.put(queryId, docList); //  add document to docMap
      }

    }

  }
  @Override
  /**
   * @see org.apache.uima.collection.CasConsumer_ImplBase#destroy()
   */
  public void destroy() {
    try {
      writer.close(); // close file
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
      queryId = qIdList.get(i);       // get i th query
      docList = docMap.get(queryId);  // get all documents that have same query id
      queryVector = queryMap.get(queryId); // get query vector
      
      //compute the cosine similarity measure for each document in document list
      for (Doc doc : docList) {
        doc.score = computeCosineSimilarity(queryVector, doc.docVector); 
      }
      
      // sort the documents by similarity
      Collections.sort(docList);
      
      // write results to output file
      for (int j = 0; j < docList.size(); j++) {
        Doc doc = docList.get(j);
        
        if (doc.relevance == 1) {
          // the rank of retrieved sentence is j + 1
          output = String.format("cosine=%.4f\trank=%d\tqid=%d\trel=%d\t%s%n",
                  doc.score, (j+1), queryId, doc.relevance, doc.text);
          System.out.print(output);
          writer.write(output);
          rrList.add((double) 1 / (j + 1));
          break;
        }
      }
    }

    // compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr(rrList);
    output = String.format("MRR=%.4f", metric_mrr);
    writer.write(output);
    System.out.println(output);
  }

  /**
   * Compute Cosine similarity between a query and a document
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;
    double overlap = 0.0; // overlap of two vectors
    double normQuery = 0.0; // L-2 norm of query vector
    double normDoc = 0.0;  // L-2 norm of document vector
    
    // calculate L-2 norm of query vector and the overlap
    for (String token : queryVector.keySet()) {
      normQuery += Math.pow(queryVector.get(token), 2);
      if (docVector.containsKey(token))
        overlap += queryVector.get(token) * docVector.get(token);
    }
    
    // calculate L-2 norm of document vector
    for (String token : docVector.keySet()) {
      normDoc += Math.pow(docVector.get(token), 2);
    }
    
    // compute cosine similarity
    cosine_similarity = overlap / (Math.sqrt(normDoc) * Math.sqrt(normQuery));

    return cosine_similarity;
  }

  /**
   * Calculate Mean Reciprocal Rank
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
