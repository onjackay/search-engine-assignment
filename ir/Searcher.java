/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import ir.Query.QueryTerm;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    /** The pagerank scores */
    HashMap<String, Double> pagerank = new HashMap<String, Double>();

    /** The HITS ranker */
    HITSRanker hitsRanker;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.hitsRanker = new HITSRanker("data/linksDavis.txt", "data/davisTitles.txt", index);

        // Read pagerank from file
        try (BufferedReader br = new BufferedReader(new FileReader("data/pagerank.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                String fileName = parts[0];
                double pr = Double.parseDouble(parts[1]);
                pagerank.put(fileName, pr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            PostingsList[] postingsLists = new PostingsList[query.queryterm.size()];
            PostingsList resultList = null;

            for (int i = 0; i < query.queryterm.size(); i++) {
                QueryTerm queryterm = query.queryterm.get(i);
                postingsLists[i] = index.getPostings(queryterm.term);
                if (i == 0) {
                    resultList = postingsLists[i];
                }
                else {
                    resultList = resultList.unionWith(postingsLists[i]);
                }
            }

            if (rankingType == RankingType.TF_IDF) {
                double[] tfidf = getTfidf(query, postingsLists, resultList, normType);
                for (int i = 0; i < resultList.size(); i++) {
                    resultList.get(i).score = tfidf[i];
                }
            }
            else if (rankingType == RankingType.PAGERANK) {
                double[] pagerank = getPagerank(resultList);
                for (int i = 0; i < resultList.size(); i++) {
                    resultList.get(i).score = pagerank[i];
                }
            }
            else if (rankingType == RankingType.COMBINATION) {
                double[] tfidf = getTfidf(query, postingsLists, resultList, normType);
                double[] pagerank = getPagerank(resultList);
                for (int i = 0; i < resultList.size(); i++) {
                    resultList.get(i).score = tfidf[i] + 1000 * pagerank[i];
                }
            }
            else if (rankingType == RankingType.HITS) {
                // TODO
                System.err.println("HITS");
                resultList = hitsRanker.rank(resultList);

            }

            resultList.sortByScore();
            return resultList;
        }
        else {
            return new PostingsList();
        }
    }

    private double[] getTfidf(Query query, PostingsList[] postingsLists, PostingsList resultList, NormalizationType normType) {
        double[] tfidf = new double[resultList.size()];
        
        for (int i = 0; i < query.queryterm.size(); i++) {
            int df = postingsLists[i].size();
            double idf = Math.log((double) Index.docNames.size() / df);
            double weight_queryterm = 1; // 1 / idf;

            for (int j = 0, k = 0; j < resultList.size(); j++) {
                int currDocId = resultList.get(j).docID;
                while (k < postingsLists[i].size() - 1 && postingsLists[i].get(k).docID < currDocId) {
                    k++;
                }
                if (postingsLists[i].get(k).docID == currDocId) {
                    int tf = postingsLists[i].get(k).positions.size();
                    double weight;
                    if (normType == NormalizationType.NUMBER_OF_WORDS) {
                        weight = tf * idf / Index.docLengths.get(currDocId);
                    }
                    else {
                        weight = tf * idf / Math.sqrt(Index.docSqrEuclLengths.get(currDocId));
                    }

                    tfidf[j] += weight * weight_queryterm;
                }
            }
        }
        return tfidf;
    }

    private double[] getPagerank(PostingsList resultList) {
        double[] pageranks = new double[resultList.size()];
        for (int i = 0; i < resultList.size(); i++) {
            String fileName = Index.docNames.get(resultList.get(i).docID);
            int index = fileName.lastIndexOf("\\");
            fileName = fileName.substring(index + 1);
            if (pagerank.containsKey(fileName)) {
                pageranks[i] = pagerank.get(fileName);
            }
            else {
                pageranks[i] = 0;
            }
        }
        return pageranks;
    }
}