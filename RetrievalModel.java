/**
 *  The search engine must support multiple retrieval models.  Some
 *  retrieval models have parameters.  All of them influence the way a
 *  query operator behaves.  Passing around a retrieval model object
 *  during query evaluation allows this information to be shared with
 *  query operators (and nested query operators) conveniently.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

public abstract class RetrievalModel {

  /**
   *  Set a retrieval model parameter.
   *  @param parameterName The name of the parameter to set.
   *  @param parametervalue The parameter's value.
   *  @return true if the parameter is set successfully, false otherwise.
   */
  boolean fb=false;
  int fbDocs=10;
  int fbTerms=100;
  double fbMu=2500;
  double fbOrigWeight=0.5;
  String fbInitialRankingFile="initialRanking.txt";
  String fbExpansionQueryFile="queryExpansion.txt";
  public abstract boolean setParameter (String parameterName, double value);

  /**
   *  Set a retrieval model parameter.
   *  @param parameterName The name of the parameter to set.
   *  @param parametervalue The parameter's value.
   *  @return true if the parameter is set successfully, false otherwise.
   */
  public abstract boolean setParameter (String parameterName, String value);
}
