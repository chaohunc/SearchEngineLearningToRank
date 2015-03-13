/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.MultiFields;

/**
 * DocLengthStore is used to access the document lengths of indexed docs.
 */
public class DocLengthStore  {
  
  private IndexReader reader;
  private  Map<String, NumericDocValues> values = new HashMap<String, NumericDocValues>();
  public  Map<String, Double> avglen = new HashMap<String, Double>();
  
  /**
   * @param reader IndexReader object created in {@link QryEval}.
   */
  public DocLengthStore(IndexReader reader) throws IOException {
    this.reader = reader;
    
    for (String field : MultiFields.getIndexedFields(reader)) {
      this.values.put(field, MultiDocValues.getNormValues(reader, field));    
      if (MultiDocValues.getNormValues(reader, field)!=null)
        avglen.put(field, calFieldAvgLen(field));
    }
  }

  
  private Double calFieldAvgLen(String field) {
    // TODO Auto-generated method stub
    double totlen=0;
    for (int i=0;i<QryEval.READER.numDocs();i++)
    {
         totlen += values.get(field).get(i);
    }
    totlen /=  QryEval.READER.numDocs();
    return totlen;
  }


  /**
   * Returns the length of the specified field in the specified document.
   *
   * @param fieldname Name of field to access lengths. "body" is the default
   * field.
   * @param docid The internal docid in the lucene index.
   */
  public long getDocLength(String fieldname, int docid) throws IOException {
    //System.out.println(docid);
    return values.get(fieldname).get(docid);
  }
}
