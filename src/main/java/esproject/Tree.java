package esproject;

import java.util.List;

/**
 * Interface for trees that can be searched using bounding boxes.
 */
public interface Tree {

    /**
     * Computes recursively the points inside the provided bounding box by checking the relationship
     * of the provided bounding box with the node bounding box. Adds the result to the list collector.
     *
     * @param upperPoint The upper left corner of the bounding box.
     * @param lowerPoint The lower right corner of the bounding box.
     * @param collector  The list collector.
     */
     void contains(final double[] upperPoint, final double[] lowerPoint, final List<Document> collector);
}
