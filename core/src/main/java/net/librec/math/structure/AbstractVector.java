package net.librec.math.structure;

import com.google.common.primitives.Doubles;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

/**
 * Implementations of generic capabilities like sum of VectorElements and dot products
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public abstract class AbstractVector implements Vector {
    private static final Log LOG = LogFactory.getLog(AbstractVector.class);

    protected int cardinality;

    public AbstractVector(int cardinality) {
        this.cardinality = cardinality;
    }


    @Override
    public VectorEntry getVectorEntry(int index) {
        return new LocalVectorEntry(index);
    }


    @Override
    public double norm(double power) {
        if (power < 0.0) {
            throw new IllegalArgumentException("Power must be >= 0");
        }
        // We can special case certain powers.
        if (Double.isInfinite(power)) {
            double result = 0.0D;
            for (VectorEntry vectorEntry : this) {
                result = Math.max(result, Math.abs(vectorEntry.get()));
            }
            return result;
        } else if (power == 2.0) {
            return Math.sqrt(getLengthSquared());
        } else if (power == 1.0) {
            double result = 0.0D;
            for (VectorEntry vectorEntry : this) {
                result += Math.abs(vectorEntry.get());
            }
            return result;
        } else if (power == 0.0D) {
            return getNumEntries();
        } else {
            double result = 0.0D;
            for (VectorEntry vectorEntry : this) {
                result += Math.pow(vectorEntry.get(), power);
            }
            result = Math.pow(result, 1.0 / power);
            return result;
        }
    }

    @Override
    public double sum() {
        double result = 0.0D;
        for (VectorEntry vectorEntry : this) {
            result += vectorEntry.get();
        }
        return result;
    }


    public double mean() {
        return sum() / getNumEntries();
    }

    protected double dotSelf() {
        double result = 0.0D;
        for (VectorEntry vectorEntry : this) {
            result += Math.pow(vectorEntry.get(), 2.0D);
        }
        return result;
    }

    @Override
    public double getLengthSquared() {
        return dotSelf();
    }


    @Override
    public final int size() {
        return getNumEntries();
    }

    public final int cardinality(){
        return cardinality;
    }

    @Override
    public int hashCode() {
        int result = cardinality;
        for (VectorEntry entry : this) {
            result += entry.index() * Doubles.hashCode(entry.get());
        }
        return result;
    }

    /**
     * Determines whether this {@link Vector} represents the same logical vector as another
     * object. Two {@link Vector}s are equal (regardless of implementation) if the value at
     * each index is the same, and the cardinalities are the same.
     */
    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String[] dictionary) {
        StringBuilder result = new StringBuilder();
        result.append('{');
        for (int index = 0; index < cardinality; index++) {
            double value = get(index);
            if (value != 0.0) {
                result.append(dictionary != null && dictionary.length > index ? dictionary[index] : index);
                result.append(':');
                result.append(value);
                result.append(',');
            }
        }
        if (result.length() > 1) {
            result.setCharAt(result.length() - 1, '}');
        } else {
            result.append('}');
        }
        return result.toString();
    }

    /**
     * toString() implementation for sparse vectors via {@link #iterator()} method
     *
     * @return String representation of the vector
     */
    public String sparseVectorToString() {
        Iterator<VectorEntry> iter = iterator();
        if (!iter.hasNext()) {
            return "{}";
        } else {
            StringBuilder result = new StringBuilder();
            result.append('{');
            while (iter.hasNext()) {
                VectorEntry vectorEntry = iter.next();
                result.append(vectorEntry.index());
                result.append(':');
                result.append(vectorEntry.get());
                result.append(',');
            }
            result.setCharAt(result.length() - 1, '}');
            return result.toString();
        }
    }

    protected final class LocalVectorEntry implements VectorEntry {
        int index;

        LocalVectorEntry(int index) {
            this.index = index;
        }

        @Override
        public double get() {
            return AbstractVector.this.get(index);
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public int position(){
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(double value) {
            AbstractVector.this.set(index, value);
        }
    }
}
