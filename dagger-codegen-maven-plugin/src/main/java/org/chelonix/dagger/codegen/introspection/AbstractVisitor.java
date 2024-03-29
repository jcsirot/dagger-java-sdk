package org.chelonix.dagger.codegen.introspection;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

abstract class AbstractVisitor {

    private Schema schema;
    private Charset encoding;
    private Path targetDirectory;

    public AbstractVisitor(Schema schema, Path targetDirectory, Charset encoding) {
        this.schema = schema;
        this.targetDirectory = targetDirectory;
        this.encoding = encoding;
    }

    void visit(Type type) throws IOException {
        TypeSpec typeSpec = generateType(type);
        JavaFile javaFile = JavaFile.builder("org.chelonix.dagger.client", typeSpec)
                .addFileComment("This class has been generated by dagger-java-sdk. DO NOT EDIT.")
                .indent("    ")
                .build();
        javaFile.writeTo(targetDirectory, encoding);
    }

    public Schema getSchema() {
        return schema;
    }

    abstract TypeSpec generateType(Type type);

}