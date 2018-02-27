package esproject;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a in-memory BKD tree (static KDB tree) . It is created with the array of {@link Document}s to be indexed
 * and optionally the documents per leaf. It cannot be modified once created. It builds the tree using a
 * bulk mechanism that requires only three passes of the documents. One to sort by longitude, one to sort by latitude
 * and one to compute the nodes bounding boxes. It is not thread safe.
 *
 * It supports queries by bounding box.
 */
public class MemoryBKDTree {

    /** Default max number of points on leaf nodes */
    public static final int DEFAULT_DOCUMENTS_PER_LEAF = 1024;

    /** Documents on the tree */
    private final Document[] documents;
    /** Max level of the tree, first level is 1*/
    private final int maxLevel;
    /** node id for the first leaf node. It represents as well the number of leaf nodes of the tree */
    private final int startLeafNodes;
    /** Offsets of documents for each leaf node.*/
    private final int[] leafDocumentsOffset;
    /** Upper point for each node of the tree. It can be fetched by nodeId -1*/
    private final double[][] maxBoundaries;
    /** Lower point for each node of the tree. It can be fetched by nodeId -1*/
    private final double[][] minBoundaries;

    /** Current node */
    private int nodeId;

    /**
     * Constructor that uses the default number of documents per leaf.
     *
     * @param documents the documents to index.
     */
    public MemoryBKDTree(final Document[] documents) {
        this(documents, DEFAULT_DOCUMENTS_PER_LEAF);
    }

    /**
     * Constructor that takes the number of documents per leaf.
     *
     * @param documents the documents to index.
     * @param maxDocumentsPerLeaf maximum number of documents per leaf node.
     */
    public MemoryBKDTree(final Document[] documents, final int maxDocumentsPerLeaf) {
        this.documents = documents;
        this.maxLevel = getTreeLevels(documents.length, maxDocumentsPerLeaf);
        // we cache this value
        this.startLeafNodes = (int)Math.pow(2, maxLevel - 1);
        this.leafDocumentsOffset = new int[startLeafNodes];
        int totalNumberOfNodes = 2 * startLeafNodes - 1;
        this.maxBoundaries = new double[totalNumberOfNodes][2];
        this.minBoundaries = new double[totalNumberOfNodes][2];
        //set the root
        this.nodeId = 1;
        //first compute documents offset for leaf nodes
        computeOffsets();
        //build the tree using bulk mechanism
        buildTree();
    }

    /**
     * Compute the number of levels needed to store the provided documents.
     *
     * @param numberDocuments number of documents to store.
     * @param maxDocumentsPerLeaf maximum number of documents per leaf.
     * @return the required number of levels.
     */
    private int getTreeLevels(final int numberDocuments, final int maxDocumentsPerLeaf) {
        int levels = 1;
        int estimate = maxDocumentsPerLeaf;
        while (estimate < numberDocuments) {
            levels++;
            estimate = (int) Math.pow(2, levels) * maxDocumentsPerLeaf;
        }
        return levels;
    }

    /**
     * Compute the offset for the documents for each leaf node.
     */
    private void computeOffsets() {
        final int minimumDocsPerLeaf = this.documents.length / this.startLeafNodes;
        final int leafsWithExtraDocuments = this.documents.length % this.startLeafNodes;
        this.leafDocumentsOffset[0] = 0;
        for (int i = 1; i < this.startLeafNodes; i++) {
            final int numberDocs = (i - 1 < leafsWithExtraDocuments) ? minimumDocsPerLeaf + 1 : minimumDocsPerLeaf;
            this.leafDocumentsOffset[i] = this.leafDocumentsOffset[i - 1] + numberDocs;
        }
    }

