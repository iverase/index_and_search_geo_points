package esproject;

import java.util.Objects;

/**
 * Represents a document that can be indexed in a {@link MemoryBKDTree}.
 */
class Document {

    /** the string data **/
    final String data;
    /** the point */
    final double[] point;

    public Document(final String data, final double longitude, final double latitude) {
        this.data = data;
        point = new double[] {longitude, latitude};
    }

    @Override
    public int hashCode() {
        int result = data.hashCode();
        long temp;
        temp = Double.doubleToLongBits(point[0]);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(point[1]);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Document)) {
            return false;
        }
        Document other = (Document) obj;
        return Objects.equals(data, other.data)
                && point[0] == other.point[0]
                && point[1] == other.point[1];
    }

    @Override
    public String toString() {
        return "data: " + data + "; point: [" + point[0] + ", " + point[1] + "]";
    }
}
