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
    // public void insertDocID(int docID) {
    //     if (!list.isEmpty() && list.getLast().docID == docID) {
    //         return;
    //     }
    //     PostingsEntry entry = new PostingsEntry(docID);
    //     list.add(entry);
    // }

    /** Append with a docID and the position */
    public void insert(int docID, int pos) {
        if (!list.isEmpty() && list.getLast().docID == docID) {
            list.getLast().addPosition(pos);
        }
        else {
            PostingsEntry entry = new PostingsEntry(docID);
            entry.addPosition(pos);
            list.add(entry);
        }
    }

    /* Intersect with another PostingList */
    public PostingsList intersectWith(PostingsList other) {
        PostingsList result = new PostingsList();

        for (int i = 0, j = 0; i < list.size(); i++) {
            while (j < other.list.size() - 1 && other.list.get(j).docID < list.get(i).docID) {
                j++;
            }
            if (list.get(i).docID == other.list.get(j).docID) {
                // result.insertDocID(list.get(i).docID);
                result.list.add(list.get(i));
            }
        }

        return result;
    }

    /* Form phrase with another PostingList */
    public PostingsList phraseWith(PostingsList other, int offset) {
        PostingsList result = new PostingsList();

        for (int i = 0, j = 0; i < list.size(); i++) {
            while (j < other.list.size() - 1 && other.list.get(j).docID < list.get(i).docID) {
                j++;
            }
            if (list.get(i).docID == other.list.get(j).docID) {
                PostingsEntry entry = new PostingsEntry(list.get(i).docID);
                ArrayList<Integer> this_pos = list.get(i).position;
                ArrayList<Integer> other_pos = other.list.get(j).position;

                for (int i_ = 0, j_ = 0; i_ < this_pos.size(); i_++) {
                    while (j_ < other_pos.size() - 1 && other_pos.get(j_) < this_pos.get(i_) + offset) {
                        j_++;
                    }
                    if (other_pos.get(j_) == this_pos.get(i_) + offset) {
                        entry.addPosition(this_pos.get(i_));
                    }
                }
                if (!entry.position.isEmpty())
                    result.list.add(entry);
            }
        }

        return result;
    }
}