    /**
     * Build the tree. First uses merge sort to order document by longitude. Then merge sort again to order
     * documents by longitude partition. Finally computes the bounding boxes for each node of the tree.
     */
    private void buildTree() {
        //Sort by longitude
        Arrays.sort(this.documents, (o1, o2) -> (o1.point[0] > o2.point[0]) ? 1 : o1.point[0] < o2.point[0] ? -1 : 0);
        //Sort by latitude each longitude partitions. If maxLevel is uneven then there is one more partition
        //by latitude.
        int numberPartitions = (int)Math.pow(2, this.maxLevel / 2);
        int partitionOffset = this.leafDocumentsOffset.length / numberPartitions;
        for (int i = 0; i < numberPartitions; i++) {
            int start = this.leafDocumentsOffset[i * partitionOffset];
            int end = (i == numberPartitions -1) ? this.documents.length : this.leafDocumentsOffset[(i +1) * partitionOffset];
            Arrays.sort(this.documents, start, end, (o1, o2) -> (o1.point[1] > o2.point[1]) ? 1 : o1.point[1] < o2.point[1] ? -1 : 0);
        }
        //process leaf boundaries
        int j = 0;
        for (int i = 0; i < this.documents.length ;) {
            int end = (j == this.leafDocumentsOffset.length - 1) ? this.documents.length : this.leafDocumentsOffset[j + 1];
            processLeafBoundaries(i, end, this.startLeafNodes + j);
            i += (end - i);
            j++;
        }
        //now build the rest of the tree upwards
        processNodeBoundaries(this.maxLevel - 1);
    }

    /**
     * Computes recursively upwards the bounding boxes for the non-leaf nodes of the tree level by level.
     *
     * @param level the current level.
     */
    private void processNodeBoundaries(final int level) {
        int nodeStart = (int) Math.pow(2, level - 1);
        final int numberNodes = nodeStart;
        for(int i = 0; i < numberNodes; i++ ) {
            this.maxBoundaries[nodeStart - 1][0] = Math.max(this.maxBoundaries[2 * nodeStart - 1][0], this.maxBoundaries[2 * nodeStart][0]);
            this.minBoundaries[nodeStart - 1][0] = Math.min(this.minBoundaries[2 * nodeStart - 1][0], this.minBoundaries[2 * nodeStart][0]);
            this.maxBoundaries[nodeStart - 1][1] = Math.max(this.maxBoundaries[2 * nodeStart - 1][1], this.maxBoundaries[2 * nodeStart][1]);
            this.minBoundaries[nodeStart - 1][1] = Math.min(this.minBoundaries[2 * nodeStart - 1][1], this.minBoundaries[2 * nodeStart][1]);
            nodeStart++;
        }
        if (level > 1) {
            processNodeBoundaries(level -1);
        }
    }

    /**
     * Computes the bounding box of one leaf node.
     *
     * @param start The starting index of the documents.
     * @param end The end index of the documents.
     * @param nodeId The leaf node id.
     */
    private void processLeafBoundaries(final int start, final int end, final int nodeId) {
        double maxLongitude = this.documents[start].point[0];
        double minLongitude = this.documents[start].point[0];
        double maxLatitude = this.documents[start].point[1];
        double minLatitude = this.documents[start].point[1];
        for (int i = start + 1; i < end; i++) {
            maxLongitude = Math.max(maxLongitude, this.documents[i].point[0]);
            minLongitude = Math.min(minLongitude, this.documents[i].point[0]);
            maxLatitude = Math.max(maxLatitude, this.documents[i].point[1]);
            minLatitude = Math.min(minLatitude, this.documents[i].point[1]);
        }
        this.maxBoundaries[nodeId - 1][0] = maxLongitude;
        this.maxBoundaries[nodeId - 1][1] = maxLatitude;
        this.minBoundaries[nodeId - 1][0] = minLongitude;
        this.minBoundaries[nodeId - 1][1] = minLatitude;
    }

    /**
     * Computes recursively the points inside the provided bounding box by checking the relationship
     * of the provided bounding box with the node bounding box. Adds the result to the list collector.
     *
     * @param upperPoint The upper left corner of the bounding box.
     * @param lowerPoint The lower right corner of the bounding box.
     * @param collector The list collector.
     */
    public void contains(final double[] upperPoint, final double[] lowerPoint, final List<Document> collector) {
        final double[] thisUpperPoint = this.maxBoundaries[nodeId - 1];
        final double[] thisLowerPoint = this.minBoundaries[nodeId - 1];
        final int rel = BoundingBoxUtils.relate(thisUpperPoint, thisLowerPoint, upperPoint, lowerPoint);
        if (rel == BoundingBoxUtils.WITHIN) {
            //add all docs
            addAll(collector);
        }
        else if (rel != BoundingBoxUtils.DISJOINT) {
            if (isLeaf()) {
                //brute force
                addOneByOne(upperPoint, lowerPoint, collector);
            } else {
                //down one level
                leftNode().contains(upperPoint, lowerPoint, collector);
                parent().rightNode().contains(upperPoint, lowerPoint, collector);
                parent();
            }
        }
    }

