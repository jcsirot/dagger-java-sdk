package org.chelonix.dagger.model;

public class DirectoryID extends Scalar<String> {

    public static DirectoryID of(String value) {
        return new DirectoryID(value);
    }

    public DirectoryID(String value) {
        super(value);
    }
}
