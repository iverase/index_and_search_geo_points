import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a in-memory BKD tree.
 */
public class MemoryBKDTree {

    /** Default max number of points on leaf nodes */
    protected static final int DEFAULT_DOCUMENTS_PER_LEAF = 1024;

    /** Documents on the tree */
    private final Document[] documents;
    /** Max level of the tree, first level is 1*/
    private final int maxLevel;
    /** node id for the first leaf node. It represents as well the number of leaf nodes of the tree */
    private final int startLeafNodes;
    /** Offsets of documents for each leaf node.*/
    private final int[] leafDocumentsOffset;
    /** Upper point for each node of the tree. It can be retrieve by nodeId -1*/
    private final double[][] maxBoundaries;
    /** Lower point for each node of the tree. It can be retrieve by nodeId -1*/
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
     * Constructor that takes the  number of documents per leaf.
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
        maxBoundaries = new double[totalNumberOfNodes][2];
        minBoundaries = new double[totalNumberOfNodes][2];
        computeOffsets();
        buildTree();
        //set the root
        this.nodeId = 1;
    }

    /**
     * Compute the number of levels needed to store the provided documents.
     *
     * @param numberDocuments number of documents to store.
     * @param maxDocumentsPerLeaf maximum number of documents per leaf.
     * @return the required number of levels.
     */
    private int getTreeLevels(final int numberDocuments, final int maxDocumentsPerLeaf) {
        //it must be a better way to do this
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
        final int minimumDocsPerLeaf = documents.length / startLeafNodes;
        final int leafsWithExtraDocuments = documents.length % startLeafNodes;
        this.leafDocumentsOffset[0] =0;
        for (int i = 1; i< startLeafNodes; i++) {
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
        Arrays.sort(documents, (o1, o2) -> (o1.point[0] > o2.point[0]) ? 1 : o1.point[0] < o2.point[0] ? -1 : 0);
        //Sort by latitude each longitude partitions. If maxLevel is uneven then there is one more partition
        //by latitude.
        int numberPartitions = (int)Math.pow(2, maxLevel / 2);
        int partitionOffset = leafDocumentsOffset.length / numberPartitions;
        for (int i = 0; i < numberPartitions; i++) {
            int start = leafDocumentsOffset[i * partitionOffset];
            int end = (i == numberPartitions -1) ? documents.length : leafDocumentsOffset[(i +1) * partitionOffset];
            Arrays.sort(documents, start, end, (o1, o2) -> (o1.point[1] > o2.point[1]) ? 1 : o1.point[1] < o2.point[1] ? -1 : 0);
        }
        //process leaf boundaries
        int j = 0;
        for (int i = 0; i < documents.length ;) {
            int end = (j == leafDocumentsOffset.length - 1) ? documents.length : leafDocumentsOffset[j + 1];
            processLeafBoundaries(i, end, startLeafNodes + j);
            i += (end -i);
            j++;
        }
        //now build the rest of the tree upwards
        processNodeBoundaries(maxLevel - 1);
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
            double maxLongitude = Math.max(maxBoundaries[2 * nodeStart - 1][0],maxBoundaries[2 * nodeStart][0]);
            double minLongitude = Math.min(minBoundaries[2 * nodeStart - 1][0],minBoundaries[2 * nodeStart][0]);
            double maxLatitude =  Math.max(maxBoundaries[2 * nodeStart - 1][1],maxBoundaries[2 * nodeStart][1]);
            double minLatitude =  Math.min(minBoundaries[2 * nodeStart - 1][1],minBoundaries[2 * nodeStart][1]);
            assert maxLatitude >= minLatitude;
            maxBoundaries[nodeStart - 1][0] = maxLongitude;
            maxBoundaries[nodeStart - 1][1] = maxLatitude;
            minBoundaries[nodeStart - 1][0] = minLongitude;
            minBoundaries[nodeStart - 1][1] = minLatitude;
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
     * @param end THe end index of the documents.
     * @param nodeId The leaf node id.
     */
    private void processLeafBoundaries(final int start, final int end, final int nodeId) {
        double maxLongitude = -180.0;
        double minLongitude = 180.0;
        double maxLatitude = -90.0;
        double minLatitude = 90.0;
        for (int i = start; i < end; i++) {
            maxLongitude = Math.max(maxLongitude, documents[i].point[0]);
            minLongitude = Math.min(minLongitude, documents[i].point[0]);
            maxLatitude = Math.max(maxLatitude, documents[i].point[1]);
            minLatitude = Math.min(minLatitude, documents[i].point[1]);
        }
        maxBoundaries[nodeId - 1][0] = maxLongitude;
        maxBoundaries[nodeId - 1][1] = maxLatitude;
        minBoundaries[nodeId - 1][0] = minLongitude;
        minBoundaries[nodeId - 1][1] = minLatitude;
    }

    /**
     * Computes the points inside the provided bounding box.
     *
     * @param upperPoint The upper left corner of the bounding box.
     * @param lowerPoint The lower right corner of the bounding box.
     * @return The matching documents.
     */
    public List<Document> contains(final double[] upperPoint, final double[] lowerPoint) {
        final List<Document> doc = new ArrayList<>();
        contains(upperPoint, lowerPoint, doc);
        return doc;
    }

    /**
     * Computes recursively the points inside the provided bounding box by checking the relationship
     * of the provided bounding box with the node bounding box.
     *
     * @param upperPoint The upper left corner of the bounding box.
     * @param lowerPoint The lower right corner of the bounding box.
     * @param doc The list collector.
     */
    private void contains(final double[] upperPoint, final double[] lowerPoint, final List<Document> doc) {
        final double[] thisUpperPoint = maxBoundaries[nodeId - 1];
        final double[] thisLowerPoint = minBoundaries[nodeId - 1];
        final int rel = BoundingBoxUtils.relate(thisUpperPoint, thisLowerPoint, upperPoint, lowerPoint);
        if (rel == BoundingBoxUtils.WITHIN) {
            //add all docs
            addAll(doc);
        }
        else if (rel != BoundingBoxUtils.DISJOINT) {
            if (isLeaf()) {
                //brute force
                addOneByOne(upperPoint, lowerPoint, doc);
            } else {
                //down one level
                leftNode().contains(upperPoint, lowerPoint, doc);
                parent().rightNode().contains(upperPoint, lowerPoint, doc);
                parent();
            }
        }
    }

    /**
     * Collects matching documents of this node by checking the spatial relationship.
     *
     * @param upperPoint The upper left corner of the bounding box.
     * @param lowerPoint The lower right corner of the bounding box.
     * @param doc The list collector.
     */
    private void addOneByOne(final double[] upperPoint, final double[] lowerPoint, final List<Document> doc) {
        final int startDocument = startDocuments();
        final int endDocument = endDocuments();
        for (int i = startDocument; i < endDocument; i++) {
            if (BoundingBoxUtils.contains(upperPoint, lowerPoint, documents[i].point)) {
                doc.add(documents[i]);
            }
        }
    }

    /**
     * Collects all documents inside this node.
     *
     * @param doc the list collector.
     */
    private void addAll(final List<Document> doc) {
        final int start = startDocuments();
        final int end = endDocuments();
        for(int i = start; i < end; i++) {
            doc.add(documents[i]);
        }
    }

    /**
     * Checks if we are in a leaf node.
     *
     * @return true if is a leaf node else false.
     */
    private boolean isLeaf() {
        return nodeId >= this.startLeafNodes;
    }

    /**
     * Checks if we are in the root node.
     *
     * @return true if is the root node else false.
     */
    private boolean isRoot() {
        return nodeId == 1;
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
        nodeId *= 2;
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
        nodeId *= 2;
        nodeId +=  1;
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
        nodeId /= 2;
        return this;
    }

    /**
     * Return the start index for the documents under current node.
     *
     * @return the start index for the documents under current node.
     */
    private int startDocuments() {
        return leafDocumentsOffset[startLeafNode()];
    }

    /**
     * Return the end index for the documents under current node.
     *
     * @return the end index for the documents under current node.
     */
    private int endDocuments() {
        final int leafIndex = endLeafNode();
        if (leafIndex == leafDocumentsOffset.length -1) {
            return documents.length;
        }
        return leafDocumentsOffset[leafIndex + 1];
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
            index = nodeId  - this.startLeafNodes;
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
            index = nodeId  - this.startLeafNodes;
        } else {
            index = rightNode().endLeafNode();
            parent();
        }

        return index;
    }
}


