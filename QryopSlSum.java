import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;

public class QryopSlSum extends QryopSl {

  private DocLengthStore doclengthscore;

  public QryopSlSum(DocLengthStore d, Qryop... q) {
    this.doclengthscore = d;
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getSmallestCurrentDocid() {

    int nextDocid = Integer.MAX_VALUE;

    for (int i = 0; i < this.daatPtrs.size(); i++) {
      DaaTPtr ptri = this.daatPtrs.get(i);
      if (nextDocid > ptri.scoreList.getDocid(ptri.nextDoc))
        nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
    }

    return (nextDocid);
  }

  @Override
  public void add(Qryop q) throws IOException {
    // TODO Auto-generated method stub
    this.args.add(q);
  }

  @Override
  public QryResult evaluate(RetrievalModel r) throws IOException {
    // TODO Auto-generated method stub
    allocDaaTPtrs(r);
    QryResult result = new QryResult();

    while (this.daatPtrs.size() > 0) {

      int nextDocid = getSmallestCurrentDocid();

      double totScore = 0;
      for (int i = 0; i < this.daatPtrs.size(); i++) {
        DaaTPtr ptri = this.daatPtrs.get(i);

        if (ptri.scoreList.getDocid(ptri.nextDoc) == nextDocid) {
          totScore += ptri.scoreList.getDocidScore(ptri.nextDoc);
          ptri.nextDoc++;
        }
      }
      result.docScores.add(nextDocid, totScore);

      for (int i = this.daatPtrs.size() - 1; i >= 0; i--) {
        DaaTPtr ptri = this.daatPtrs.get(i);

        if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
          this.daatPtrs.remove(i);
        }
      }
    }

    return result;
  }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return null;
  }

}
