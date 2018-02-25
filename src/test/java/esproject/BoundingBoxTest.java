package esproject;


import org.junit.Test;

/**
 * Bounding box tests.
 */
public class BoundingBoxTest {

    @Test
    public void testWithinContains(){
        double maxLon1 = 2;
        double minLon1 = -2;
        double maxLat1 = 2;
        double minLat1 = -2;

        double maxLon2 = 1;
        double minLon2 = -1;
        double maxLat2 = 1;
        double minLat2 = -1;

        int rel = BoundingBoxUtils.relate(new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1}, new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2});
        assert rel == BoundingBoxUtils.CONTAINS;
        rel = BoundingBoxUtils.relate(new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2}, new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1});
        assert rel == BoundingBoxUtils.WITHIN;
    }

    @Test
    public void testWithinContains2(){
        double maxLon1 = 179.99505379082706;
        double minLon1 = 90.36496823562595;
        double maxLat1 = 89.99893486735135;
        double minLat1 = -89.98343477835365;

        double maxLon2 = 179.99505379082706;
        double minLon2 = -0.9484494133937176;
        double maxLat2 = 89.99893486735135;
        double minLat2 = -89.98343477835365;

        int rel = BoundingBoxUtils.relate(new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1}, new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2});
        assert rel == BoundingBoxUtils.WITHIN;
        rel = BoundingBoxUtils.relate(new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2}, new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1});
        assert rel == BoundingBoxUtils.CONTAINS;
    }

    @Test
    public void testWithinContains3(){
        double maxLon1 = -67.57643224359754;
        double minLon1 = -73.23141859943509;
        double maxLat1 = 78.50178086592939;
        double minLat1 = 67.2855473555206;

        double maxLon2 = -67.57643224359754;
        double minLon2 = -73.23141859943509;
        double maxLat2 = 89.99903508225057;
        double minLat2 = 67.2855473555206;

        int rel = BoundingBoxUtils.relate(new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1}, new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2});
        assert rel == BoundingBoxUtils.WITHIN;
        rel = BoundingBoxUtils.relate(new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2}, new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1});
        assert rel == BoundingBoxUtils.CONTAINS;
    }


    @Test
    public void testIntersects(){
        double maxLon1 = 2;
        double minLon1 = -2;
        double maxLat1 = 2;
        double minLat1 = -2;

        double maxLon2 = 3;
        double minLon2 = 0;
        double maxLat2 = 3;
        double minLat2 = 0;

        int rel = BoundingBoxUtils.relate(new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1}, new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2});
        assert rel == BoundingBoxUtils.INTERSECTS;
        rel = BoundingBoxUtils.relate(new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2}, new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1});
        assert rel == BoundingBoxUtils.INTERSECTS;
    }

    @Test
    public void testDisjoint(){
        double maxLon1 = 2;
        double minLon1 = -2;
        double maxLat1 = 2;
        double minLat1 = -2;

        double maxLon2 = 13;
        double minLon2 = 12;
        double maxLat2 = 12;
        double minLat2 = 11;

        int rel = BoundingBoxUtils.relate(new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1}, new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2});
        assert rel == BoundingBoxUtils.DISJOINT;
        rel = BoundingBoxUtils.relate(new double[]{maxLon2, maxLat2}, new double[]{minLon2, minLat2}, new double[]{maxLon1, maxLat1}, new double[]{minLon1, minLat1});
        assert rel == BoundingBoxUtils.DISJOINT;
    }
}
