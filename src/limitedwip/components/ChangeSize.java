package limitedwip.components;

public class ChangeSize {
    public final int value;
    public final boolean timedOut;

    public ChangeSize(int value) {
        this(value, false);
    }

    public ChangeSize(int value, boolean timedOut) {
        this.value = value;
        this.timedOut = timedOut;
    }

    public ChangeSize add(ChangeSize that) {
        return new ChangeSize(this.value + that.value, this.timedOut | that.timedOut);
    }

    @Override public String toString() {
        String result = "ChangeSize(" + value;
        if (timedOut) result += ", timedOut";
        return result + ")";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChangeSize that = (ChangeSize) o;

        return value == that.value && timedOut == that.timedOut;
    }

    @Override public int hashCode() {
        int result = value;
        result = 31 * result + (timedOut ? 1 : 0);
        return result;
    }
}
