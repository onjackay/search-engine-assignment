/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Map;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
        //
        //  YOUR CODE HERE
        //
        int cntRelevant = 0;
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                cntRelevant++;
            }
        }
        if (cntRelevant == 0) {
            return;
        }
        HashMap<String, Double> termWeights = new HashMap<String, Double>();
        // Extract current query terms and multiply weights with alpha
        for (QueryTerm term : queryterm) {
            if (termWeights.containsKey(term.term)) {
                termWeights.put(term.term, termWeights.get(term.term) + term.weight * alpha);
            } else {
                termWeights.put(term.term, term.weight * alpha);
            }
        }

        try {
            for (int i = 0; i < docIsRelevant.length; i++) {
                if (!docIsRelevant[i]) continue;
                String docPath = Index.docNames.get(results.get(i).docID);
                Reader reader = new InputStreamReader(new FileInputStream(docPath), StandardCharsets.UTF_8);
                Tokenizer tok = new Tokenizer(reader, true, false, true, engine.patterns_file);
                while (tok.hasMoreTokens()) {
                    String token = tok.nextToken();
                    // Term frequency only
                    if (termWeights.containsKey(token)) {
                        termWeights.put(token, termWeights.get(token) + beta / cntRelevant);
                    } else {
                        termWeights.put(token, beta / cntRelevant);
                    }
                }
            }
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }

        // Update query terms
        queryterm.clear();
        for (Map.Entry<String, Double> entry : termWeights.entrySet()) {
            queryterm.add(new QueryTerm(entry.getKey(), entry.getValue()));
        }
    }
}


