/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
        return list.get( i );
    }

    // 
    //  YOUR CODE HERE
    //

    /** Append with a docID */
    public void insertDocID(int docID) {
        // for (PostingsEntry entry: list) {
        //     if (entry.docID == docID) {
        //         return;
        //     }
        // }
        if (!list.isEmpty() && list.getLast().docID == docID) {
            return;
        }
        PostingsEntry entry = new PostingsEntry();
        entry.docID = docID;
        list.add(entry);
    }

    /* Intersect two PostingLists */
    public PostingsList intersect(PostingsList other) {
        PostingsList result = new PostingsList();

        for (int i = 0, j = 0; i < list.size(); i++) {
            while (j < other.list.size() - 1 && other.list.get(j).docID < list.get(i).docID) {
                j++;
            }
            if (list.get(i).docID == other.list.get(j).docID) {
                result.insertDocID(list.get(i).docID);
            }
        }

        return result;
    }
}

