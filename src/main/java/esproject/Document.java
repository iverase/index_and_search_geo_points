package esproject;

import java.util.Objects;

/**
 * Represents a document that can be indexed in a {@link KDBTree}.
 */
class Document {

    /** the string data **/
    final String data;
    /** the point */
    final double[] point;

    public Document(final String data, final double longitude, final double latitude) {
        this.data = data;
        this.point = new double[] {longitude, latitude};
    }

    @Override
    public int hashCode() {
        int result = this.data.hashCode();
        long temp;
        temp = Double.doubleToLongBits(this.point[0]);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(this.point[1]);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Document)) {
            return false;
        }
        Document other = (Document) obj;
        return Objects.equals(this.data, other.data)
                && this.point[0] == other.point[0]
                && this.point[1] == other.point[1];
    }

    @Override
    public String toString() {
        return "data: " + this.data + "; point: [" + this.point[0] + ", " + this.point[1] + "]";
    }
}
