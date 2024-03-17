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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
            PostingsList resultList = getWildCardPostings(query.queryterm.getFirst().term);
            for (int i = 1; i < query.queryterm.size(); i++) {
                QueryTerm queryterm = query.queryterm.get(i);
                resultList = resultList.intersectWith(getWildCardPostings(queryterm.term));
            }
            return resultList;
        }
        else if (queryType == QueryType.PHRASE_QUERY) {
            PostingsList resultList = getWildCardPostings(query.queryterm.getFirst().term);
            for (int i = 1; i < query.queryterm.size(); i++) {
                QueryTerm queryterm = query.queryterm.get(i);
                resultList = resultList.phraseWith(getWildCardPostings(queryterm.term), i);
            }
            return resultList;
        }
        else if (queryType == QueryType.RANKED_QUERY) {
            PostingsList[] postingsLists = new PostingsList[query.queryterm.size()];
            PostingsList resultList = null;

            for (int i = 0; i < query.queryterm.size(); i++) {
                QueryTerm queryterm = query.queryterm.get(i);
                postingsLists[i] = getWildCardPostings(queryterm.term);
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
                resultList = hitsRanker.rank(resultList);
            }

            resultList.sortByScore();
            // getNDCG(resultList);
            return resultList;
        }
        else {
            return new PostingsList();
        }
    }

    private PostingsList getWildCardPostings(String term) {
        if (term.indexOf('*') == -1) {
            return index.getPostings(term);
        }
        List<KGramPostingsEntry> kgPostings = kgIndex.getWildCardPostings(term);
        PostingsList resultList = null;
        for (KGramPostingsEntry entry : kgPostings) {
            String token = kgIndex.getTermByID(entry.tokenID);
            if (resultList == null) {
                resultList = index.getPostings(token);
            }
            else {
                resultList = resultList.unionWith(index.getPostings(token));
            }
        }
        return resultList;
    }

    private double[] getTfidf(Query query, PostingsList[] postingsLists, PostingsList resultList, NormalizationType normType) {
        double[] tfidf = new double[resultList.size()];
        
        for (int i = 0; i < query.queryterm.size(); i++) {
            String term = query.queryterm.get(i).term;
            double weight_queryterm = query.queryterm.get(i).weight;
            if (term.indexOf("*") == -1) {
                int df = postingsLists[i].size();
                double idf = Math.log((double) Index.docNames.size() / df);

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
            else {
                List<KGramPostingsEntry> kgPostings = kgIndex.getWildCardPostings(term);
                for (KGramPostingsEntry entry : kgPostings) {
                    String token = kgIndex.getTermByID(entry.tokenID);
                    PostingsList postingsList = index.getPostings(token);
                    int df = postingsList.size();
                    double idf = Math.log((double) Index.docNames.size() / df);

                    for (int j = 0, k = 0; j < resultList.size(); j++) {
                        int currDocId = resultList.get(j).docID;
                        while (k < postingsList.size() && postingsList.get(k).docID < currDocId) {
                            k++;
                        }
                        if (k < postingsList.size() && postingsList.get(k).docID == currDocId) {
                            int tf = postingsList.get(k).positions.size();
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
            }
        }
        return tfidf;
    }

    private double[] getPagerank(PostingsList resultList) {
        double[] pageranks = new double[resultList.size()];
        for (int i = 0; i < resultList.size(); i++) {
            String fileName = Index.docNames.get(resultList.get(i).docID);
            int index = fileName.lastIndexOf("\\");
            String tos = fileName.substring(index + 1);
            if (pagerank.containsKey(tos)) {
                pageranks[i] = pagerank.get(tos);
            }
            else {
                pageranks[i] = 0;
            }
        }
        return pageranks;
    }

    private void getNDCG(PostingsList resultList) {
        // Read relevance score
        HashMap<String, Integer> relevance = new HashMap<String, Integer>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("data/average_relevance_filtered.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                relevance.put(parts[0], Integer.parseInt(parts[1]));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Calculate nDCG
        int n = Math.min(resultList.size(), 50);
        double dcg = 0, idcg = 0;
        int[] rel = new int[n];
        for (int i = 0; i < n; i++) {
            String fileName = Index.docNames.get(resultList.get(i).docID);
            int index = fileName.lastIndexOf("\\");
            String tos = fileName.substring(index + 1);
            if (relevance.containsKey(tos) && !tos.equals("Mathematics.f")) {
                rel[i] = relevance.get(tos);
                dcg += rel[i] / Math.log(i + 2);
            }
            else {
                rel[i] = 0;
            }
        }
        Arrays.sort(rel);
        for (int i = 0; i < n; i++) {
            idcg += rel[n - i - 1] / Math.log(i + 2);
        }
        System.err.println("DCG: " + dcg);
        System.err.println("nDCG: " + dcg / idcg);
    }
}