/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;


public class SpellChecker {
    /** The regular inverted index to be used by the spell checker */
    Index index;

    /** K-gram index to be used by the spell checker */
    KGramIndex kgIndex;

    /** The auxiliary class for containing the value of your ranking function for a token */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }

        public int compareTo(Object other) {
            if (this.score == ((KGramStat)other).score) return 0;
            return this.score < ((KGramStat)other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling
     * correction should pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;


    /**
      * The threshold for edit distance for a candidate spelling
      * correction to be accepted.
      */
    private static final int MAX_EDIT_DISTANCE = 2;


    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Computes the Jaccard coefficient for two sets A and B, where the size of set A is 
     *  <code>szA</code>, the size of set B is <code>szB</code> and the intersection 
     *  of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        //
        // YOUR CODE HERE
        //
        return (double) intersection / (szA + szB - intersection);
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming.
     * Allowed operations are:
     *      => insert (cost 1)
     *      => delete (cost 1)
     *      => substitute (cost 2)
     */
    private int editDistance(String s1, String s2) {
        //
        // YOUR CODE HERE
        //
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int i = 0; i <= s2.length(); i++) {
            dp[0][i] = i;
        }
        for (int i = 0; i < s1.length(); i++) {
            for (int j = 0; j < s2.length(); j++) {
                if (s1.charAt(i) == s2.charAt(j)) {
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    dp[i + 1][j + 1] = Math.min(dp[i][j + 1] + 1, dp[i + 1][j] + 1);
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /**
     * Get all candidates for spelling correction.
     */
    private String[] getCandidates(String term) {
        HashSet<String> candidates = kgIndex.getKGramWords(term);
        HashSet<String> kgrams = kgIndex.getKGrams(term);

        candidates.removeIf(word -> {
            String augmentedWord = "$" + word + "$";
            int intersection = 0;
            for (int i = 0; i < augmentedWord.length() - kgIndex.getK() + 1; i++) {
                String kgram = augmentedWord.substring(i, i + kgIndex.getK());
                if (kgrams.contains(kgram)) {
                    intersection++;
                }
            }
            return jaccard(kgrams.size(), augmentedWord.length() - kgIndex.getK() + 1, intersection) < JACCARD_THRESHOLD || editDistance(term, word) > MAX_EDIT_DISTANCE;
        });
        return candidates.toArray(new String[0]);
    }

    /**
     *  Checks spelling of all terms in <code>query</code> and returns up to
     *  <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {
        //
        // YOUR CODE HERE
        //
        
        /** Single-term query */
        if (query.size() == 1) {
            String term = query.queryterm.get(0).term;
            String[] candidates = getCandidates(term);
            KGramStat[] stats = new KGramStat[candidates.length];
            for (int i = 0; i < candidates.length; i++) {
                stats[i] = new KGramStat(candidates[i], index.getPostings(candidates[i]).size());
            }
            Arrays.sort(stats);
            String result[] = new String[Math.min(limit, stats.length)];
            for (int i = 0; i < Math.min(limit, stats.length); i++) {
                result[i] = stats[stats.length - i - 1].getToken();
            }
            return result;
        }
        else {
            List<List<KGramStat>> qCorrections = new ArrayList<List<KGramStat>>(query.size());
            for (int i = 0; i < query.size(); i++) {
                String term = query.queryterm.get(i).term;
                String[] candidates = getCandidates(term);
                KGramStat[] stats = new KGramStat[candidates.length];
                for (int j = 0; j < candidates.length; j++) {
                    stats[j] = new KGramStat(candidates[j], Math.log(index.getPostings(candidates[j]).size()) - editDistance(term, candidates[j]));
                }
                Arrays.sort(stats);
                List<KGramStat> list = new ArrayList<KGramStat>(Math.min(limit, stats.length));
                for (int j = 0; j < Math.min(limit, stats.length); j++) {
                    list.add(new KGramStat(stats[stats.length - j - 1].getToken(), editDistance(term, stats[stats.length - j - 1].getToken())));
                }
                qCorrections.add(list);
            }
            List<KGramStat> merged = mergeCorrections(qCorrections, limit);
            merged.sort(null);
            String[] result = new String[Math.min(limit, merged.size())];
            for (int i = 0; i < Math.min(limit, merged.size()); i++) {
                result[i] = merged.get(merged.size() - i - 1).getToken();
            }
            return result;
        }
    }

    /**
     *  Merging ranked candidate spelling corrections for all query terms available in
     *  <code>qCorrections</code> into one final merging of query phrases. Returns up
     *  to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        //
        // YOUR CODE HERE
        //
        /** For intersection querytype */

        class Candidate {
            String term;
            PostingsList postings;

            Candidate(String term, PostingsList postings) {
                this.term = term;
                this.postings = postings;
            }
        }

        List<Candidate> candidates = new ArrayList<Candidate>();
        

        
        for (int i = 0; i < qCorrections.size(); i++) {
            if (i == 0) {
                for (int j = 0; j < qCorrections.get(0).size(); j++) {
                    String term = qCorrections.get(0).get(j).getToken();
                    candidates.add(new Candidate(term, index.getPostings(term)));
                }
            }
            else {
                List<Candidate> newCandidates = new ArrayList<Candidate>();
                
                for (int j = 0; j < candidates.size(); j++) {
                    for (int k = 0; k < qCorrections.get(i).size(); k++) {
                        String term = qCorrections.get(i).get(k).getToken();
                        PostingsList postings = index.getPostings(term);
                        if (postings == null) {
                            continue;
                        }
                        newCandidates.add(new Candidate(candidates.get(j).term + " " + term, postings.intersectWith(candidates.get(j).postings)));
                    }
                }
                
                newCandidates.sort((a, b) -> {
                    return b.postings.size() - a.postings.size();
                });
                candidates = newCandidates.subList(0, Math.min((qCorrections.size() - i) * limit, newCandidates.size()));
            }
        }
        
        List<KGramStat> merged = new ArrayList<KGramStat>();
        for (int i = 0; i < candidates.size(); i++) {
            merged.add(new KGramStat(candidates.get(i).term, candidates.get(i).postings.size()));
        }
        return merged;
    }
}
