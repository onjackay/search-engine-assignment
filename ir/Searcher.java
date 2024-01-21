/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import ir.Query.QueryTerm;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        if (query.queryterm.isEmpty()) {
            return new PostingsList();
        }
        else if (queryType == QueryType.INTERSECTION_QUERY) {
            PostingsList resultList = index.getPostings(query.queryterm.getFirst().term);
            for (int i = 1; i < query.queryterm.size(); i++) {
                QueryTerm queryterm = query.queryterm.get(i);
                resultList = resultList.intersectWith(index.getPostings(queryterm.term));
            }
            return resultList;
        }
        else if (queryType == QueryType.PHRASE_QUERY) {
            PostingsList resultList = index.getPostings(query.queryterm.getFirst().term);
            for (int i = 1; i < query.queryterm.size(); i++) {
                QueryTerm queryterm = query.queryterm.get(i);
                resultList = resultList.phraseWith(index.getPostings(queryterm.term), i);
            }
            return resultList;
        }
        else {
            return new PostingsList();
        }
    }
}