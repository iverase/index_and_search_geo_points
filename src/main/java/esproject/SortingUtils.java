package esproject;

import java.util.Arrays;

/**
 * Methods for sorting {@link Document}
 */
class SortingUtils {

    /**
     * Sort documents by provided dimension.
     *
     * @param documents the documents to sort.
     * @param dimension the dimension, 0 is longitude, 1 is latitude.
     */
    public static void sortByDimension(Document[] documents, int dimension) {
        sortByDimension(documents, 0, documents.length, dimension);
    }

    /**
     * Sort documents subset by provided dimension.
     *
     * @param documents the documents to sort.
     * @param start the start of the subset.
     * @param end the end of the subset.
     * @param dimension the dimension, 0 is longitude, 1 is latitude.
     */
    public static void sortByDimension(Document[] documents, int start, int end, int dimension) {
        Arrays.sort(documents, start, end, (o1, o2) -> (o1.point[dimension] > o2.point[dimension]) ? 1 : o1.point[dimension] < o2.point[dimension] ? -1 : 0);
    }

    private SortingUtils() {
        //no instances
    }
}
