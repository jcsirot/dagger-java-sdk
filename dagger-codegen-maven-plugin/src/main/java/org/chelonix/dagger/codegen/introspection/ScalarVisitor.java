package org.chelonix.dagger.codegen.introspection;

import com.squareup.javapoet.*;
import com.sun.jdi.ClassType;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Function;

class ScalarVisitor extends AbstractVisitor {
    public ScalarVisitor(Schema schema, Path targetDirectory, Charset encoding) {
        super(schema, targetDirectory, encoding);
    }

    @Override
    TypeSpec generateType(Type type) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(Helpers.formatTypeName(type))
                .addJavadoc(type.getDescription())
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(
                        ClassName.bestGuess("Scalar"),
                        ClassName.get(String.class)));

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameter(ClassName.get(String.class), "value")
                .addStatement("super(value)").build();

        classBuilder.addMethod(constructor);

        return classBuilder.build();
    }
}
