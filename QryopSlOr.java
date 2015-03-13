import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class QryopSlOr extends QryopSl {

  @Override
  public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
    // TODO Auto-generated method stub
    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);

    return 0.0;
  }

  @Override
  public void add(Qryop a) throws IOException {
    // TODO Auto-generated method stub
    this.args.add(a);
  }

  @Override
  public QryResult evaluate(RetrievalModel r) throws IOException {
    // TODO Auto-generated method stub
    return (evaluateBoolean(r));

  }

  /**
   * Implemented #Or operator return a document if at least one of the query arguments occurs in the
   * document. Use the MAX function to combine the scores from the query arguments.
   * 
   * @param A
   *          retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */

  private QryResult evaluateBoolean(RetrievalModel r) throws IOException {
    // Initialization

    allocDaaTPtrs(r);
    QryResult result = new QryResult();

    // Sort the arguments so that the longest lists are first. This
    // improves the efficiency of exact-match OR without changing
    // the result.

    for (int i = 0; i < (this.daatPtrs.size() - 1); i++) {
      for (int j = i + 1; j < this.daatPtrs.size(); j++) {
        if (this.daatPtrs.get(i).scoreList.scores.size() < this.daatPtrs.get(j).scoreList.scores
                .size()) {
          ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
          this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
          this.daatPtrs.get(j).scoreList = tmpScoreList;
        }
      }
    }

    // use HashMap to memorize how many documents had be extracted from terms
    // And Max functions to update the Hashmp if the another term with more term frequencies also
    // appears in the same document
    HashMap<Integer, Double> hmap = new HashMap<Integer, Double>();
    for (int j = 0; j < this.daatPtrs.size(); j++) {
      DaaTPtr ptrj = this.daatPtrs.get(j);
      for (; ptrj.nextDoc < ptrj.scoreList.scores.size(); ptrj.nextDoc++) {
        if (hmap.containsKey(ptrj.scoreList.getDocid(ptrj.nextDoc)) == false) {
          hmap.put(ptrj.scoreList.getDocid(ptrj.nextDoc),
                  ptrj.scoreList.getDocidScore(ptrj.nextDoc));

        } else {
          if (ptrj.scoreList.getDocidScore(ptrj.nextDoc) > hmap.get(ptrj.scoreList
                  .getDocid(ptrj.nextDoc))) {
            hmap.remove(ptrj.scoreList.getDocid(ptrj.nextDoc));
            hmap.put(ptrj.scoreList.getDocid(ptrj.nextDoc),
                    ptrj.scoreList.getDocidScore(ptrj.nextDoc));
          }
        }

      }
    }

    // Update the Hashamp into the result
    Iterator<Integer> totSetItr = hmap.keySet().iterator();
    while (totSetItr.hasNext()) {
      Integer key = totSetItr.next();
      result.docScores.add(key, hmap.get(key));
    }
    freeDaaTPtrs();

    return result;
  }

  @Override
  public String toString() {
    return null;
  }

}
