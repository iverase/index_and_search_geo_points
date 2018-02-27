package esproject;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a in-memory BKD tree (static KDB tree) . It is created with the array of {@link Document}s to be indexed
 * and optionally the documents per leaf. It cannot be modified once created. It builds the tree using a
 * bulk mechanism that requires only three passes of the documents. One to sort by longitude, one to sort by latitude
 * and one to compute the nodes bounding boxes. It is not thread safe.
 * <p>
 * It supports queries by bounding box.
 */
public class KDBTree {

    /**
     * Default max number of points on leaf nodes
     */
    public static final int DEFAULT_DOCUMENTS_PER_LEAF = 1024;

    /**
     * Documents on the tree
     */
    private final Document[] documents;
    /**
     * Max level of the tree, first level is 1
     */
    private final int maxLevel;
    /**
     * Start document index in the documents array
     */
    private final int startDocument;
    /**
     * End document index in the documents array
     */
    private final int endDocument;
    /**
     * node id for the first leaf node. It represents as well the number of leaf nodes of the tree
     */
    private final int startLeafNodes;
    /**
     * minimum current value for docs per leaf
     */
    private final int minimumDocsPerLeaf;
    /**
     * number of leafs with one extra documents.
     */
    private final int leafsWithExtraDocument;
    /**
     * Upper point for each node of the tree. It can be fetched by nodeId -1
     */
    private final double[][] maxBoundaries;
    /**
     * Lower point for each node of the tree. It can be fetched by nodeId -1
     */
    private final double[][] minBoundaries;

    /**
     * Current node
     */
    private int nodeId;

    /**
     * Constructor that takes the number of documents per leaf and a start and a subset of the input array.
     *
     * @param documents           the documents to index.
     * @param maxDocumentsPerLeaf maximum number of documents per leaf node.
     * @param sorted              flags if th documents are sorted by longitude.
     */
    public KDBTree(final Document[] documents, final int maxDocumentsPerLeaf, int startDocuments, int endDocuments, boolean sorted) {
        this.documents = documents;
        this.maxLevel = getTreeLevels(endDocuments - startDocuments, maxDocumentsPerLeaf);
        this.startDocument = startDocuments;
        this.endDocument = endDocuments;
        // we cache this values
        this.startLeafNodes = (int) Math.pow(2, maxLevel - 1);
        this.minimumDocsPerLeaf = (endDocuments - startDocuments)/ this.startLeafNodes;
        this.leafsWithExtraDocument = (endDocuments - startDocuments) % this.startLeafNodes;
        //init arrays for bounding boxes
        int totalNumberOfNodes = 2 * startLeafNodes - 1;
        this.maxBoundaries = new double[totalNumberOfNodes][2];
        this.minBoundaries = new double[totalNumberOfNodes][2];
        //set the root
        this.nodeId = 1;
        //build the tree using bulk mechanism
        buildTree(sorted);
    }

    /**
     * Compute the number of levels needed to store the provided documents.
     *
     * @param numberDocuments     number of documents to store.
     * @param maxDocumentsPerLeaf maximum number of documents per leaf.
     * @return the required number of levels.
     */
    private int getTreeLevels(final int numberDocuments, final int maxDocumentsPerLeaf) {
        int levels = 1;
        int estimate = maxDocumentsPerLeaf;
        while (estimate < numberDocuments) {
            levels++;
            estimate = (int) Math.pow(2, levels - 1) * maxDocumentsPerLeaf;
        }
        return levels;
    }

