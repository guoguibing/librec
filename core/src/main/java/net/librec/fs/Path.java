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
package net.librec.fs;

public class Path implements Comparable {

    /**
     * The directory separator, a slash.
     */
    public static final String SEPARATOR = "/";

    private static final String CUR_DIR = "/";

    /**
     * this is always an absolute path
     */
    private String path;

    /**
     * Resolve a child path against a parent path.
     *
     * @param parent the parent path
     * @param child  the child path
     */
    public Path(String parent, String child) {
        this(new Path(parent), new Path(child));
    }


    /**
     * Resolve a child path against a parent path.
     *
     * @param parent the parent path
     * @param child  the child path
     */
    public Path(Path parent, String child) {
        this(parent, new Path(child));
    }

    /**
     * Resolve a child path against a parent path.
     *
     * @param parent the parent path
     * @param child  the child path
     */
    public Path(String parent, Path child) {
        this(new Path(parent), child);
    }

    /**
     * Resolve a child path against a parent path.
     *
     * @param parent the parent path
     * @param child  the child path
     */
    public Path(Path parent, Path child) {
        if (child.isAbsolute()) {
            path = child.path;
        }

        StringBuilder sb = new StringBuilder(parent.path.length() + 1 + child.path.length());
        sb.append(parent.path);
        if (!parent.path.equals("/")) {
            sb.append("/");
        }
        sb.append(child);
        path = sb.toString();
    }

    /**
     * Check if the input path is absolute path.
     *
     * @param path the input path
     * @return true if the input path is absolute path.
     */
    private static boolean isAbsolutePath(String path) {
        return path.startsWith(SEPARATOR);
    }

    /**
     * Check if the input path is empty or null.
     *
     * @param path the input path
     * @throws IllegalArgumentException if the input path is illegal
     */
    private void checkPathArg(String path) throws IllegalArgumentException {
        // disallow construction of a Path from an empty string
        if (path == null) {
            throw new IllegalArgumentException(
                    "Can not create a Path from a null string");
        }
        if (path.length() == 0) {
            throw new IllegalArgumentException(
                    "Can not create a Path from an empty string");
        }
    }

    /**
     * Construct a path from a String.  Path strings are URIs, but with
     * unescaped elements and some additional normalization.
     *
     * @param pathString string of the path
     * @throws IllegalArgumentException if illegal argument error occurs
     */
    public Path(String pathString) throws IllegalArgumentException {
        checkPathArg(pathString);

        path = normalizePath(pathString);
    }

    /**
     * Normalize a path string to use non-duplicated forward slashes as
     * the path separator and remove any trailing path separators.
     *
     * @param path the input path
     * @return Normalized path string.
     */
    private static String normalizePath(String path) {
        // Remove double forward slashes.
        path = path.replaceAll("//", "/");

        // trim trailing slash from non-root path
        int minLength = 1;
        if (path.length() > minLength && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * @return the number of elements in this path.
     */
    public int depth() {
        int depth = 0;
        int slash = path.length() == 1 && path.charAt(0) == '/' ? -1 : 0;
        while (slash != -1) {
            depth++;
            slash = path.indexOf(SEPARATOR, slash + 1);
        }
        return depth;
    }

    public boolean isAbsolute() {
        return isAbsolutePath(path);
    }

    /**
     * @return true if and only if this path represents the root of a file system
     */
    public boolean isRoot() {
        return getParent() == null;
    }

    /**
     * @return the final component of this path.
     */
    public String getName() {
        int slash = path.lastIndexOf(SEPARATOR);
        return path.substring(slash + 1);
    }

    /**
     * @return the parent of a path or null if at root.
     */
    public Path getParent() {
        int lastSlash = path.lastIndexOf('/');
        int start = 0;
        if ((path.length() == start) ||               // empty path
                (lastSlash == start && path.length() == start + 1)) { // at root
            return null;
        }
        String parent;
        if (lastSlash == -1) {
            parent = CUR_DIR;
        } else {
            int end = 0;
            parent = path.substring(0, lastSlash == end ? end + 1 : lastSlash);
        }
        return new Path(parent);
    }

    /**
     * Adds a suffix to the final name in the path.
     *
     * @param suffix a suffix to the final name in the path
     * @return the path with suffix
     */
    public Path suffix(String suffix) {
        return new Path(getParent(), getName() + suffix);
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Path)) {
            return false;
        }
        Path that = (Path) o;
        return this.path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int compareTo(Object o) {
        Path that = (Path) o;
        return this.path.compareTo(that.path);
    }
}
