package esproject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test for memory BKD tree.
 */
public class BKDTreeTest {

    @Test
    public void testBasicContains() {
        ArrayList<Document> documents = new ArrayList<>(8);
        documents.add(new Document("1", 0, 0));
        documents.add(new Document("2", 0, 1));
        documents.add(new Document("3", 1, 0));
        documents.add(new Document("4", 1, 1));
        documents.add(new Document("5", 30, 0));
        documents.add(new Document("6", 0, 30));
        documents.add(new Document("7", 30, 30));
        documents.add(new Document("8", 40, 40));
        List<MemoryBKDTree> bkdTrees = MemoryBKDTree.createBKDTree(documents.toArray(new Document[documents.size()]));

        double minlon = -2;
        double maxlon = 2;
        double minLat = -2;
        double maxLat = 2;
        double[] lowerPoint = new double[]{minlon, minLat};
        double[] upperPoint = new double[]{maxlon, maxLat};

        ArrayList<Document> answer = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            if (BoundingBoxUtils.contains(upperPoint, lowerPoint, doc.point)) {
                answer.add(doc);
            }
        }
        List<Document> treeAnswer = new ArrayList<>();
        for (MemoryBKDTree tree : bkdTrees) {
            tree.contains(upperPoint, lowerPoint, treeAnswer);
        }
        assert answer.size() == 4;
        assert answer.size() == treeAnswer.size();
    }

    @Test
    public void testBasicContainsDateLine() {
        ArrayList<Document> documents = new ArrayList<>(8);
        documents.add(new Document("1", -180, 0));
        documents.add(new Document("2", 179, 0));
        documents.add(new Document("3", -179, 0));
        documents.add(new Document("4", 180, 0));
        documents.add(new Document("5", -179, 1));
        documents.add(new Document("6", 179, 1));
        documents.add(new Document("7", 30, 0));
        documents.add(new Document("8", -40, 0));
        List<MemoryBKDTree> bkdTrees = MemoryBKDTree.createBKDTree(documents.toArray(new Document[documents.size()]));

        double minlon = 178;
        double maxlon = -178;
        double minLat = -2;
        double maxLat = 2;
        double[] lowerPoint = new double[]{minlon, minLat};
        double[] upperPoint = new double[]{maxlon, maxLat};

        ArrayList<Document> answer = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            if (BoundingBoxUtils.contains(upperPoint, lowerPoint, doc.point)) {
                answer.add(doc);
            }
        }
        List<Document> treeAnswer = new ArrayList<>();
        for (MemoryBKDTree tree : bkdTrees) {
            tree.contains(upperPoint, lowerPoint, treeAnswer);
        }
        assert answer.size() == 6;
        assert answer.size() == treeAnswer.size();
    }

    @Test
    public void testRandomContains() {
        Random random = new Random();
        int docs = random.nextInt(150000) + 150000;
        ArrayList<Document> documents = new ArrayList<>(docs);
        for (int i = 0; i<docs; i ++) {
            String val = Float.toString(random.nextFloat());
            double lon = random.nextDouble() * 360 -180;
            double lat = random.nextDouble() * 180 - 90;
            documents.add(new Document(val, lon, lat));
        }

        List<MemoryBKDTree> bkdTrees = MemoryBKDTree.createBKDTree(documents.toArray(new Document[documents.size()]));
        List<Document> treeAnswer = new ArrayList<>();
        for (int j= 0; j< 100; j ++) {
            double minlon = random.nextDouble() * 360 - 180;
            double maxlon =random.nextDouble() * 360 - 180;
            double height = random.nextDouble() * 90;
            double minLat = random.nextDouble() * 180 - 90;
            double maxLat = (minLat + height > 90) ? 90 : minLat + height;
            double[] lowerPoint = new double[]{minlon, minLat};
            double[] upperPoint = new double[]{maxlon, maxLat};
            ArrayList<Document> answer = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                if (BoundingBoxUtils.contains(upperPoint, lowerPoint, doc.point)) {
                    answer.add(doc);
                }
            }
            for (MemoryBKDTree tree : bkdTrees) {
                tree.contains(upperPoint, lowerPoint, treeAnswer);
            }
            assert answer.size() == treeAnswer.size() : "Expected: " + answer.size() + " got: " +  treeAnswer.size();
            treeAnswer.clear();
        }

    }
}