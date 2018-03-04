package esproject;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a BKD tree which is a collection of {@link KDBTree} built inn a way
 * that ensures that all trees leaf nodes contains the required maximum documents per leaf
 * except the last one which only has one level.
 *
 * The array of {@link Document} is sorted before creating the trees so we ensure that trees
 * do not overlap.
 */
public class BKDTree implements Tree {

    /**
     * The list of {@link KDBTree} trees.
     */
    private final List<KDBTree> KDBTrees;

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
        this.KDBTrees = new ArrayList<>();
        // we sort the array now to make sure the trees do not overlap
        SortingUtils.sortByDimension(documents, 0);
        int start = 0;
        while (true) {
            int docsFullTree = getDocumentsForFullTree(documents.length - start, maxDocumentsPerLeaf);
            this.KDBTrees.add(new KDBTree(documents, maxDocumentsPerLeaf, start, start + docsFullTree, true));
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
        for (KDBTree tree : this.KDBTrees) {
            tree.contains(upperPoint, lowerPoint, collector);
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.KDBTrees.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BKDTree)) {
            return false;
        }
        BKDTree other = (BKDTree) obj;
        return this.KDBTrees.equals(other.KDBTrees);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BKD tree built with " + this.KDBTrees.size() + " KDB trees:\n" );
        for(int i =0; i < this.KDBTrees.size(); i++) {
            builder.append(" Tree " + (i + 1) + ": " + this.KDBTrees.get(i).toString() + "\n");
        }
        return builder.toString();
    }
}
