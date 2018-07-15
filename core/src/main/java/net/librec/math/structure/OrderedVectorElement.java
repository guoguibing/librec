package net.librec.math.structure;

import com.google.common.primitives.Doubles;

/**
 * Ordered Vector Element
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public class OrderedVectorElement implements Comparable<OrderedVectorElement> {
    private final int index;
    private final double value;

    OrderedVectorElement(int index, double value) {
        this.index = index;
        this.value = value;
    }

    @Override
    public int compareTo(OrderedVectorElement that) {
        // both indexes are positive, and neither can be Integer.MAX_VALUE (otherwise there would be
        // an array somewhere with Integer.MAX_VALUE + 1 elements)
        return this.index - that.index;
    }

    @Override
    public int hashCode() {
        return index ^ Doubles.hashCode(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrderedVectorElement)) {
            return false;
        }
        OrderedVectorElement other = (OrderedVectorElement) o;
        return index == other.index && value == other.value;
    }

    public final int getIndex() {
        return index;
    }

    public final double getValue() {
        return value;
    }
}
