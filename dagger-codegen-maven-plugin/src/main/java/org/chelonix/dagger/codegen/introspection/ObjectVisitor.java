package org.chelonix.dagger.codegen.introspection;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.chelonix.dagger.codegen.introspection.Helpers.isIdToConvert;

class ObjectVisitor extends AbstractVisitor {
    public ObjectVisitor(Schema schema, Path targetDirectory, Charset encoding) {
        super(schema, targetDirectory, encoding);
    }

    private static MethodSpec withMethod(String var, TypeName type, TypeName returnType) {
        return MethodSpec.methodBuilder("with" + capitalize(var))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(type, var)
                .returns(returnType)
                .addStatement("this.arguments.$1L = $1L", var)
                .addStatement("return this")
                .build();
    }

    @Override
    TypeSpec generateType(Type type) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(Helpers.formatTypeName(type))
                .addJavadoc(type.getDescription())
                .addModifiers(Modifier.PUBLIC)
                //.addSuperinterface(ClassName.bestGuess("IdProvider"))
                .addField(FieldSpec.builder(ClassName.bestGuess("QueryBuilder"), "queryBuilder",Modifier.PRIVATE).build());

        if ("Query".equals(type.getName())) {
            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addParameter(ClassName.bestGuess("org.chelonix.dagger.client.engineconn.Connection"), "connection")
                    .addStatement("this.connection = connection")
                    .addStatement("this.queryBuilder = new QueryBuilder(connection.getGraphQLClient())")
                    .build();
            classBuilder.addMethod(constructor);
            classBuilder.addField(FieldSpec.builder(
                    ClassName.bestGuess("org.chelonix.dagger.client.engineconn.Connection"),
                    "connection",Modifier.PRIVATE).build());
            classBuilder.addSuperinterface(AutoCloseable.class);
            MethodSpec closeMethod = MethodSpec.methodBuilder("close")
                    .addException(Exception.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("this.connection.close()")
                    .build();
            classBuilder.addMethod(closeMethod);
        } else {
            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PROTECTED)
                    .addJavadoc("Empty constructor for JSON-B deserialization")
                    .build();
            classBuilder.addMethod(constructor);

            for (Field scalarField : type.getFields().stream().filter(f -> f.getTypeRef().isScalar()).toList()) {
                if ("id".equals(scalarField.getName())) {
                    classBuilder.addSuperinterface(ParameterizedTypeName.get(
                            ClassName.bestGuess("IdProvider"),
                            scalarField.getTypeRef().formatOutput()));
                }
                classBuilder.addField(scalarField.getTypeRef().formatOutput(), scalarField.getName(), Modifier.PRIVATE);
            }
        }

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameter(ClassName.bestGuess("QueryBuilder"), "queryBuilder")
                .addCode("this.queryBuilder = queryBuilder;")
                .build();
        classBuilder.addMethod(constructor);

        for (Field field: type.getFields())
        {
            if (field.hasOptionalArgs()) {
                buildFieldArgumentsHelpers(classBuilder, field, type);
            }

            if (field.hasOptionalArgs()) {
                buildFieldMethod(classBuilder, field, true);
            }
            buildFieldMethod(classBuilder, field, false);
        }

