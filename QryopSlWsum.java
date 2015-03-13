import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;



public class QryopSlWsum extends QryopSl {

  private DocLengthStore doclengthscore;
  ArrayList<Double> weightList = new ArrayList<Double>();
  public QryopSlWsum(DocLengthStore d, Qryop... q) {
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
      if (ptri.nextDoc < ptri.scoreList.scores.size()
              && nextDocid > ptri.scoreList.getDocid(ptri.nextDoc))
        nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
    }

    return (nextDocid);
  }


  @Override
  public QryResult evaluate(RetrievalModel r) throws IOException {
    // TODO Auto-generated method stub
    allocDaaTPtrs(r);
    QryResult result = new QryResult();
    double weightSum=0;
    double[] divisor = new double[weightList.size()];
    for (int i=0;i<weightList.size();i++)
      weightSum += weightList.get(i);
    for (int i=0;i<weightList.size();i++)
    {
      divisor[i] =  weightList.get(i)/weightSum;
      System.out.println("divisor "+i+ " "+divisor[i] );
    }

    
    
    result.docScores.defaultMLE = 1;
    for (int i = 0; i < this.daatPtrs.size(); i++) {
      DaaTPtr ptri = this.daatPtrs.get(i);
      //result.docScores.defaultMLE *= ptri.scoreList.defaultMLE* divisor[i];
     result.docScores.defaultMLE *= Math.pow(ptri.scoreList.defaultMLE, divisor[i]);
    //  System.out.println("d "+ ptri.scoreList.defaultMLE* divisor[i]);
    
    }
    //sSystem.out.println("dMLE "+ result.docScores.defaultMLE);
    
    int flag = 0;
    while (flag == 0) {

      int nextDocid = getSmallestCurrentDocid();

      double indriScore = 0;

      for (int i = 0; i < this.daatPtrs.size(); i++) {
        DaaTPtr ptri = this.daatPtrs.get(i);
          
        if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
          //System.out.println(QryopSlScore.getDefaultScoreFromMLE((RetrievalModelIndri) r,
          //        ptri.scoreList.defaultMLE, nextDocid, ptri.scoreList.field));
          indriScore += QryopSlScore.getDefaultScoreFromMLE((RetrievalModelIndri) r,
                  ptri.scoreList.defaultMLE, nextDocid, ptri.scoreList.field)* divisor[i];
       //   if (QryEval.getExternalDocid(nextDocid).equals("clueweb09-en0010-61-35743"))
      //       System.out.println("default"+ indriScore);
          continue;
        }
        if (ptri.scoreList.getDocid(ptri.nextDoc) == nextDocid) {
          indriScore += ptri.scoreList.getDocidScore(ptri.nextDoc)* divisor[i];
          ptri.nextDoc++;
        } else {
          indriScore += QryopSlScore.getDefaultScoreFromMLE((RetrievalModelIndri) r,
                  ptri.scoreList.defaultMLE, nextDocid, ptri.scoreList.field)* divisor[i] ;
        }
      }
     // System.out.println("indriScore" + indriScore);
      if (nextDocid!=Integer.MAX_VALUE)
      result.docScores.add(nextDocid, indriScore);
      flag = 1;
      for (int i = this.daatPtrs.size() - 1; i >= 0; i--) {
        DaaTPtr ptri = this.daatPtrs.get(i);

        if (ptri.nextDoc < ptri.scoreList.scores.size()) {
          flag = 0;
        }
      }
    }
    if (this.daatPtrs!=null && this.daatPtrs.size()!=0)
    {
    result.docScores.field = new String(this.daatPtrs.get(0).scoreList.field);
    return result;
    }
    else
    {
      result.docScores.field = new String("body");
   
      return result;
    }
    }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void add(Qryop q) throws IOException {
    // TODO Auto-generated method stub
    this.args.add(q);
  }

  public void addweight(double weight) {
    // TODO Auto-generated method stub
    this.weightList.add(weight);
  }

}
