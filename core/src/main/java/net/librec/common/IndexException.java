package net.librec.common;

/**
 * Exception thrown when a matrix or vector is accessed at an index, or dimension,
 * which does not logically exist in the entity.
 *
 * @author Keqiang Wang
 */
public class IndexException extends IllegalArgumentException {

    public IndexException(int index, int cardinality) {
        super("Index " + index + " is outside allowable range of [0," + cardinality + ')');
    }

    public IndexException(String exceptionMsg) {
        super(exceptionMsg);
    }

}