        ClassName thisType = ClassName.bestGuess(Helpers.formatTypeName(type));
        classBuilder.addMethod(MethodSpec.methodBuilder("with")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(UnaryOperator.class), thisType), "fun")
                .returns(thisType)
                .addStatement("return fun.apply(this)")
                .build());

        return classBuilder.build();
    }

    private void buildFieldMethod(TypeSpec.Builder classBuilder, Field field, boolean withOptionalArgs) {
        String fieldAlias = field.getName();
        // FIXME Reserved Word
        if ("import".equals(fieldAlias)) {
            fieldAlias = "importTarball";
        }

        MethodSpec.Builder fieldMethodBuilder = MethodSpec.methodBuilder(fieldAlias).addModifiers(Modifier.PUBLIC);
        TypeName returnType = "id".equals(field.getName()) ? field.getTypeRef().formatOutput() : field.getTypeRef().formatInput();
        fieldMethodBuilder.returns(returnType);
        List<ParameterSpec> mandatoryParams = field.getRequiredArgs().stream()
                .map(arg -> ParameterSpec.builder(
                        arg.getType().formatInput(),
                        arg.getName()).build())
                .toList();
        fieldMethodBuilder.addParameters(mandatoryParams);
        if (withOptionalArgs && field.hasOptionalArgs()) {
            fieldMethodBuilder.addParameter(ClassName.bestGuess(capitalize(field.getName()) + "Arguments"), "optArgs");
        }
        // Fix using '$' char in javadoc
        fieldMethodBuilder.addJavadoc(field.getDescription().replace("$", "$$"));
        field.getRequiredArgs().forEach(arg -> fieldMethodBuilder.addJavadoc("\n@param $L $L", arg.getName(), arg.getDescription()));

        if (field.getTypeRef().isScalar() && !isIdToConvert(field) && !"Query".equals(field.getParentObject().getName())) {
            fieldMethodBuilder.beginControlFlow("if (this.$L != null)", fieldAlias);
            fieldMethodBuilder.addStatement("return $L", fieldAlias);
            fieldMethodBuilder.endControlFlow();
        }
        if (field.hasArgs()) {
            fieldMethodBuilder.addStatement("Arguments.Builder builder = Arguments.newBuilder()");
        }
        field.getRequiredArgs().forEach(arg -> fieldMethodBuilder.addStatement("builder.add(\"$1L\", $1L)", arg.getName()));
        if (field.hasArgs()) {
            fieldMethodBuilder.addStatement("Arguments fieldArgs = builder.build()");
        }
        if (withOptionalArgs && field.hasOptionalArgs()) {
            fieldMethodBuilder.addStatement("fieldArgs = fieldArgs.merge(optArgs.toArguments())");
        }
        if (field.hasArgs()) {
            fieldMethodBuilder.addStatement("QueryBuilder nextQueryBuilder = this.queryBuilder.chain(\"$L\", fieldArgs)", field.getName());
        } else {
            fieldMethodBuilder.addStatement("QueryBuilder nextQueryBuilder = this.queryBuilder.chain(\"$L\")", field.getName());
        }

        if (field.getTypeRef().isListOfObject()) {
            List<Field> arrayFields = Helpers.getArrayField(field, getSchema());
            CodeBlock block = arrayFields.stream().map(f -> CodeBlock.of("$S", f.getName())).collect(CodeBlock.joining(",", "List.of(", ")"));
            fieldMethodBuilder.addStatement("nextQueryBuilder = nextQueryBuilder.chain($L)", block);
            fieldMethodBuilder.addStatement("return nextQueryBuilder.executeListQuery($L.class)",
                    field.getTypeRef().getListElementType().getName());
            fieldMethodBuilder
                    .addException(InterruptedException.class)
                    .addException(ExecutionException.class)
                    .addException(ClassName.bestGuess("DaggerQueryException"));
        } else if (field.getTypeRef().isList()) {
            fieldMethodBuilder.addStatement("return nextQueryBuilder.executeListQuery($L.class)",
                    field.getTypeRef().getListElementType().getName());
            fieldMethodBuilder
                    .addException(InterruptedException.class)
                    .addException(ExecutionException.class)
                    .addException(ClassName.bestGuess("DaggerQueryException"));
        } else if (isIdToConvert(field)) {
            fieldMethodBuilder.addStatement("nextQueryBuilder.executeQuery()");
            fieldMethodBuilder.addStatement("return this");
            fieldMethodBuilder
                    .addException(InterruptedException.class)
                    .addException(ExecutionException.class)
                    .addException(ClassName.bestGuess("DaggerQueryException"));
        } else if (field.getTypeRef().isObject()) {
            fieldMethodBuilder.addStatement("return new $L(nextQueryBuilder)", returnType);
        } else {
            fieldMethodBuilder.addStatement("return nextQueryBuilder.executeQuery($L.class)", returnType);
            fieldMethodBuilder
                    .addException(InterruptedException.class)
                    .addException(ExecutionException.class)
                    .addException(ClassName.bestGuess("DaggerQueryException"));
        }

        classBuilder.addMethod(fieldMethodBuilder.build());
    }

    /**
     * Builds the class containing the optional arguments and the associated builder.
     * @param classBuilder
     * @param field
     * @param type
     */
    private void buildFieldArgumentsHelpers(TypeSpec.Builder classBuilder, Field field, Type type) {
        String fieldArgumentsClassName = capitalize(field.getName())+"Arguments";
        String fieldArgumentsBuilderClassName = capitalize(field.getName())+"ArgumentsBuilder";

        /* Inner class XXXArguments */
        TypeSpec.Builder fieldArgumentsClassBuilder = TypeSpec
                .classBuilder(fieldArgumentsClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        List<FieldSpec> optionalArgFields = field.getOptionalArgs().stream()
                .map(arg -> FieldSpec.builder(
                        "id".equals(arg.getName()) && "Query".equals(type.getName()) ?
                                arg.getType().formatOutput() : arg.getType().formatInput(),
                        arg.getName()).build())
                .toList();
        fieldArgumentsClassBuilder.addFields(optionalArgFields);
        MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
        fieldArgumentsClassBuilder.addMethod(constructor);
        MethodSpec newBuilder = MethodSpec.methodBuilder("newBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.bestGuess(fieldArgumentsBuilderClassName))
                .addCode("$1L args = new $1L();\nreturn new $2L(args);", fieldArgumentsClassName, fieldArgumentsBuilderClassName)
                .build();
        fieldArgumentsClassBuilder.addMethod(newBuilder);

        List<CodeBlock> blocks = field.getOptionalArgs().stream()
                .map(arg -> CodeBlock.of("builder.add($1S, this.$1L);", arg.getName()))
                .toList();
        List<MethodSpec> optionalArgFieldGetter = field.getOptionalArgs().stream()
                .map(arg -> Helpers.getter(arg.getName(),
                        "id".equals(arg.getName()) && "Query".equals(type.getName()) ?
                        arg.getType().formatOutput() :
                        arg.getType().formatInput()))
                .toList();
        fieldArgumentsClassBuilder.addMethods(optionalArgFieldGetter);

        MethodSpec toArguments = MethodSpec.methodBuilder("toArguments")
                .returns(ClassName.bestGuess("Arguments"))
                .addStatement("Arguments.Builder builder = Arguments.newBuilder()")
                .addCode(CodeBlock.join(blocks, "\n"))
                .addStatement("\nreturn builder.build()")
                .build();
        fieldArgumentsClassBuilder.addMethod(toArguments);
        // fieldArgumentsClassBuilder.addJavadoc("Optional arguments for {@link $T#$L}\n\n", ClassName.bestGuess(field.getParentObject().getName()), field.getName());
        fieldArgumentsClassBuilder.addJavadoc("@see $T", ClassName.bestGuess(fieldArgumentsBuilderClassName));
        classBuilder.addType(fieldArgumentsClassBuilder.build());

        /* Inner class XXXArgumentsBuilder */
        TypeSpec.Builder fieldArgumentsBuilderClassBuilder = TypeSpec
                .classBuilder(fieldArgumentsBuilderClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        fieldArgumentsBuilderClassBuilder.addField(ClassName.bestGuess(fieldArgumentsClassName), "arguments", Modifier.PRIVATE);
        constructor = MethodSpec.constructorBuilder()
                .addParameter(ClassName.bestGuess(fieldArgumentsClassName), "arguments")
                .addStatement("this.arguments = arguments")
                .build();
        fieldArgumentsBuilderClassBuilder.addMethod(constructor);
        List<MethodSpec> optionalArgFieldWithMethods = field.getOptionalArgs().stream()
                .map(arg -> withMethod(
                        arg.getName(),
                        "id".equals(arg.getName()) && "Query".equals(type.getName()) ?
                                arg.getType().formatOutput() : arg.getType().formatInput(),
                        ClassName.bestGuess(fieldArgumentsBuilderClassName)))
                .toList();
        fieldArgumentsBuilderClassBuilder.addMethods(optionalArgFieldWithMethods);
        MethodSpec build = MethodSpec.methodBuilder("build").addModifiers(Modifier.PUBLIC)
                .returns(ClassName.bestGuess(fieldArgumentsClassName))
                .addStatement("return this.arguments")
                .build();
        fieldArgumentsBuilderClassBuilder.addMethod(build);
        fieldArgumentsBuilderClassBuilder.addJavadoc("A builder for {@link $T}", ClassName.bestGuess(fieldArgumentsClassName));

        classBuilder.addType(fieldArgumentsBuilderClassBuilder.build());
    }
}
