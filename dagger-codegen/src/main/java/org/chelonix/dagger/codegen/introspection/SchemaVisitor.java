package org.chelonix.dagger.codegen.introspection;

public interface SchemaVisitor {

    void visitScalar(Type type);

    void visitObject(Type type, Schema schema);

    void visitInput(Type type);

    void visitEnum(Type type);
}
