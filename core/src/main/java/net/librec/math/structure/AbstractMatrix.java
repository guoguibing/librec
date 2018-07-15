package net.librec.math.structure;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A few universal implementations of convenience functions for a JVM-backed matrix.
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public abstract class AbstractMatrix implements Matrix, DataSet {
    protected final Log LOG = LogFactory.getLog(this.getClass());

    protected int rows;
    protected int columns;

    public AbstractMatrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    public int columnSize() {
        return columns;
    }

    @Override
    public int size(){
        return this.getNumEntries();
    }

    @Override
    public int rowSize() {
        return rows;
    }

    @Override
    public AbstractMatrix clone(){
        try {
            return (AbstractMatrix) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        int maxRowsToDisplay = 10;

        StringBuilder s = new StringBuilder("{\n");
        for (int rowIndex = 0; rowIndex < maxRowsToDisplay && rowIndex < rowSize(); rowIndex++) {
            Vector vector = row(rowIndex);
            s.append(" ").append(rowIndex)
                    .append(" =>\t")
                    .append(vector.toString())
                    .append('\n');
        }

        String returnString = s.toString();
        if (maxRowsToDisplay <= rowSize())
            return returnString + ("... }");
        else {
            return returnString + ("}");
        }
    }
}
