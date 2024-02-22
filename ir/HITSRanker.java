/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.util.*;
import java.io.*;


public class HITSRanker {

    /**
     *   Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    /**
     *   Convergence criterion: hub and authority scores do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     *   The inverted index
     */
    Index index;

    /**
     *   Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String,Integer> titleToId = new HashMap<String,Integer>();

    /**
     *   Sparse vector containing hub scores
     */
    HashMap<Integer,Double> hubs;

    /**
     *   Sparse vector containing authority scores
     */
    HashMap<Integer,Double> authorities;

    /**
     *  Edges of the graph
     */
    HashMap<Integer, ArrayList<Integer>> linksTo = new HashMap<Integer, ArrayList<Integer>>();
    HashMap<Integer, ArrayList<Integer>> linksFrom = new HashMap<Integer, ArrayList<Integer>>();

    /**
     *  The set of documents that we base our ranking on
     */
    Set<Integer> baseSet;

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    
    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph.
     * Each page is a node in graph with a distinct nodeID associated with it.
     * There is an edge between two nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     *  nodeID;outNodeID1,outNodeID2,...,outNodeIDK
     * This means that there are edges between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format:
     *  nodeID;pageTitle
     *  
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the same
     *       as docIDs used by search engine's Indexer
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     * @param      index           The inverted index
     */
    public HITSRanker( String linksFilename, String titlesFilename, Index index ) {
        this.index = index;
        readDocs( linksFilename, titlesFilename );
    }


    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path.
     * For example, given the path "davisWiki/hello.f",
     * the function will return "hello.f".
     *
     * @param      path  The file path
     *
     * @return     The file name.
     */
    private String getFileName( String path ) {
        String result = "";
        StringTokenizer tok = new StringTokenizer( path, "\\/" );
        while ( tok.hasMoreTokens() ) {
            result = tok.nextToken();
        }
        return result;
    }


    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     */
    void readDocs( String linksFilename, String titlesFilename ) {
        //
        // YOUR CODE HERE
        //
        int fileIndex = 0;
        
        try {
            System.err.println("Reading title file... ");
            BufferedReader in = new BufferedReader(new FileReader(titlesFilename));
            String line;
            while ((line = in.readLine()) != null) {
                String[] s = line.split(";");
                int id = Integer.parseInt(s[0]);
                titleToId.put(s[1], id);
            }
            System.err.println("Read " + titleToId.size() + " number of titles");
            in.close();
        }
        catch (FileNotFoundException e) {
            System.err.println("File " + titlesFilename + " not found!");
        }
        catch (IOException e) {
            System.err.println("Error reading file " + titlesFilename);
        }

        try {
            System.err.print( "Reading link file... " );
            BufferedReader in = new BufferedReader( new FileReader( linksFilename ));
            String line;
            while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
                int index = line.indexOf( ";" );
                int fromId = Integer.parseInt(line.substring( 0, index ));
                if (index == line.length() - 1) {
                    continue;
                }
                String[] tos = line.substring( index+1, line.length() ).split(",");
                for (String to: tos) {
                    int toId = Integer.parseInt(to);
                    if (!linksTo.containsKey(fromId)) {
                        linksTo.put(fromId, new ArrayList<Integer>());
                    }
                    linksTo.get(fromId).add(toId);

                    if (!linksFrom.containsKey(toId)) {
                        linksFrom.put(toId, new ArrayList<Integer>());
                    }
                    linksFrom.get(toId).add(fromId);
                }
            }
            if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
                System.err.print( "stopped reading since documents table is full. " );
            }
            else {
                System.err.print( "done. " );
            }
            in.close();
        }
        catch ( FileNotFoundException e ) {
            System.err.println( "File " + linksFilename + " not found!" );
        }
        catch ( IOException e ) {
            System.err.println( "Error reading file " + linksFilename );
        }
        System.err.println( "Read " + fileIndex + " number of documents" );

    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param      titles  The titles of the documents in the root set
     */
    private void iterate(String[] titles) {
        //
        // YOUR CODE HERE
        //
        hubs = new HashMap<Integer,Double>();
        authorities = new HashMap<Integer,Double>();

        Set<Integer> baseSet = new HashSet<Integer>();
        for (String title: titles) {
            Integer id = titleToId.get(title);
            if (id != null) {
                baseSet.add(id);
                if (linksTo.containsKey(id)) {
                    for (int to: linksTo.get(id)) {
                        baseSet.add(to);
                    }
                }
                if (linksFrom.containsKey(id)) {
                    for (int from: linksFrom.get(id)) {
                        baseSet.add(from);
                    }
                }
            }
        }

        for (int id: baseSet) {
            hubs.put(id, 1.0);
            authorities.put(id, 1.0);
        }

        for (int step = 0; step < MAX_NUMBER_OF_STEPS; step++) {
            System.err.println("Step " + step + "...");
            HashMap<Integer, Double> newHubs = new HashMap<Integer,Double>();
            HashMap<Integer, Double> newAuthorities = new HashMap<Integer,Double>();
            for (int id: baseSet) {
                double hub = 0;
                double authority = 0;
                if (linksFrom.containsKey(id)) {
                    for (int from: linksFrom.get(id)) {
                        authority += hubs.get(from);
                    }
                }
                if (linksTo.containsKey(id)) {
                    for (int to: linksTo.get(id)) {
                        hub += authorities.get(to);
                    }
                }
                newHubs.put(id, hub);
                newAuthorities.put(id, authority);
            }

            double hubSum = 0;
            double authoritySum = 0;
            for (int id: baseSet) {
                hubSum += newHubs.get(id) * newHubs.get(id);
                authoritySum += newAuthorities.get(id) * newAuthorities.get(id);
            }
            hubSum = Math.sqrt(hubSum);
            authoritySum = Math.sqrt(authoritySum);

            double hubsDiff = 0;
            double authoritiesDiff = 0;
            for (int id: baseSet) {
                double hub = newHubs.get(id) / hubSum;
                double authority = newAuthorities.get(id) / authoritySum;
                hubsDiff += Math.abs(hubs.get(id) - hub);
                authoritiesDiff += Math.abs(authorities.get(id) - authority);
                hubs.put(id, hub);
                authorities.put(id, authority);
            }

            if (hubsDiff < EPSILON && authoritiesDiff < EPSILON) {
                break;
            }
        }
    }


    /**
     * Rank the documents in the subgraph induced by the documents present
     * in the postings list `post`.
     *
     * @param      post  The list of postings fulfilling a certain information need
     *
     * @return     A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {
        //
        // YOUR CODE HERE
        //
        int n = post.size();
        String[] titles = new String[n];
        for (int i = 0; i < n; i++) {
            titles[i] = Index.docNames.get(post.get(i).docID);
        }
        iterate(titles);
        HashMap<Integer,Double> scores = new HashMap<Integer,Double>();
        for (int i = 0; i < n; i++) {
            scores.put(post.get(i).docID, hubs.get(titleToId.get(titles[i])) + authorities.get(titleToId.get(titles[i])));
        }
        HashMap<Integer,Double> sortedScores = sortHashMapByValue(scores);
        PostingsList result = new PostingsList();
        for (Map.Entry<Integer,Double> e: sortedScores.entrySet()) {
            result.insert(e.getKey(), 0, e.getValue());
        }
        return result;
    }


    /**
     * Sort a hash map by values in the descending order
     *
     * @param      map    A hash map to sorted
     *
     * @return     A hash map sorted by values
     */
    private HashMap<Integer,Double> sortHashMapByValue(HashMap<Integer,Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer,Double> > list = new ArrayList<Map.Entry<Integer,Double> >(map.entrySet());
      
            Collections.sort(list, new Comparator<Map.Entry<Integer,Double>>() {
                public int compare(Map.Entry<Integer,Double> o1, Map.Entry<Integer,Double> o2) { 
                    return (o2.getValue()).compareTo(o1.getValue()); 
                } 
            }); 
              
            HashMap<Integer,Double> res = new LinkedHashMap<Integer,Double>(); 
            for (Map.Entry<Integer,Double> el : list) { 
                res.put(el.getKey(), el.getValue()); 
            }
            return res;
        }
    } 


    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param      map        A hash map
     * @param      fname      The filename
     * @param      k          A number of entries to write
     */
    void writeToFile(HashMap<Integer,Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
            
            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer,Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k) break;
                }
            }
            writer.close();
        } catch (IOException e) {}
    }


    /**
     * Rank all the documents in the links file. Produces two files:
     *  hubs_top_30.txt with documents containing top 30 hub scores
     *  authorities_top_30.txt with documents containing top 30 authority scores
     */
    void rank() {
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer,Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer,Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
        if ( args.length != 2 ) {
            System.err.println( "Please give the names of the link and title files" );
        }
        else {
            HITSRanker hr = new HITSRanker( args[0], args[1], null );
            hr.rank();
        }
    }
} 