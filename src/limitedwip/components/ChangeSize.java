package limitedwip.components;

public class ChangeSize {
    public final int value;
    public final boolean isApproximate;

    public ChangeSize(int value) {
        this(value, false);
    }

    public ChangeSize(int value, boolean isApproximate) {
        this.value = value;
        this.isApproximate = isApproximate;
    }

    public ChangeSize add(ChangeSize that) {
        return new ChangeSize(this.value + that.value, this.isApproximate | that.isApproximate);
    }

    @Override public String toString() {
        String result = "ChangeSize(" + value;
        if (isApproximate) result += ", isApproximate";
        return result + ")";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChangeSize that = (ChangeSize) o;

        return value == that.value && isApproximate == that.isApproximate;
    }

    @Override public int hashCode() {
        int result = value;
        result = 31 * result + (isApproximate ? 1 : 0);
        return result;
    }
}