    /**
     * Collects matching documents of this node by checking the spatial relationship.
     *
     * @param upperPoint The upper left corner of the bounding box.
     * @param lowerPoint The lower right corner of the bounding box.
     * @param collector The list collector.
     */
    private void addOneByOne(final double[] upperPoint, final double[] lowerPoint, final List<Document> collector) {
        final int startDocument = startDocuments();
        final int endDocument = endDocuments();
        for (int i = startDocument; i < endDocument; i++) {
            if (BoundingBoxUtils.contains(upperPoint, lowerPoint, this.documents[i].point)) {
                collector.add(this.documents[i]);
            }
        }
    }

    /**
     * Collects all documents inside this node.
     *
     * @param collector the list collector.
     */
    private void addAll(final List<Document> collector) {
        final int start = startDocuments();
        final int end = endDocuments();
        for(int i = start; i < end; i++) {
            collector.add(this.documents[i]);
        }
    }

    /**
     * Checks if we are in a leaf node.
     *
     * @return true if is a leaf node else false.
     */
    private boolean isLeaf() {
        return this.nodeId >= this.startLeafNodes;
    }

    /**
     * Checks if we are in the root node.
     *
     * @return true if is the root node else false.
     */
    private boolean isRoot() {
        return this.nodeId == 1;
    }

    /**
     * Moves to the left node of current node.
     *
     * @return this tree.
     */
    private MemoryBKDTree leftNode() {
        if (isLeaf()) {
            throw new IllegalStateException("Call leftNode() method on leaf node.");
        }
        this.nodeId *= 2;
        return this;
    }

    /**
     * Moves to the right node of current node.
     *
     * @return this tree
     */
    private MemoryBKDTree rightNode() {
        if (isLeaf()) {
            throw new IllegalStateException("Call rightNode() method on leaf node.");
        }
        this.nodeId *= 2;
        this.nodeId += 1;
        return this;
    }

    /**
     * Moves to the parent of current node.
     *
     * @return this tree.
     */
    private MemoryBKDTree parent() {
        if (isRoot()) {
            throw new IllegalStateException("Call parent() method on root node.");
        }
        this.nodeId /= 2;
        return this;
    }

    /**
     * Return the start index for the documents under current node.
     *
     * @return the start index for the documents under current node.
     */
    private int startDocuments() {
        return this.leafDocumentsOffset[startLeafNode()];
    }

    /**
     * Return the end index for the documents under current node.
     *
     * @return the end index for the documents under current node.
     */
    private int endDocuments() {
        final int leafIndex = endLeafNode();
        if (leafIndex == this.leafDocumentsOffset.length - 1) {
            return this.documents.length;
        }
        return this.leafDocumentsOffset[leafIndex + 1];
    }

    /**
     * Return the position of the start leaf node under current node with
     * respect the first leaf node.
     *
     * @return index of the start leaf node.
     */
    private int startLeafNode() {
        final int index;
        if (isLeaf()) {
            index = this.nodeId  - this.startLeafNodes;
        } else {
            index = leftNode().startLeafNode();
            parent();
        }
        return index;
    }

    /**
     * Return the position of the end leaf node under current node with
     * respect the first leaf node.
     *
     * @return index of the end leaf node.
     */
    private int endLeafNode() {
        final int index;
        if (isLeaf()) {
            index = this.nodeId  - this.startLeafNodes;
        } else {
            index = rightNode().endLeafNode();
            parent();
        }
        return index;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + documents.hashCode();
        result = 31 * result + maxBoundaries.hashCode();
        result = 31 * result + minBoundaries.hashCode();
        result = 31 * result + leafDocumentsOffset.hashCode();
        result = 31 * result + Integer.hashCode(maxLevel);
        result = 31 * result + Integer.hashCode(startLeafNodes);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        //too complex, we consider they are equals if they are the same instance
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "levels: " + this.maxLevel + "; number docs:" + documents.length;
    }
}