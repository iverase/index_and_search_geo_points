package esproject;

/**
 * Methods for computing spatial relationship between two bounding boxes and a bounding box
 * and a point.
 */
class BoundingBoxUtils {

    /** bounding boxes are disjoint*/
    public static final int DISJOINT = 0;
    /** bounding boxes intersects*/
    public static final int INTERSECTS = 1;
    /** first bounding boxes contains second bounding box*/
    public static final int CONTAINS = 2;
    /** first bounding boxes is within second bounding box*/
    public static final int WITHIN = 3;

    /**
     * Checks if a bounding box is valid.
     *
     * @param upperPoint The left upper corner of the bounding box.
     * @param lowerPoint The right lower corner of the bounding box.
     * @return true if it i valid.
     */
    public static boolean checkBoundingBox(final double[] upperPoint, final double[] lowerPoint) {
        if (checkLongitude(upperPoint[0]) && checkLongitude(lowerPoint[0])
                && checkLatitude(lowerPoint[1]) && checkLatitude(lowerPoint[1])) {
            if (upperPoint[1] >= lowerPoint[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a longitude is valid.
     *
     * @param longitude the longitude to check.
     * @return true if valid.
     */
    public static boolean checkLongitude(double longitude) {
        return longitude >= -180 && longitude <= 180;
    }

    /**
     * Checks if a latitude is valid.
     *
     * @param latitude the latitude to check.
     * @return true if valid.
     */
    public static boolean checkLatitude(double latitude) {
        return latitude >= -90 && latitude <= 90;
    }

    /**
     * Checks if a point is inside of a bounding box.
     *
     * @param upperPoint The left upper corner of the bounding box.
     * @param lowerPoint The right lower corner of the bounding box.
     * @param point The point.
     * @return true if the point is inside the bounding box..
     */
    public static boolean contains(final double[] upperPoint, final double[] lowerPoint, final double[] point)
    {
        if(point[1] <= upperPoint[1] && point[1] >=  lowerPoint[1]) {
            double minX = lowerPoint[0];
            double maxX = upperPoint[0];
            double pX = point[0];
            double rawWidth = maxX - minX;
            if (rawWidth < 0.0D) {
                maxX = minX + rawWidth + 360.0D;
            }
            if (pX < minX) {
                pX += 360.0D;
            } else {
                if (pX <= maxX) {
                    return true;
                }
                pX -= 360.0D;
            }
            return pX >= minX && pX <= maxX ? true : false;
        } else {
            return false;
        }
    }

    /**
     * Computes the spatial relationship of the first provided bounding box with the second one.
     *
     * @param upperPoint1 The left upper corner of the first bounding box.
     * @param lowerPoint1 The right lower corner of the first bounding box.
     * @param upperPoint2 The left upper corner of the second bounding box.
     * @param lowerPoint2 The right lower corner of the second bounding box.
     * @return the relationship of the first bounding box with the second bounding box. One
     * of {@value DISJOINT}, {@value INTERSECTS}, {@value CONTAINS} or {@value WITHIN}.
     */
    public static int relate(final double[] upperPoint1, final double[] lowerPoint1, final double[] upperPoint2, final double[] lowerPoint2) {
        final int yIntersect = relate(lowerPoint1[1], upperPoint1[1],lowerPoint2[1], upperPoint2[1]);
        if(yIntersect == 0) {
            return DISJOINT;
        } else {
            final int xIntersect = relateXRange(lowerPoint1[0], upperPoint1[0],lowerPoint2[0], upperPoint2[0]);
            if(xIntersect == DISJOINT) {
                return DISJOINT;
            }
            if (xIntersect == yIntersect) {
                return xIntersect;
            }
            if(upperPoint1[1] == upperPoint2[1] && lowerPoint1[1] == lowerPoint2[1]) {
                return xIntersect;
            }
            if(upperPoint1[0] == upperPoint2[0] && lowerPoint1[0] == lowerPoint2[0]) {
                return yIntersect;
            }
            return INTERSECTS;
        }
    }

    private static int relateXRange(double coordMin1, double coordMax1, double coordMin2, double coordMax2) {
        double rawWidth1 = coordMax1 - coordMin1;
        if (rawWidth1 == 360.0D) {
            return 2;
        }

        if (rawWidth1 < 0.0D) {
            coordMax1 = coordMin1 + rawWidth1 + 360.0D;
        }

        double rawWidth2 = coordMax2 - coordMin2;
        if (rawWidth2 == 360.0D) {
            return 3;
        }

        if (rawWidth2 < 0.0D) {
            coordMax2 = coordMin2 + rawWidth2 + 360.0D;
        }

        if (coordMax1 < coordMin2) {
            coordMin1 += 360.0D;
            coordMax1 += 360.0D;
        } else if (coordMax2 < coordMin1) {
            coordMin2 += 360.0D;
            coordMax2 += 360.0D;
        }

        return relate(coordMin1, coordMax1, coordMin2, coordMax2);
    }

    private static int relate(final double coordMin1, final double coordMax1, final double coordMin2, final double coordMax2) {
        if (coordMin2 <= coordMax1 && coordMax2 >= coordMin1) {
            if (coordMin2 >= coordMin1 && coordMax2 <= coordMax1) {
                return CONTAINS;
            } else if (coordMin2 <= coordMin1 && coordMax2 >= coordMax1) {
                return WITHIN;
            } else {
                return INTERSECTS;
            }
        }
        return DISJOINT;
    }

    private BoundingBoxUtils() {
        //no instances
    }
}
