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
        else if (queryType == QueryType.RANKED_QUERY) {
            int[] df = new int[query.queryterm.size()];
            PostingsList[] postingsLists = new PostingsList[query.queryterm.size()];
            PostingsList resultList = null;

            for (int i = 0; i < query.queryterm.size(); i++) {
                QueryTerm queryterm = query.queryterm.get(i);
                postingsLists[i] = index.getPostings(queryterm.term);
                df[i] = postingsLists[i].size();

                if (i == 0) {
                    resultList = postingsLists[i];
                }
                else {
                    resultList = resultList.unionWith(postingsLists[i]);
                }
            }

            for (int i = 0; i < query.queryterm.size(); i++) {
                double idf = Math.log((double) Index.docNames.size() / df[i]);
                double weight_query = 1; // 1 / idf;

                for (int j = 0, k = 0; j < resultList.size(); j++) {
                    while (k < postingsLists[i].size() - 1 && postingsLists[i].get(k).docID < resultList.get(j).docID) {
                        k++;
                    }
                    if (postingsLists[i].get(k).docID == resultList.get(j).docID) {
                        int tf = postingsLists[i].get(k).positions.size();
                        double weight = tf * idf / Index.docLengths.get(resultList.get(j).docID);
    
                        resultList.get(j).score += weight * weight_query;
                    }
                }
            }
            
            resultList.sortByScore();
            
            return resultList;
        }
        else {
            return new PostingsList();
        }
    }
}