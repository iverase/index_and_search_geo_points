package esproject;

/**
 * Test for KDBTree.
 */
public class KDBTreeTest extends AbstractTreeTest {

    @Override
    public Tree getTree(Document[] documents, int maxDocsPerLeaf) {
        return new KDBTree(documents, maxDocsPerLeaf);
    }
}
