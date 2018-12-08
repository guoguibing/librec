package net.librec.math.structure;

import net.librec.common.IndexException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

/**
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */

public abstract class SequentialSparseVector extends AbstractVector {
    private static final Log LOG = LogFactory.getLog(AbstractVector.class);

    public SequentialSparseVector(int size) {
        super(size);
    }

    @Override
    public SequentialSparseVector clone() {
        try {
            SequentialSparseVector r = (SequentialSparseVector) super.clone();
            r.cardinality = cardinality;
            return r;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Can't happen");
        }
    }

    /**
     * Set the value at the given index, without checking bounds
     * Noted that for the DenseVector or RandomAccessSparseVector(Random Access Vector),
     * the position is as the same the index
     *
     * @param position an int position into the receiver
     * @param value    a double value to set
     * @throws IndexException if the index is out of bounds
     */
    public abstract void setAtPosition(int position, double value);

    /**
     * Return the value at the given index, without checking bounds
     * Noted that for the DenseVector or RandomAccessSparseVector(Random Access Vector),
     * the position is as the same the index
     *
     * @param position an int position
     * @return the double at the position
     * @throws IndexException if the index is out of bounds
     */
    public abstract double getAtPosition(int position);

    public abstract int getIndexAtPosition(int position);

    protected abstract void reshape();

    public abstract int[] getIndices();

    public abstract VectorEntry getVectorEntryAtPosition(int position);

    public double dot(SequentialSparseVector thatVector) {
        double value = 0.0D;
        Iterator<VectorEntry> thisIterator = this.iterator();
        Iterator<Vector.VectorEntry> thatIterator = thatVector.iterator();

        Vector.VectorEntry thisVectorEntry, thatVectorEntry;
        if (thisIterator.hasNext() && thatIterator.hasNext()) {
            thisVectorEntry = thisIterator.next();
            thatVectorEntry = thatIterator.next();

            while (thisIterator.hasNext() && thatIterator.hasNext()) {
                int thisIndex = thisVectorEntry.index();
                int thatIndex = thatVectorEntry.index();
                if (thisIndex == thatIndex) {
                    value += thisVectorEntry.get() * thatVectorEntry.get();
                    thisVectorEntry = thisIterator.next();
                    thatVectorEntry = thatIterator.next();
                } else if (thisIndex < thatIndex) {
                    thisVectorEntry = thisIterator.next();
                } else {
                    thatVectorEntry = thatIterator.next();
                }
            }
        }
        return value;
    }

    @Override
    public double getIteratorAdvanceCost() {
        return 1;
    }

    @Override
    public boolean isAddConstantTime() {
        return false;
    }

    /**
     * @return true
     */
    @Override
    public boolean isSequentialAccess() {
        return true;
    }

    @Override
    public double getLookupCost() {
        return Math.max(1, Math.round(Math.log(getNumEntries()) / Math.log(2)));
    }

    /**
     * @return false
     */
    @Override
    public boolean isDense() {
        return false;
    }

}
