package esproject;

/**
 * Test for BKDTree.
 */
public class BKDTreeTest extends AbstractTreeTest {

    @Override
    public Tree getTree(Document[] documents, int maxDocsPerLeaf) {
        return new BKDTree(documents, maxDocsPerLeaf);
    }
}
