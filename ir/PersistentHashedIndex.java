/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "../index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    /** size of an entry in the dictionary file */
    public static final int DICT_ENTRY_SIZE = 12;
    /** size of an entry in a posting list in the data file */
    // public static final int DATA_ENTRY_SIZE = 8;

    String[] hashStrings = new String[(int) TABLESIZE];

    public int newHashKey(String token) {
        int hash_val = (int) ((token.hashCode() % TABLESIZE + TABLESIZE) % TABLESIZE);
        while (hashStrings[hash_val] != null) {
            hash_val = (int) ((hash_val + 1) % TABLESIZE);
        }
        hashStrings[hash_val] = token;
        return hash_val;
    }

    public int getHashKey(String token) {
        int hash_val = (int) ((token.hashCode() % TABLESIZE + TABLESIZE) % TABLESIZE);
        while (hashStrings[hash_val] != token) {
            hash_val = (int) ((hash_val + 1) % TABLESIZE);
        }
        return hash_val;
    }

    public int getCollisions(String token) {
        int hash_val = (int) ((token.hashCode() % TABLESIZE + TABLESIZE) % TABLESIZE);
        int result = 0;
        while (hashStrings[hash_val] != token && result < TABLESIZE) {
            hash_val = (int) ((hash_val + 1) % TABLESIZE);
            result++;
        }
        return result;
    }

    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        //
        //  YOUR CODE HERE
        //
        /** The starting position in data file */
        long pos;
        /** The size of posting list (in byte) */
        int size;

        public Entry(long pos, int size) {
            this.pos = pos;
            this.size = size;
        }
    }

    HashMap<Integer, Entry> entryMap = new HashMap<Integer, Entry>();

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }
    
    public static long bytesToLong(final byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    public static byte[] intToBytes(int l) {
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }
    
    public static int bytesToInt(final byte[] b) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            String token = readToken(ptr);
            byte[] data = new byte[size - token.length() - 1];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /** Reads only the token from the data file */
    String readToken(long ptr) {
        try {
            StringBuffer str = new StringBuffer();
            dataFile.seek(ptr);
            char ch = (char) dataFile.readByte();
            while (ch != ' ') {
                str.append(ch);
                ch = (char) dataFile.readByte();
            }
            return str.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        //
        //  YOUR CODE HERE
        //
        try {
            dictionaryFile.seek(ptr);
            byte[] pos_byte = longToBytes(entry.pos);
            byte[] size_byte = intToBytes(entry.size);
            dictionaryFile.write(pos_byte);
            dictionaryFile.write(size_byte);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {   
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
        //
        try {
            dictionaryFile.seek( ptr );
            byte[] pos_byte = new byte[8];
            byte[] size_byte = new byte[4];
            dictionaryFile.readFully( pos_byte );
            dictionaryFile.readFully( size_byte );
            return new Entry(bytesToLong(pos_byte), bytesToInt(size_byte));
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();
            for (Map.Entry<String, PostingsList> e: index.entrySet()) {
                String token = e.getKey();
                PostingsList postingsList = e.getValue();

                int hash_val = newHashKey(token);
                collisions += getCollisions(token);

                String str = token + ' ' + postingsList.toString();
                
                writeEntry(new Entry(free, str.length()), hash_val * DICT_ENTRY_SIZE);
                writeData(str, free);

                free += str.length();
            }

            //  YOUR CODE HERE
            //
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        PostingsList postingsList = new PostingsList();

        try {
            int hash_val = (int) ((token.hashCode() % TABLESIZE + TABLESIZE) % TABLESIZE);
            Entry entry;
            
            while (true) {
                entry = readEntry(hash_val * DICT_ENTRY_SIZE);
                if (entry.size == 0) {
                    return postingsList;
                }
                if (readToken(entry.pos).equals(token)) {
                    break;
                }
                hash_val++;
            }
            String str = readData(entry.pos, entry.size);

            String[] docs = str.split(";");
            for (String str_doc: docs) {
                int index = str_doc.indexOf(':');
                int docID = Integer.parseInt(str_doc.substring(0, index));
                String[] positions = str_doc.substring(index + 1).split(",");

                for (String str_pos: positions) {
                    int pos = Integer.parseInt(str_pos);
                    postingsList.insert(docID, pos);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return postingsList;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        //  YOUR CODE HERE
        //
        PostingsList postingList = index.get(token);
        
        if (postingList == null) {
            postingList = new PostingsList();
            postingList.insert(docID, offset);
            index.put(token, postingList);
        }
        else {
            postingList.insert(docID, offset);
        }
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
