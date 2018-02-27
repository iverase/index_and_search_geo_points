package esproject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a BKD tree
 */
public class BKDTree {

    private final List<KDBTree> KBDTrees;

    public BKDTree(final Document[] documents) {
        this(documents, KDBTree.DEFAULT_DOCUMENTS_PER_LEAF);
    }

    public BKDTree(final Document[] documents, final int maxDocumentsPerLeaf) {
        Arrays.sort(documents, (o1, o2) -> (o1.point[0] > o2.point[0]) ? 1 : o1.point[0] < o2.point[0] ? -1 : 0);
        this.KBDTrees = new ArrayList<>();
        int start = 0;
        while (true) {
            int docsFullTree = getDocumentsForFullTree(documents.length - start, maxDocumentsPerLeaf);
            this.KBDTrees.add(new KDBTree(documents, maxDocumentsPerLeaf, start, start + docsFullTree, true));
            start = start + docsFullTree;
            if (start >= documents.length) {
                break;
            }
        }
    }

    /**
     * Returns the number of documents needed to fill up a tree.
     * @param length The size of the documents.
     * @param maxDocsPerLef the max number of documents per leaf.
     * @return the number of documents needed to fill up a tree. Always lower or equal
     * to the provided length.
     */
    private int getDocumentsForFullTree(final int length, final int maxDocsPerLef) {
        if (length <= maxDocsPerLef) {
            return length;
        }
        int level = 2;
        while ((int) Math.pow(2, level - 1) * maxDocsPerLef < length) {
            level++;
        }
        return (int) Math.pow(2, level - 2) * maxDocsPerLef;
    }

    /**
     * Computes recursively the points inside the provided bounding box by checking the relationship
     * of the provided bounding box with the node bounding box. Adds the result to the list collector.
     *
     * @param upperPoint The upper left corner of the bounding box.
     * @param lowerPoint The lower right corner of the bounding box.
     * @param collector  The list collector.
     */
    public void contains(final double[] upperPoint, final double[] lowerPoint, final List<Document> collector) {
        for (KDBTree tree : this.KBDTrees) {
            tree.contains(upperPoint, lowerPoint, collector);
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.KBDTrees.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BKDTree)) {
            return false;
        }
        BKDTree other = (BKDTree) obj;
        return this.KBDTrees.equals(other.KBDTrees);
    }

    @Override
    public String toString() {
        return this.KBDTrees.toString();
    }
}
