/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.common;

/**
 * Librec Exception
 *
 * @author WangYuFeng
 */
public class LibrecException extends Exception {

    private static final long serialVersionUID = 6348898228892052525L;

    public LibrecException() {
        super();
    }

    public LibrecException(String message, Throwable cause) {
        super(message, cause);
    }

    public LibrecException(String message) {
        super(message);
    }

    public LibrecException(Throwable cause) {
        super(cause);
    }
}
