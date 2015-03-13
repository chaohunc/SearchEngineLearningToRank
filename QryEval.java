/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class QryEval {

  static String usage = "Usage:  java " + System.getProperty("sun.java.command") + " paramFile\n\n";

  // The index file reader is accessible via a global variable. This
  // isn't great programming style, but the alternative is for every
  // query operator to store or pass this value, which creates its
  // own headaches.

  public static IndexReader READER;

  static DocLengthStore doclenStore;

  // Create and configure an English analyzer that will be used for
  // query parsing.

  public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
          Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }

  /**
   * @param args
   *          The only argument is the path to the parameter file.
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    long startTime = System.nanoTime();

    // load Sample.param from local if args == null
    // args[0] = "./bin/Sample.param";
    if (args.length == 0) {
      args = new String[1];
      args[0] = "./bin/Sample.param";
    }

    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }

    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    scan.close();

    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));
    System.out.println(QryEval.READER.maxDoc());
    System.out.println(QryEval.READER.numDocs());

    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }

    doclenStore = new DocLengthStore(READER);
    RetrievalModel model = null;

    if (params.get("retrievalAlgorithm").equals("UnrankedBoolean"))
      model = new RetrievalModelUnrankedBoolean();
    else if (params.get("retrievalAlgorithm").equals("RankedBoolean"))
      model = new RetrievalModelRankedBoolean();
    else if (params.get("retrievalAlgorithm").equals("BM25")) {
      RetrievalModelBM25 m2 = new RetrievalModelBM25();
      m2.b = Double.parseDouble(params.get("BM25:b"));
      m2.k1 = Double.parseDouble(params.get("BM25:k_1"));
      model = m2;
    } else if (params.get("retrievalAlgorithm").equals("Indri")) {
      RetrievalModelIndri m2 = new RetrievalModelIndri();
      m2.lambda = Double.parseDouble(params.get("Indri:lambda"));
      m2.mu = Double.parseDouble(params.get("Indri:mu"));
      model = m2;
    }

    // query expansion

    if (params.get("fb") != null && params.get("fb").equals("true")) {
      model.fb = true;
      model.fbDocs = Integer.parseInt(params.get("fbDocs"));
      model.fbTerms = Integer.parseInt(params.get("fbTerms"));
      model.fbMu = Double.parseDouble(params.get("fbMu"));
      model.fbOrigWeight = Double.parseDouble(params.get("fbOrigWeight"));
      model.fbInitialRankingFile = params.get("fbInitialRankingFile");
      model.fbExpansionQueryFile = params.get("fbExpansionQueryFile");
    }
    if (params.get("retrievalAlgorithm").equals("letor")) {
      buildTrainingDataForletor(params);
      buildSVMmodel(params);
      RetrievalModelBM25 m2 = new RetrievalModelBM25();
      m2.b = Double.parseDouble(params.get("BM25:b"));
      m2.k1 = Double.parseDouble(params.get("BM25:k_1"));
      model = m2;
    }
    /*
     * The code below is an unorganized set of examples that show you different ways of accessing
     * the index. Some of these are only useful in HW2 or HW3.
     */

    /*
     * // Lookup the document length of the body field of doc 0.
     * System.out.println(s.getDocLength("body", 0));
     * 
     * // How to use the term vector. TermVector tv = new TermVector(1, "body");
     * System.out.println(tv.stemString(100)); // get the string for the 100th stem
     * System.out.println(tv.stemDf(100)); // get its df System.out.println(tv.totalStemFreq(100));
     * // get its ctf
     */

    /**
     * The index is open. Start evaluating queries. The examples below show query trees for two
     * simple queries. These are meant to illustrate how query nodes are created and connected.
     * However your software will not create queries like this. Your software will use a query
     * parser. See parseQuery.
     * 
     * The general pattern is to tokenize the query term (so that it gets converted to lowercase,
     * stopped, stemmed, etc), create a Term node to fetch the inverted list, create a Score node to
     * convert an inverted list to a score list, evaluate the query, and print results.
     * 
     * Modify the software so that you read a query from a file, parse it, and form the query tree
     * automatically.
     */

    // A different way to create the previous query. This doesn't use
    // a stack, but it may make it easier to see how you would parse a
    // query with a stack-based architecture.

    // Using the example query parser. Notice that this does no
    // lexical processing of query terms. Add that to the query
    // parser.

    BufferedReader in = new BufferedReader(new FileReader(params.get("queryFilePath")));
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
            params.get("trecEvalOutputPath"))));

    String tempStr;

    BufferedWriter queryExpansionwriter = null;
    if (params.get("fbExpansionQueryFile") != null)
      queryExpansionwriter = new BufferedWriter(
              new FileWriter(new File(model.fbExpansionQueryFile)));
    
    BufferedWriter testingFVwriter = new BufferedWriter(new FileWriter(new File(
            params.get("letor:testingFeatureVectorsFile"))));
    
    HashMap<String,Integer> relDocument = new HashMap<String,Integer>();
    ArrayList<String> queryIDToQuery = new ArrayList<String>();
    
    while ((tempStr = in.readLine()) != null) {
      // print the query in processing
      System.out.println(tempStr);
      queryIDToQuery.add(tempStr);
      String[] st = tempStr.split(":");
      String queryID = st[0];
      Qryop qTree;
      String query = new String(st[1]);

      if (params.get("retrievalAlgorithm").equals("Indri"))
        query = "#AND ( " + query + " )";
      else if (params.get("retrievalAlgorithm").equals("letor"))
        query = "#SUM ( " + query + " )";
          
      qTree = parseQuery(query, model);

      // printResults(query, qTree.evaluate(model));
      QryResult queryResult = qTree.evaluate(model);

      if (params.get("retrievalAlgorithm").equals("letor"))
      {
        int rank;
        if (queryResult.docScores.scores.size() > 0) {
          queryResult.docScores.sort();
          for (int i = 0; i < queryResult.docScores.scores.size(); i++) {
            rank = Math.min(100,queryResult.docScores.scores.size())-i;
            relDocument.put(queryID+"_"+getExternalDocid(queryResult.docScores.getDocid(i)), rank);
//            writer.write(getExternalDocid(result.docScores.getDocid(i)) + " " + rank
//                    + " " + result.docScores.getDocidScore(i) + " run-1\n");
            if (i >= 99)
              break;
          }
        }
      }
      // if fb is true , do query expansion
      boolean fb = false;
      if (model.fb == true) {
        fb = true;
        String queryExpansionStr = queryExpansion(queryResult, model, queryID);
        System.out.println(queryExpansionStr);
        queryExpansionwriter.write(queryID + ": " + queryExpansionStr + "\n");
        query = "#Wand( " + model.fbOrigWeight + " " + query + " " + (1 - model.fbOrigWeight) + " "
                + queryExpansionStr + ")";
        model.fb = false;
        System.out.println(query);
        qTree = parseQuery(query, model);
        queryResult = qTree.evaluate(model);
      }
      model.fb = fb;
      // write results to file

      writeResults(queryID, query, queryResult, writer);

    }
    ArrayList<Integer> doclist = new  ArrayList<Integer> ();
    if (params.get("retrievalAlgorithm").equals("letor"))
    {
      doclist = buildTestingDataForletor(params, relDocument, queryIDToQuery
            , testingFVwriter);
      testingSVM(params);
    }
    testingFVwriter.flush();
    testingFVwriter.close();
    if (queryExpansionwriter != null) {
      queryExpansionwriter.flush();
      queryExpansionwriter.close();
    }
    in.close();
    writer.close();

    // Later HW assignments will use more RAM, so you want to be aware
    // of how much memory your program uses.
    if (params.get("retrievalAlgorithm").equals("letor"))
    {
      writer = new BufferedWriter(new FileWriter(new File(
              params.get("trecEvalOutputPath"))));
      BufferedReader testingFV = new BufferedReader(new FileReader(params.get("letor:testingFeatureVectorsFile")));
      BufferedReader testingDS = new BufferedReader(new FileReader(params.get("letor:testingDocumentScores")));
      String tempFVStr, tempDSStr;
      QryResult resultQ = new QryResult();
      int nowDoclist=0;
      String oldQueryID = new String();
      while ((tempFVStr = testingFV.readLine())!=null)
      {
        String newQueryID = tempFVStr.split(" ")[1].split(":")[1];
        if(nowDoclist!=0&& !newQueryID.equals(oldQueryID))
        {
          resultQ.docScores.sort();
          for (int i=0;i<resultQ.docScores.scores.size();i++)
          {
            int rank = i + 1;
            if (rank > 100)
              break;
            writer.write(oldQueryID + " Q0 " + getExternalDocid(resultQ.docScores.getDocid(i)) + " " + rank
                + " " + resultQ.docScores.getDocidScore(i) + " run-1\n");
           // System.out.println(oldQueryID + " Q0 " + getExternalDocid(resultQ.docScores.getDocid(i)) + " " + rank
           //         + " " + resultQ.docScores.getDocidScore(i) + " run-1\n");
          }
          resultQ = new QryResult();
        }
        tempDSStr = testingDS.readLine();
        System.out.println(doclist.get(nowDoclist)+" "+Double.parseDouble(tempDSStr));
        resultQ.docScores.add(doclist.get(nowDoclist), Double.parseDouble(tempDSStr));
        
        oldQueryID = newQueryID;
        nowDoclist++;
      }
      resultQ.docScores.sort();
      for (int i=0;i<resultQ.docScores.scores.size();i++)
      {
        int rank = i + 1;
        if (rank > 100)
          break;
        writer.write(oldQueryID + " Q0 " + getExternalDocid(resultQ.docScores.getDocid(i)) + " " + rank
            + " " + resultQ.docScores.getDocidScore(i) + " run-1\n");
      //  System.out.println(oldQueryID + " Q0 " + getExternalDocid(resultQ.docScores.getDocid(i)) + " " + rank
      //          + " " + resultQ.docScores.getDocidScore(i) + " run-1\n");
      }
      writer.flush();
      writer.close();
    }
    printMemoryUsage(false);
    long estimatedTime = System.nanoTime() - startTime;
    System.out.print("Time elasped: " + estimatedTime + "nanaseconds");
    /*
     * used for experimenting System.out.println("body: "+ body); System.out.println("url: "+ url);
     * System.out.println("inlink: "+ inlink); System.out.println("title: "+ title);
     * System.out.println("keywords: "+ keywords);
     */
  }

  private static void testingSVM(Map<String, String> params) throws Exception {
    // TODO Auto-generated method stub
    //svm_rank_classify example3/train.dat example3/model example3/predictions.train
    Process cmdProc = Runtime.getRuntime().exec(
            new String[] { params.get("letor:svmRankClassifyPath"),   params.get("letor:testingFeatureVectorsFile") ,
                    params.get("letor:svmRankModelFile") ,params.get("letor:testingDocumentScores") });
    BufferedReader stdoutReader = new BufferedReader(
            new InputStreamReader(cmdProc.getInputStream()));
        String line;
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
            new InputStreamReader(cmdProc.getErrorStream()));

        // get the return value from the executable. 0 means success, non-zero 
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
          throw new Exception("SVM Rank crashed.");
        }
  }

  private static void buildSVMmodel(Map<String, String> params) throws Exception {
    // TODO Auto-generated method stub
  //  Process process = new ProcessBuilder( "/Users/chaohunc/Documents/workspace/SearchEngine/svm_rank_learn","-c 0.0001 trainingFeatureVectorsFile.txt model").start();
    // runs svm_rank_learn from within Java to train the model
    // execPath is the location of the svm_rank_learn utility, 
    // which is specified by letor:svmRankLearnPath in the parameter file.
    // FEAT_GEN.c is the value of the letor:c parameter.
    Process cmdProc = Runtime.getRuntime().exec(
        new String[] { params.get("letor:svmRankLearnPath"), "-c", params.get("letor:svmRankParamC") , params.get("letor:trainingFeatureVectorsFile") ,
                params.get("letor:svmRankModelFile")   });
    // The stdout/stderr consuming code MUST be included.
    // It prevents the OS from running out of output buffer space and stalling.

    // consume stdout and print it out for debugging purposes
    BufferedReader stdoutReader = new BufferedReader(
        new InputStreamReader(cmdProc.getInputStream()));
    String line;

    // consume stderr and print it for debugging purposes
    BufferedReader stderrReader = new BufferedReader(
        new InputStreamReader(cmdProc.getErrorStream()));


    // get the return value from the executable. 0 means success, non-zero 
    // indicates a problem
    int retValue = cmdProc.waitFor();
    if (retValue != 0) {
      throw new Exception("SVM Rank crashed.");
    }
  }

  private static ArrayList<Integer> buildTestingDataForletor(Map<String, String> params, HashMap<String,Integer> relDocument, ArrayList<String> queryIDToQuery
          ,BufferedWriter writer)
          throws Exception {
    // TODO Auto-generated method stub
    HashMap<String, Double> pRankDocument = new HashMap<String, Double>();

   //   relDocument.put(strArr[0] + "_" + strArr[2], Integer.parseInt(strArr[3]));
    String tempStr2;
    BufferedReader pin = new BufferedReader(new FileReader(params.get("letor:pageRankFile")));
    while ((tempStr2 = pin.readLine()) != null) 
    {
     // System.out.println(tempStr);
      String[] strArr = tempStr2.split("\t");
      pRankDocument.put(strArr[0], Double.parseDouble(strArr[1]));
    }

    ArrayList<ArrayList<Double>> totlist = new ArrayList<ArrayList<Double>> ();
    
    for (String tempStr:queryIDToQuery)
    { // print the query in processing
      System.out.println(tempStr);
      String[] st = tempStr.split(":");
      String queryID = st[0];
      String query = new String(st[1]);

      HashMap<Integer, ArrayList<Double>> hmap = new HashMap<Integer, ArrayList<Double>>();
      for (Entry<String, Integer> relList : relDocument.entrySet()) {
        String[] strArr = relList.getKey().split("_");
        if (strArr[0].equals(st[0])) {
          hmap = buildFeature0to4(hmap,strArr[1],relList,pRankDocument, queryID);          
        }
      }
      hmap = buildFeatures5to17(hmap,params,query); 
      /*
      for (Entry<Integer,ArrayList<Double>> en : hmap.entrySet() )
      {
          System.out.println(en.getKey() + " "+ en.getValue().toString());
      }
*/
      hmap = normalizeHmap(hmap,params);
      for (Entry<Integer,ArrayList<Double>> en: hmap.entrySet()) 
      {
        en.getValue().set(0, 0.0);
        totlist.add(en.getValue());          
      }
    }
    ArrayList<Integer> doclist= writeHmapToFile(totlist,writer);
    return doclist;
    // printResults(query, qTree.evaluate(model));
  }

  private static void buildTrainingDataForletor(Map<String, String> params)
          throws Exception {
    // TODO Auto-generated method stub
    HashMap<String, Integer> relDocument = new HashMap<String, Integer>();
    HashMap<String, Double> pRankDocument = new HashMap<String, Double>();

    BufferedReader br = new BufferedReader(new FileReader(params.get("letor:trainingQrelsFile")));
    String tempStr;
    while ((tempStr = br.readLine()) != null) {
      String[] strArr = tempStr.split(" ");
      relDocument.put(strArr[0] + "_" + strArr[2], Integer.parseInt(strArr[3]));
    }
    br.close();

    BufferedReader pin = new BufferedReader(new FileReader(params.get("letor:pageRankFile")));
    while ((tempStr = pin.readLine()) != null) 
    {
     // System.out.println(tempStr);
      String[] strArr = tempStr.split("\t");
      pRankDocument.put(strArr[0], Double.parseDouble(strArr[1]));
    }

    br.close();
    BufferedReader in = new BufferedReader(new FileReader(params.get("letor:trainingQueryFile")));
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
            params.get("letor:trainingFeatureVectorsFile"))));
    ArrayList<ArrayList<Double>> totlist = new ArrayList<ArrayList<Double>> ();
    
    while ((tempStr = in.readLine()) != null) {
      // print the query in processing
   //   System.out.println(tempStr);
      String[] st = tempStr.split(":");
      String queryID = st[0];
      String query = new String(st[1]);

      HashMap<Integer, ArrayList<Double>> hmap = new HashMap<Integer, ArrayList<Double>>();
      for (Entry<String, Integer> relList : relDocument.entrySet()) {
        String[] strArr = relList.getKey().split("_");
        if (strArr[0].equals(st[0])) {
          hmap = buildFeature0to4(hmap,strArr[1],relList,pRankDocument, queryID);          
        }
      }
      hmap = buildFeatures5to17(hmap,params,query); 
      hmap = normalizeHmap(hmap,params);
      for (Entry<Integer,ArrayList<Double>> en: hmap.entrySet()) 
        totlist.add(en.getValue());          
    }
    writeHmapToFile(totlist,writer);
    in.close();
    // printResults(query, qTree.evaluate(model));
  }

  private static ArrayList<Integer>  writeHmapToFile(ArrayList<ArrayList<Double>> totlist, BufferedWriter writer) throws IOException {
    // TODO Auto-generated method stub
    
  //  for (int j=0;j<totlist.size();j++)
  //    System.out.println(j+" "+totlist.get(j).toString()+" # "+ getExternalDocid(totlist.get(j).get(2).intValue()));
         
    ArrayList<Integer> doclist = new ArrayList<Integer> ();
//    DecimalFormat df = new DecimalFormat("#.########");      
    for (int z=0;z<totlist.size();z++)
    {
      ArrayList<Double> featurelistTemp =totlist.get(z); 
      StringBuffer strToAppend= new StringBuffer();
      for (int i=0;i<featurelistTemp.size();i++)
      {
        if (i==0)           
          strToAppend.append(featurelistTemp.get(i).intValue());
        else if (i==1)
        {
          strToAppend.append(" qid:"+featurelistTemp.get(i).intValue());
        }
        else if (i==2) //read docID
        {
          doclist.add(featurelistTemp.get(i).intValue());
        }
        else 
        {
          strToAppend.append(" "+(i-2)+":"+Double.valueOf(featurelistTemp.get(i)));            
        } //        for ()
      }
      writer.write(strToAppend.toString()+" # " + getExternalDocid(featurelistTemp.get(2).intValue())+"\n");
      
    }
    writer.flush();
    return doclist;
  }

  private static HashMap<Integer, ArrayList<Double>> normalizeHmap(
          HashMap<Integer, ArrayList<Double>> hmap, Map<String, String> params) {
    int sizeFL=0;
    for (Entry<Integer,ArrayList<Double>> en:hmap.entrySet())
      {
      sizeFL = en.getValue().size();
      break;
      }
    HashSet<Integer> featureDisable= new  HashSet<Integer>();
    if (params.get("letor:featureDisable")!=null)
    {
      String[] featureDisableStr = params.get("letor:featureDisable").split(",");
      for (String str: featureDisableStr)
        featureDisable.add(Integer.parseInt(str));
      
    }
    for (int i=sizeFL-1;i>=3;i--)
    {
         double min = Double.MAX_VALUE;
         double max = 0;
         for (Entry<Integer,ArrayList<Double>> en: hmap.entrySet())
           {                 
               if (en.getValue().get(i).isNaN()==true)
                 continue;
               if (en.getValue().get(i)<min)
                   min = en.getValue().get(i);
               if (en.getValue().get(i)>max)
                   max = en.getValue().get(i);
           }
        // System.out.println("minV "+i+" "+min);
        // System.out.println("maxV "+i+" "+max);
         for (Entry<Integer,ArrayList<Double>> en: hmap.entrySet())
         {
             double nowVal=  en.getValue().get(i);

             if (en.getValue().get(i).isNaN()==true)
               en.getValue().set(i, (double) 0);
             else if (max-min!=0)
             {
               en.getValue().set(i, (nowVal-min)/(max-min));
             }
             else
               en.getValue().set(i, (double) 0);
             if (featureDisable.contains(i-2))
             {
                 en.getValue().remove(i);
             }
          }
    }   
    return hmap;
  }

  private static HashMap<Integer, ArrayList<Double>> buildFeature0to4(HashMap<Integer, ArrayList<Double>> hmap, String externaldocid,
          Entry<String, Integer> relList, HashMap<String, Double> pRankDocument,String queryID) throws Exception {
    // TODO Auto-generated method stub
    // classRank
    ArrayList<Double> featurelist = new ArrayList<Double>();

    int docid = getInternalDocid(externaldocid);
    featurelist.add((double)relList.getValue());
    Document d = QryEval.READER.document(docid);
    int spamscore = Integer.parseInt(d.get("score"));
    
    //queryID
    featurelist.add(Double.parseDouble(queryID));

    //docID
    featurelist.add((double)docid);

    // f1
    featurelist.add((double) spamscore);
    String rawUrl = d.get("rawUrl");
    int count = 0;
    for (int j = 0; j < rawUrl.length(); j++)
      if (rawUrl.charAt(j) == '/')
        count++;
    // f2
    featurelist.add((double) count);
    // f3
    if (rawUrl.contains("wikipedia.org"))
      featurelist.add((double) 1);
    else
      featurelist.add((double) 0);
    // f4
    if (pRankDocument.containsKey(externaldocid))
      featurelist.add(pRankDocument.get(externaldocid));
    else 
      featurelist.add((double) 0/0);
      
    hmap.put(docid, featurelist);
    return hmap;

  }

  private static HashMap<Integer, ArrayList<Double>> buildFeatures5to17(HashMap<Integer, ArrayList<Double>> hmap, Map<String,String> params,String query) throws IOException {
    // TODO Auto-generated method stub
    Qryop qTree;

    RetrievalModelBM25 bm25 = new RetrievalModelBM25();
    bm25.b = Double.parseDouble(params.get("BM25:b"));
    bm25.k1 = Double.parseDouble(params.get("BM25:k_1"));

    
    
    RetrievalModelIndri indri = new RetrievalModelIndri();
    indri.lambda = Double.parseDouble(params.get("Indri:lambda"));
    indri.mu = Double.parseDouble(params.get("Indri:mu"));

    RetrievalModelRankedBoolean rankedBoolean = new RetrievalModelRankedBoolean();
    
//    String[] attributes = { "", "title", "attri", "inlink" };
    String[] attributes = new String[4];
    attributes[0]="body";
    attributes[1]="title";
    attributes[2]="url";
    attributes[3]="inlink";
    String[] querySplit;
    for (String attr : attributes) {
      String queryFinal = new String();
      for (String querySingle : query.split(" "))
        queryFinal = queryFinal + " " + querySingle + "." + attr;
      System.out.println("Attr" + queryFinal + " "+attr);
      qTree = parseQuery("#SUM ( " + queryFinal + " )", bm25);
      
      QryResult queryResultBM25 = qTree.evaluate(bm25);
      HashMap <Integer,Double> BM25score = new HashMap <Integer,Double>();
      for (int i=0;i< queryResultBM25.docScores.scores.size();i++)
          BM25score.put(queryResultBM25.docScores.getDocid(i),queryResultBM25.docScores.getDocidScore(i));
          


      //f6    
      HashMap <Integer,Double> Indriscore = new HashMap <Integer,Double>();
      qTree = parseQuery("#AND(" + queryFinal + ")", indri);
      QryResult queryResultIndri = qTree.evaluate(indri);
      for (int i=0;i< queryResultIndri.docScores.scores.size();i++)
        Indriscore.put(queryResultIndri.docScores.getDocid(i),queryResultIndri.docScores.getDocidScore(i));

      //f7
      HashMap<Integer, Integer> docidToTermOverlap = new HashMap<Integer, Integer>();

      int querySize = 0;

      
      for (String querySingle : query.split(" ")) {
        System.out.println(querySingle);
//        System.out.println(tokenizeQuery(querySingle)[0].toString());
        if (tokenizeQuery(querySingle).length==0)
            continue;
        QryopIlTerm term = new QryopIlTerm(tokenizeQuery(querySingle)[0], attr);
        QryResult qryresult = term.evaluate(null);
        double idf = Math.log( (double)QryEval.READER.getDocCount("body")/(double)qryresult.invertedList.df);

        for (int k = 0; k < qryresult.invertedList.df; k++) {
          int docid = qryresult.invertedList.getDocid(k);
          //f8
          if (docidToTermOverlap.containsKey(docid))
            docidToTermOverlap.put(docid, docidToTermOverlap.get(docid) + 1);
          else
            docidToTermOverlap.put(docid, 1);
        }
        querySize++;
      }
      
      for (Entry<Integer, ArrayList<Double>> entry: hmap.entrySet())
      {
        int docid = entry.getKey();
        ArrayList<Double> featurelist = entry.getValue();
        Terms terms = QryEval.READER.getTermVector(docid, attr);
        if (terms ==null)
        {
          featurelist.add((double)0/0);
          featurelist.add((double)0/0);
          featurelist.add((double)0/0);
          hmap.put(docid, featurelist);
          continue;
        }
        
        
        if (BM25score.containsKey(docid))    
          featurelist.add(BM25score.get(docid));
        else 
          featurelist.add((double)0);            
        
        if (Indriscore.containsKey(docid))
          featurelist.add(Indriscore.get(docid));
        else
          featurelist.add((double)0);            
        
        if (docidToTermOverlap.containsKey(docid))
          featurelist.add((double)docidToTermOverlap.get(docid)/(double)querySize);
        else
          featurelist.add((double)0); 
          hmap.put(docid, featurelist);
        
      }
    }
    
    //f17
    HashMap <Integer,Double> RankedBooleanscore = new HashMap <Integer,Double>();

    HashMap<Integer, Double>  docidTotf = new HashMap<Integer, Double>();
    HashMap<Integer, Double>  docidTotfidf = new HashMap<Integer, Double>();

    qTree = parseQuery("#AND(" + query + ")", rankedBoolean);
    QryResult queryResultRanked = qTree.evaluate(rankedBoolean);
    for (int i=0;i< queryResultRanked.docScores.scores.size();i++)
      RankedBooleanscore.put(queryResultRanked.docScores.getDocid(i),queryResultRanked.docScores.getDocidScore(i));

    for (String querySingle : query.split(" "))
    {


      if (tokenizeQuery(querySingle).length==0)
        continue;
      QryopIlTerm term = new QryopIlTerm(tokenizeQuery(querySingle)[0], "body");
      QryResult qryresult = term.evaluate(null);
      double idf = Math.log( (double)QryEval.READER.getDocCount("body")/(double)qryresult.invertedList.df);

      for (int k = 0; k < qryresult.invertedList.df; k++) {
        int docid = qryresult.invertedList.getDocid(k);
        double tf = qryresult.invertedList.getTf(k);
        //f15
        if (docidTotf.containsKey(docid))
          docidTotf.put(docid, docidTotf.get(docid) + tf);
        else
          docidTotf.put(docid, tf);        
        //f16
        if (docidTotfidf.containsKey(docid))
          docidTotfidf.put(docid, docidTotfidf.get(docid) + tf*idf);
        else
          docidTotfidf.put(docid, tf*idf);
      }
    }
    for (Entry<Integer, ArrayList<Double>> entry: hmap.entrySet())
    {
      int docid = entry.getKey();
      ArrayList<Double> featurelist = entry.getValue();
      
      if (RankedBooleanscore.containsKey(docid))
        featurelist.add((double)RankedBooleanscore.get(docid));
      else
        featurelist.add((double)0); 
      
      double lenD = QryEval.doclenStore.getDocLength("body",docid);
  //    if (docidTotf.containsKey(docid))
  //      featurelist.add((double)docidTotf.get(docid)/(double)lenD);
  //    else
  //      featurelist.add((double)0); 
      
      if (docidTotfidf.containsKey(docid))
        featurelist.add((double)docidTotfidf.get(docid)/(double)lenD);
      else
        featurelist.add((double)0); 
    }    
    return hmap;
  }

  private static String queryExpansion(QryResult queryResult, RetrievalModel model, String qid)
          throws NumberFormatException, Exception {

    HashMap<String, Double> hmap = new HashMap<String, Double>();
    HashMap<String, Double> MLEmap = new HashMap<String, Double>();

    long lenC = QryEval.READER.getSumTotalTermFreq("body");
    // model.fbDocs = 100;

    if (model.fbInitialRankingFile != null) {

      BufferedReader in = new BufferedReader(new FileReader(model.fbInitialRankingFile));

      String tempStr;
      queryResult = new QryResult();
      while ((tempStr = in.readLine()) != null) {
        // if (tempStr.)
        String[] str = tempStr.split(" ");
        if (str[0].equals(qid) && Integer.parseInt(str[3]) <= model.fbDocs) {
          queryResult.docScores.add(getInternalDocid(str[2]), Double.parseDouble(str[4]));
        }
      }

    }
    queryResult.docScores.sort();

    for (int i = 0; i < model.fbDocs; i++) {
      // String eid = getExternalDocid(queryResult.docScores.getDocid(i));
      TermVector tv = new TermVector(queryResult.docScores.getDocid(i), "body");
      System.out.println("hi score " + queryResult.docScores.getDocid(i) + " "
              + queryResult.docScores.getDocidScore(i));
      // System.out.println(tv.luceneTerms.getDocCount());
      long lenD = QryEval.doclenStore.getDocLength("body", queryResult.docScores.getDocid(i));
      for (int j = 0; j < tv.stemsLength(); j++) {
        double score = queryResult.docScores.getDocidScore(i);

        if (tv.stemString(j) == null)
          continue;
        double MLE;
        double ctf = (double) QryEval.READER.totalTermFreq(new Term("body", new BytesRef(tv
                .stemString(j))));
        if (!MLEmap.containsKey(tv.stemString(j))) {
          MLE = ctf / (double) lenC;
          MLEmap.put(tv.stemString(j), ctf);
        } else
          MLE = MLEmap.get(tv.stemString(j)) / (double) lenC;
        score *= ((double) tv.stemFreq(j) + model.fbMu * MLE) / (lenD + model.fbMu);
        score *= Math.log((double) lenC / ctf);

        // System.out.println(tv.stemString(j) + " " + tv.stemFreq(j) + " " + score);
        if (hmap.containsKey(tv.stemString(j)))
          hmap.put(tv.stemString(j), score + hmap.get(tv.stemString(j)));
        else
          hmap.put(tv.stemString(j), score);

      }
      // for (Terms r:tv.luceneTerms)
      // {
      // System.out.println(r.toString());
      // }
      // docmap.put(, value)
      // hmap.put(, value)
    }
    hmap = (HashMap<String, Double>) MapUtil.sortByValue(hmap);
    Iterator<Entry<String, Double>> iter = hmap.entrySet().iterator();
    int nowNumOfUsedTerm = 0;
    HashMap<String, Double> finalMap = new HashMap<String, Double>();
    double totalweight = 0;
    double nowWeight = 0;

    iter = hmap.entrySet().iterator();

    /*
     * nowNumOfUsedTerm=0;
     * 
     * while (iter.hasNext() && nowNumOfUsedTerm<model.fbTerms) { Entry<String, Double> entry =
     * iter.next(); totalweight +=entry.getValue(); nowNumOfUsedTerm++; }
     */
    String queryExpansionString = "#Wand(";
    nowNumOfUsedTerm = 0;
    iter = hmap.entrySet().iterator();
    double sumWeight = 0;
    while (iter.hasNext() && nowNumOfUsedTerm < model.fbTerms) {
      Entry<String, Double> entry = iter.next();
      if (entry.getKey().contains(".") || entry.getKey().contains(",")) {
        continue;
      }
      nowWeight = entry.getValue();
      queryExpansionString += " " + nowWeight;
      queryExpansionString += " " + entry.getKey();
      sumWeight += nowWeight;
      nowNumOfUsedTerm++;
    }
    System.out.println("sumw: " + sumWeight);
    queryExpansionString += " )";

    /*
     * for(Entry<String, Double> entry : hmap.entrySet()) { System.out.println(entry.getKey()+ " "+
     * entry.getValue()); }
     */
    // Qryop qTree = parseQuery(queryExpansionString, model);
    // QryResult queryResult2 = qTree.evaluate(model);

    return queryExpansionString;
  }

  /**
   * Write the results of models into files
   * 
   * @param queryID
   *          , query, result (model) , fileToWrite
   * @return void
   */
  private static void writeResults(String queryID, String query, QryResult result,
          BufferedWriter writer) throws IOException {

    System.out.println(result.docScores.scores.size());
    int rank;
    if (result.docScores.scores.size() > 0) {
      result.docScores.sort();
      for (int i = 0; i < result.docScores.scores.size(); i++) {
        rank = i + 1;
        if (rank > 100)
          break;
        writer.write(queryID + " Q0 " + getExternalDocid(result.docScores.getDocid(i)) + " " + rank
                + " " + result.docScores.getDocidScore(i) + " run-1\n");
        // System.out.println(queryID + " Q0 " + getExternalDocid(result.docScores.getDocid(i)) +
        // " " + rank
        // + " " + result.docScores.getDocidScore(i) + " run-1\n");
      }
    }
    writer.flush();
  }

  /**
   * Write an error message and exit. This can be done in other ways, but I wanted something that
   * takes just one statement so that it is easy to insert checks without cluttering the code.
   * 
   * @param message
   *          The error message to write before exiting.
   * @return void
   */
  static void fatalError(String message) {
    System.err.println(message);
    System.exit(1);
  }

  /**
   * Get the external document id for a document specified by an internal document id. If the
   * internal id doesn't exists, returns null.
   * 
   * @param iid
   *          The internal document id of the document.
   * @throws IOException
   */
  static String getExternalDocid(int iid) throws IOException {
    Document d = QryEval.READER.document(iid);
    String eid = d.get("externalId");
    return eid;
  }

  /**
   * Finds the internal document id for a document specified by its external id, e.g.
   * clueweb09-enwp00-88-09710. If no such document exists, it throws an exception.
   * 
   * @param externalId
   *          The external document id of a document.s
   * @return An internal doc id suitable for finding document vectors etc.
   * @throws Exception
   */
  static int getInternalDocid(String externalId) throws Exception {
    Query q = new TermQuery(new Term("externalId", externalId));

    IndexSearcher searcher = new IndexSearcher(QryEval.READER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }

  /**
   * parseQuery converts a query string into a query tree.
   * 
   * @param qString
   *          A string containing a query.
   * @param model
   * @param qTree
   *          A query tree
   * @throws IOException
   */
  // static int title=0,inlink=0,url=0,body=0,keywords=0;
  static Qryop parseQuery(String qString, RetrievalModel model) throws IOException {

    Qryop currentOp = null;
    Stack<Qryop> stack = new Stack<Qryop>();

    // Add a default query operator to an unstructured query. This
    // is a tiny bit easier if unnecessary whitespace is removed.

    qString = qString.trim();

    if (model instanceof RetrievalModelBM25) {
      if (!qString.startsWith("#SUM"))
        qString = "#SUM(" + qString + ")";
    } else if (model instanceof RetrievalModelRankedBoolean
            || model instanceof RetrievalModelUnrankedBoolean) {
      if (!qString.startsWith("#AND")) {
        qString = "#AND(" + qString + ")";
      }
    } else if (model instanceof RetrievalModelIndri) {
      if (!qString.startsWith("#AND"))
        qString = "#AND(" + qString + ")";
      // else if (!qString.endsWith(")"))
      // qString = "#AND(" + qString + ")";

    }
    // Tokenize the query.

    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;
    int nowToken = 0;
    // Each pass of the loop processes one token. To improve
    // efficiency and clarity, the query operator on the top of the
    // stack is also stored in currentOp.
    System.out.println("cc" + QryEval.READER.maxDoc());
    Boolean isNowWeight = true;
    while (tokens.hasMoreTokens()) {
      token = tokens.nextToken();
      System.out.println(token);
      if (token.matches("[ ,(\t\n\r]")) {
        // Ignore most delimiters.
      } else if (token.equalsIgnoreCase("#and")) {

        currentOp = new QryopSlAnd();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#syn")) {
        currentOp = new QryopIlSyn();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#or")) {
        currentOp = new QryopSlOr();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#wand")) {
        currentOp = new QryopSlWand(doclenStore);
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#wsum")) {
        currentOp = new QryopSlWsum(doclenStore);
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#Sum")) {
        currentOp = new QryopSlSum(doclenStore);
        stack.push(currentOp);
      } else if (token.toLowerCase().contains("#near")) {
        currentOp = new QryopIlNear(Integer.parseInt(token.substring(6, token.length())));
        stack.push(currentOp);
      } else if (token.toLowerCase().contains("#window")) {
        currentOp = new QryopIlWindow(Integer.parseInt(token.substring(8, token.length())));
        stack.push(currentOp);

      } else if (token.startsWith(")")) { // Finish current query operator.
        // If the current query operator is not an argument to
        // another query operator (i.e., the stack is empty when it
        // is removed), we're done (assuming correct syntax - see
        // below). Otherwise, add the current operator as an
        // argument to the higher-level operator, and shift
        // processing back to the higher-level operator.

        stack.pop();

        if (stack.empty())
          break;

        Qryop arg = currentOp;
        currentOp = stack.peek();
        currentOp.add(arg);
        isNowWeight = true;
      } else {
        // System.out.println(currentOp.args.size();
        // Do lexical processing of the token before creating the query term,
        // and check to see whether the token specifies a particular field (e.g., apple.title).
        if (currentOp instanceof QryopSlWsum) {
          System.out.println("aha" + ((QryopSlWsum) currentOp).weightList.size() + " "
                  + currentOp.args.size());

          if (((QryopSlWsum) currentOp).weightList.size() == currentOp.args.size()) {
            ((QryopSlWsum) currentOp).addweight(Double.parseDouble(token));
            System.out.println("aha" + ((QryopSlWsum) currentOp).weightList.size() + " "
                    + currentOp.args.size());

            isNowWeight = false;
            continue;
          } else
            isNowWeight = true;

        }
        if (currentOp instanceof QryopSlWand) {
          System.out.println(((QryopSlWand) currentOp).weightList.size() + " "
                  + currentOp.args.size());
          if (((QryopSlWand) currentOp).weightList.size() == currentOp.args.size()) {
            ((QryopSlWand) currentOp).addweight(Double.parseDouble(token));
            isNowWeight = false;
            continue;

          } else
            isNowWeight = true;
        }

        // || currentOp instanceof QryopSlWand
        if (token.contains(".")) {
          String[] tokenArr = token.split("\\.");
          if (tokenizeQuery(tokenArr[0]).length != 0)
            currentOp.add(new QryopIlTerm(tokenizeQuery(tokenArr[0])[0], tokenArr[1]));

        } else {
          if (tokenizeQuery(token).length != 0) {

            currentOp.add(new QryopIlTerm(tokenizeQuery(token)[0]));

            /*
             * used for experimenting String t = tokenizeQuery(token)[0]; title += new
             * InvList(t,"title").df; url += new InvList(t,"url").df; body += new
             * InvList(t,"body").df; inlink+= new InvList(t,"inlink").df; keywords += new
             * InvList(t,"keywords").df;
             */
          }
        }
        if (currentOp instanceof QryopSlWand)
          if (((QryopSlWand) currentOp).weightList.size() != currentOp.args.size())
            ((QryopSlWand) currentOp).weightList
                    .remove(((QryopSlWand) currentOp).weightList.size() - 1);
        // System.out.println(((QryopSlWsum) currentOp).weightList.size() +
        // " "+currentOp.args.size());
        // System.out.println(currentOp.args.get(0));
        if (currentOp instanceof QryopSlWsum)
          if (((QryopSlWsum) currentOp).weightList.size() != currentOp.args.size())
            ((QryopSlWsum) currentOp).weightList
                    .remove(((QryopSlWsum) currentOp).weightList.size() - 1);

      }
    }
    System.out.println(currentOp);
    // A broken structured query can leave unprocessed tokens on the
    // stack, so check for that.

    if (tokens.hasMoreTokens()) {
      if (model instanceof RetrievalModelRankedBoolean
              || model instanceof RetrievalModelUnrankedBoolean)
        qString = "#OR(" + qString + ")";
      else if (model instanceof RetrievalModelBM25)
        qString = "#SUM(" + qString + ")";
      else if (model instanceof RetrievalModelIndri)
        qString = "#AND(" + qString + ")";

      return parseQuery(qString, model);
    }

    return currentOp;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can indicate whether garbage
   * collection should be performed, which slows the program but reduces memory usage.
   * 
   * @param gc
   *          If true, run the garbage collector before reporting.
   * @return void
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc) {
      runtime.gc();
    }

    System.out.println("Memory used:  "
            + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Print the query results.
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO THAT IT OUTPUTS IN THE
   * FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName
   *          Original query.
   * @param result
   *          Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException
   */
  static void printResults(String queryName, QryResult result) throws IOException {

    System.out.println(queryName + ":  ");
    if (result.docScores.scores.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      for (int i = 0; i < result.docScores.scores.size(); i++) {
        System.out.println("\t" + i + ":  " + getExternalDocid(result.docScores.getDocid(i)) + ", "
                + result.docScores.getDocidScore(i));
      }
    }
  }

  /**
   * Given a query string, returns the terms one at a time with stopwords removed and the terms
   * stemmed using the Krovetz stemmer.
   * 
   * Use this method to process raw query terms.
   * 
   * @param query
   *          String containing query
   * @return Array of query tokens
   * @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }
}
