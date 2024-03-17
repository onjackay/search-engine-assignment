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
            KGramStat[] stats = new KGramStat[candidates.size()];
            int j = 0;
            for (String candidate : candidates) {
                stats[j++] = new KGramStat(candidate, index.getPostings(candidate).size());
            }
            Arrays.sort(stats);
            String result[] = new String[Math.min(limit, stats.length)];
            for (int i = 0; i < Math.min(limit, stats.length); i++) {
                result[i] = stats[stats.length - i - 1].getToken();
            }
            return result;
        }
        return null;
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
        return null;
    }
}