    /**
     * Build the tree. First uses merge sort to order document by longitude. Then merge sort again to order
     * documents by longitude partition. Finally computes the bounding boxes for each node of the tree.
     *
     * @param sorted              flags if th documents are sorted by longitude.
     */
    private void buildTree(boolean sorted) {
        //Sort by longitude if needed
        if (!sorted) {
            Arrays.sort(this.documents, this.startDocument, this.endDocument, (o1, o2) -> (o1.point[0] > o2.point[0]) ? 1 : o1.point[0] < o2.point[0] ? -1 : 0);
        }
        //Sort by latitude each longitude partitions. If maxLevel is uneven then there is one more partition
        //by latitude.
        int numberLongitudePartitions = (int) Math.pow(2, this.maxLevel / 2);
        int leafNodesPerLongitudePartition = this.startLeafNodes / numberLongitudePartitions;
        for (int i = 0; i < startLeafNodes;) {
            int start = startDocuments(i);
            int end = endDocuments(i + leafNodesPerLongitudePartition - 1);
            Arrays.sort(this.documents, start, end, (o1, o2) -> (o1.point[1] > o2.point[1]) ? 1 : o1.point[1] < o2.point[1] ? -1 : 0);
            i += leafNodesPerLongitudePartition;
        }
        //process leaf boundaries
        for (int i = 0; i < this.startLeafNodes; i++) {
            int start = startDocuments(i);
            int end = endDocuments(i);
            processLeafBoundaries(start, end, startLeafNodes + i);
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
        for (int i = 0; i < numberNodes; i++) {
            this.maxBoundaries[nodeStart - 1][0] = Math.max(this.maxBoundaries[2 * nodeStart - 1][0], this.maxBoundaries[2 * nodeStart][0]);
            this.minBoundaries[nodeStart - 1][0] = Math.min(this.minBoundaries[2 * nodeStart - 1][0], this.minBoundaries[2 * nodeStart][0]);
            this.maxBoundaries[nodeStart - 1][1] = Math.max(this.maxBoundaries[2 * nodeStart - 1][1], this.maxBoundaries[2 * nodeStart][1]);
            this.minBoundaries[nodeStart - 1][1] = Math.min(this.minBoundaries[2 * nodeStart - 1][1], this.minBoundaries[2 * nodeStart][1]);
            nodeStart++;
        }
        if (level > 1) {
            processNodeBoundaries(level - 1);
        }
    }

    /**
     * Computes the bounding box of one leaf node.
     *
     * @param start  The starting index of the documents.
     * @param end    The end index of the documents.
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
     * @param collector  The list collector.
     */
    public void contains(final double[] upperPoint, final double[] lowerPoint, final List<Document> collector) {
        final double[] thisUpperPoint = this.maxBoundaries[nodeId - 1];
        final double[] thisLowerPoint = this.minBoundaries[nodeId - 1];
        final int rel = BoundingBoxUtils.relate(thisUpperPoint, thisLowerPoint, upperPoint, lowerPoint);
        if (rel == BoundingBoxUtils.WITHIN) {
            //add all docs
            addAll(collector);
        } else if (rel != BoundingBoxUtils.DISJOINT) {
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
     * @param collector  The list collector.
     */
    private void addOneByOne(final double[] upperPoint, final double[] lowerPoint, final List<Document> collector) {
        if (!isLeaf()) {
            throw new IllegalStateException("Call addOneByOne() method on non-leaf node.");
        }
        final int startDocument = startDocuments(this.nodeId - this.startLeafNodes);
        final int endDocument = endDocuments(this.nodeId - this.startLeafNodes);
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
        final int start = startDocuments(startLeafNode());
        final int end = endDocuments(endLeafNode());
        for (int i = start; i < end; i++) {
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
    private KDBTree leftNode() {
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
    private KDBTree rightNode() {
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
    private KDBTree parent() {
        if (isRoot()) {
            throw new IllegalStateException("Call parent() method on root node.");
        }
        this.nodeId /= 2;
        return this;
    }

    /**
     * Computes the start index of the documents for the provided leaf.
     *
     * @param positionLeaf the position of the leaf.
     * @return he start index for the documents for the provided leaf position.
     */
    private int startDocuments(int positionLeaf) {
        if (positionLeaf  < leafsWithExtraDocument) {
            return this.startDocument + positionLeaf * (minimumDocsPerLeaf + 1);
        } else {
            return this.startDocument + positionLeaf * minimumDocsPerLeaf + leafsWithExtraDocument;
        }
    }

    /**
     * Computes the end index of the documents for the provided leaf.
     *
     * @param positionLeaf the position of the leaf.
     * @return he end index for the documents for the provided leaf position.
     */
    private int endDocuments(int positionLeaf) {
        if (positionLeaf == startLeafNodes - 1) {
            return this.endDocument;
        } else {
            return startDocuments(positionLeaf + 1);
        }
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
        result = 31 * result + Integer.hashCode(maxLevel);
        result = 31 * result + Integer.hashCode(startLeafNodes);
        result = 31 * result + Integer.hashCode(minimumDocsPerLeaf);
        result = 31 * result + Integer.hashCode(leafsWithExtraDocument);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        //too complex, we consider they are equals if they are the same instance
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "levels: " + this.maxLevel  + "; docs per leaf:" + minimumDocsPerLeaf + "; number docs:" + documents.length;
    }
}