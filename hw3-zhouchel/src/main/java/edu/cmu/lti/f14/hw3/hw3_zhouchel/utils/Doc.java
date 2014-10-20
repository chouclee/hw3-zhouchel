package edu.cmu.lti.f14.hw3.hw3_zhouchel.utils;

import java.util.HashMap;

/**
 * Represent a reference to a document that contains all related information.
 * 
 * @author zhouchel
 * 
 */
public class Doc implements Comparable<Doc> {
  /**
   * term frequency vector of this document
   */
  public HashMap<String, Integer> docVector;

  /**
   * query id
   */
  public int queryId;

  /**
   * relevance to the query, 1 stands for relevant and 0 means irrelevant.
   */
  public int relevance;

  /**
   * given a query, the similarity between this document and the query
   */
  public double score;

  /**
   * Sentence text
   */
  public String text;

  /**
   * Default Constructor
   */
  public Doc() {
    docVector = new HashMap<String, Integer>();
    queryId = 0;
    relevance = 0;
    score = 0.0;
  }

  /**
   * Constructor
   * 
   * @param queryId
   *          Query id
   * @param relevance
   *          Relevance to the query, 1 stands for relevant and 0 means irrelevant.
   * @param text
   *          Sentence text
   * @param docVector
   *          Term frequency vector of this document
   */
  public Doc(int queryId, int relevance, String text, HashMap<String, Integer> docVector) {
    this.queryId = queryId;
    this.text = text;
    this.docVector = docVector;
    this.relevance = relevance;
  }

  @Override
  /**
   * Implement the Comparable interface
   */
  public int compareTo(Doc o) {
    // TODO Auto-generated method stub
    if (this.score > o.score)
      return -1;
    if (this.score < o.score)
      return +1;
    return 0;
  }
}
