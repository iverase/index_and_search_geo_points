package esproject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a BKD tree which is a collection of {@link KDBTree} built inn a way
 * that ensures that all trees leaf nodes contains the required maximum documents er leaf
 * except the last one which only has one level.
 *
 * The array of {@link Document} is sorted before creating the trees so we ensure that trees do not overlap.
 */
public class BKDTree implements Tree {

    /**
     * The list of {@link KDBTree} trees.
     */
    private final List<KDBTree> KBDTrees;

    /**
     * Constructor that uses the default documents per leaf.
     *
     * @param documents the documents to be indexed.
     */
    public BKDTree(final Document[] documents) {
        this(documents, KDBTree.DEFAULT_DOCUMENTS_PER_LEAF);
    }

    /**
     * Constructor with documents per leaf.
     *
     * @param documents the documents to be indexed.
     * @param maxDocumentsPerLeaf the documents per leaf.
     */
    public BKDTree(final Document[] documents, final int maxDocumentsPerLeaf) {
        // we sort the array now to mke sure the trees do not overlap
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

   @Override
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
