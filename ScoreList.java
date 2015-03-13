/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.*;

public class ScoreList {
  public class ScoreComparator implements Comparator<ScoreListEntry> {
    public int compare(ScoreListEntry o1, ScoreListEntry o2) {
      
          if (o2.score>o1.score)
            return 1;
          else if (o2.score==o1.score)
          {
              if (o1.externalid.compareTo(o2.externalid)>0)
                return 1;
              else
                return 0;
          }
          else
            return 0;     
    }
  }
  //  A little utilty class to create a <docid, score> object.

  public class ScoreListEntry {
    public int docid;
    public double score;
    public String externalid;
    public ScoreListEntry(int docid, double score) throws IOException {
      this.externalid = new String (QryEval.getExternalDocid(docid));
      this.docid = docid;
      this.score = score;
    }
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();
   
  double defaultBelieveScore;
  public double defaultMLE;
  public String field;
  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void sort()
  {
    Collections.sort(scores,new ScoreComparator());
  }
  

  
  public void add(int docid, double score) throws IOException {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }

}
