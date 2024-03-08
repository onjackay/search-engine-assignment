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

    public void insert(int docID, int pos, double score) {
        if (!list.isEmpty() && list.getLast().docID == docID) {
            list.getLast().addPosition(pos);
            list.getLast().score = score;
        }
        else {
            PostingsEntry entry = new PostingsEntry(docID);
            entry.addPosition(pos);
            entry.score = score;
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
                ArrayList<Integer> this_pos = list.get(i).positions;
                ArrayList<Integer> other_pos = other.list.get(j).positions;

                for (int i_ = 0, j_ = 0; i_ < this_pos.size(); i_++) {
                    while (j_ < other_pos.size() - 1 && other_pos.get(j_) < this_pos.get(i_) + offset) {
                        j_++;
                    }
                    if (other_pos.get(j_) == this_pos.get(i_) + offset) {
                        entry.addPosition(this_pos.get(i_));
                    }
                }
                if (!entry.positions.isEmpty())
                    result.list.add(entry);
            }
        }

        return result;
    }

    /* Union with another PostingList */
    public PostingsList unionWith(PostingsList other) {
        PostingsList result = new PostingsList();
        int i = 0, j = 0;
        while (i < list.size() && j < other.list.size()) {
            if (list.get(i).docID < other.list.get(j).docID) {
                result.list.add(list.get(i));
                i++;
            }
            else if (list.get(i).docID > other.list.get(j).docID) {
                result.list.add(other.list.get(j));
                j++;
            }
            else {
                result.list.add(list.get(i));
                i++;
                j++;
            }
        }
        while (i < list.size()) {
            result.list.add(list.get(i));
            i++;
        }
        while (j < other.list.size()) {
            result.list.add(other.list.get(j));
            j++;
        }
        return result;
    }

    /** Return a string representation of the posting list */
    public String toString() {
        StringBuffer str = new StringBuffer();
        for (PostingsEntry e: list) {
            str.append(e.docID);
            str.append(':');

            for (int pos: e.positions) {
                str.append(pos);
                str.append(',');
            }
            str.append(';');
        }
        return str.toString();
    }

    /** Sort the list by scores. Only used for ranked searching result. */
    public void sortByScore() {
        list.sort(null);
    }
}

