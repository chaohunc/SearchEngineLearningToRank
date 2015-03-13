import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

//import Qryop.DaaTPtr;

public class QryopIlWindow extends QryopIl {
  int windowsLen;

  public QryopIlWindow(int distance) {
    this.windowsLen = distance;

    // TODO Auto-generated constructor stub
  }

  @Override
  public QryResult evaluate(RetrievalModel r) throws IOException {
    // TODO Auto-generated method stub
    System.out.print("ddddddddd");
    return (evaluateBoolean(r));

  }

  @Override
  public void add(Qryop a) throws IOException {
    // TODO Auto-generated method stub
    this.args.add(a);
  }

  
  /**
   * Implemented #Near operator, which will extract the document if all of the query arguments 
   * occur in the document, in order, with no more than n-1 terms separating two adjacent terms. 
   * For example, #NEAR/2(a b c) matches "a b c", "a x b c", "a b x c", and "a x b x c", 
   * but not "a x x b c". The document's score will be the number of times the NEAR/n operator 
   * matched the document (i.e., its frequency).
   * 
   * @param  A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  
  public QryResult evaluateBoolean(RetrievalModel r) throws IOException {
    // Initialization
    allocDaaTPtrs(r);
    
    QryResult result = new QryResult();

    DaaTPtr ptr0 = this.daatPtrs.get(0);
    result.invertedList.field = new String(this.daatPtrs.get(0).invList.field);

    System.out.println(ptr0.scoreList);

    // 1. Similar to QryopSlAnd, first we need to find the document contain all query terms
    // 2. And then saved the Vector<Int> , which are positions of the terms appearing in the documents
    // 3. Search to check if the term is near the other term in desired distance and in the right order

    EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {

      int ptr0Docid = ptr0.invList.postings.get(ptr0.nextDoc).docid;
      // Do the other query arguments have the ptr0Docid?
      ArrayList<Vector<Integer>> nowdoc = new ArrayList<Vector<Integer>>();
      nowdoc.add(ptr0.invList.postings.get(ptr0.nextDoc).positions);
      for (int j = 1; j < this.daatPtrs.size(); j++) {

        DaaTPtr ptrj = this.daatPtrs.get(j);

        while (true) {
          if (ptrj.nextDoc >= ptrj.invList.postings.size())
            break EVALUATEDOCUMENTS; // No more docs can match
          else if (ptrj.invList.postings.get(ptrj.nextDoc).docid > ptr0Docid)
            continue EVALUATEDOCUMENTS; // The ptr0docid can't match.
          else if (ptrj.invList.postings.get(ptrj.nextDoc).docid < ptr0Docid)
            ptrj.nextDoc++; // Not yet at the right doc.
          else {
            nowdoc.add(ptrj.invList.postings.get(ptrj.nextDoc).positions);
            break; // ptrj matches ptr0Docid
          }
        }
      }

      // Search to check if the term is near the other term in desired distance and in the right
      // order
      int pos, findmatch = 0;
      List<Integer> list = new ArrayList<Integer>(); 
      int[] nowposPtr= new int [nowdoc.size()];
      for (int j=0;j<nowdoc.size();j++)
      {
          nowposPtr[j] = 0;
      }
      boolean end=false;
      while (end==false)
      {
        int minVal = nowdoc.get(0).get(nowposPtr[0]);
        int minWhichQuery = 0; 
        int maxVal = nowdoc.get(0).get(nowposPtr[0]);
        int maxWhichQuery = 0; 
        for (int z = 1; z < nowdoc.size(); z++) {
            if (nowdoc.get(z).get(nowposPtr[z])<minVal)
            {
               minVal = nowdoc.get(z).get(nowposPtr[z]);
               minWhichQuery = z; 
            }
            else if (nowdoc.get(z).get(nowposPtr[z])>maxVal)
            {
              maxVal = nowdoc.get(z).get(nowposPtr[z]);
              maxWhichQuery = z;
            }
        }
        if (maxVal - minVal +1 <= windowsLen)
        {
          if (!list.contains(maxVal))
            list.add(maxVal);
          for (int z=0;z<nowdoc.size();z++)
          {
            nowposPtr[z]++;
            if (nowposPtr[z]>=nowdoc.get(z).size())
              end=true;
          }
        }
        else
        {
        
          nowposPtr[minWhichQuery]++;
          if (nowposPtr[minWhichQuery]>=nowdoc.get(minWhichQuery).size())
            end = true;
        }       
      }
      
      if (list.size()!=0)
      { 
         // System.out.println(list);
          result.invertedList.appendPosting(ptr0Docid,list);
      }
        
    }

    freeDaaTPtrs();
    return result;

  }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return null;
  }

  public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
    // TODO Auto-generated method stub
    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);

    return 0.0;
  }

}
