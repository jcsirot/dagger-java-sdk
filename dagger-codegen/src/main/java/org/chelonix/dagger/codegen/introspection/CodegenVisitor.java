package org.chelonix.dagger.codegen.introspection;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.chelonix.dagger.codegen.introspection.Helpers.isIdToConvert;

public class CodegenVisitor implements SchemaVisitor {

    private static final Map<String, String> customScalar = new HashMap<>() {{
        put("ContainerID", "Container");
        put("FileID", "File");
        put("DirectoryID", "Directory");
        put("SecretID", "Secret");
        put("SocketID","Socket");
        put("CacheVolumeID","Cache");
        put("ProjectID","Project");
        put("ProjectCommandID", "ProjectCommand");
    }};

    private static MethodSpec withMethod(String var, TypeName type, TypeName returnType) {
        return MethodSpec.methodBuilder("with" + capitalize(var))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(type, var)
                .returns(returnType)
                .addStatement("this.arguments.$1L = $1L", var)
                .addStatement("return this")
                .build();
    }

    private static MethodSpec getter(String var, TypeName type) {
        return MethodSpec.methodBuilder("get" + capitalize(var))
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement("return this.$L", var)
                .build();
    }

    private Function<String, Writer> writerProvider;

    public CodegenVisitor(Function<String, Writer> writerProvider) {
        this.writerProvider = writerProvider;
    }

    @Override
    public void visitScalar(Type type) {
        try (Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("templates/scalar.mustache"));
             Writer writer = writerProvider.apply(String.format("org/chelonix/dagger/sdk/client/%s.java", type.getName())))
        {
            Template tmpl = Mustache.compiler().escapeHTML(false).compile(reader);
            Map<String, Object> data = new HashMap<>(){{
                put("packageName", "org.chelonix.dagger.sdk.client");
                put("scalarType", "String");
                put("className", formatTypeName(type));
                put("classDescription", type.getDescription());
            }};
            writer.write(tmpl.execute(data));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    static class TypeScalarFieldContext {
        public String fieldName;
        public String fieldType;
        public String setter;

        public TypeScalarFieldContext(Field field) {
            this.fieldName = field.getName();
            this.fieldType = field.getTypeRef().formatOutput();
            this.setter = "set" + capitalize(field.getName());
        }
    }

    static class TypeFieldContext {
        public String fieldName;
        public String fieldAsClassName;
        public String fieldDescription;
        public String returnType;
        public List<FieldArgContext> args, optionalArgs, mandatoryArgs;
        public boolean hasArguments, hasOptionalArguments, hasMandatoryArguments;
        public boolean isScalar;
        public boolean continueChaining;
        public boolean executeQuery;
        public boolean returnList, notReturnList;
        public boolean isId;
        public boolean isIdType;
        public String returnListElementType;

        public TypeFieldContext(Field field) {
            this.fieldName = field.getName();
            this.fieldAsClassName = capitalize(field.getName());
            this.fieldDescription = field.getDescription().replace("\n", "<br/>");;
            this.isId = "id".equals(field.getName());
            this.isScalar = field.getTypeRef().isScalar();
            this.isIdType = !this.isId && this.isScalar && field.getParentObject().getName().equals(customScalar.get(field.getTypeRef().getTypeName()));
            this.returnType = isId ? field.getTypeRef().formatOutput() : field.getTypeRef().formatInput();
            this.args = field.getArgs().stream().map(FieldArgContext::new).toList();
            this.optionalArgs = field.getArgs().stream().map(FieldArgContext::new).filter(a-> a.isOptional).toList();
            this.mandatoryArgs = field.getArgs().stream().map(FieldArgContext::new).filter(a-> !a.isOptional).toList();
            this.hasArguments = !this.args.isEmpty();
            this.hasOptionalArguments = !this.optionalArgs.isEmpty();
            this.hasMandatoryArguments = !this.mandatoryArgs.isEmpty();
            this.continueChaining = !field.getTypeRef().isScalar() && !field.getTypeRef().isList();
            this.executeQuery = !continueChaining;
            this.returnList = field.getTypeRef().isList();
            this.notReturnList = !returnList;
            this.returnListElementType = field.getTypeRef().isList() ? field.getTypeRef().getListElementType().getName() : "null";
        }
    }

    static class FieldArgContext {
        public String argType;
        public String argName;
        public String argNameCapitalized;
        public String argDescription;
        public boolean isOptional;

        public FieldArgContext(InputValue arg) {
            this.argType = arg.getType().formatInput();
            this.argName = arg.getName();
            this.argNameCapitalized = capitalize(argName);
            this.argDescription = arg.getDescription().replace("\n", "<br/>");
            this.isOptional = arg.getType().isOptional();
        }
    }

    @Override
    public void visitObject(Type type, Schema schema) {
        try (Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("templates/object.mustache"));
             Writer writer = writerProvider.apply(String.format("org/chelonix/dagger/sdk/client/%s.java", type.getName())))
        {
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(formatTypeName(type))
                    .addJavadoc(type.getDescription())
                    .addModifiers(Modifier.PUBLIC)
                    .addField(FieldSpec.builder(ClassName.bestGuess("QueryContext"), "queryContext",Modifier.PRIVATE).build());

            if ("Query".equals(type.getName())) {
                MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.bestGuess("io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient"), "graphQLClient")
                        .addCode("this.queryContext = new QueryContext(graphQLClient);")
                        .build();
                classBuilder.addMethod(constructor);
            } else {
                MethodSpec constructor = MethodSpec.constructorBuilder()
                        .addJavadoc("Empty constructor for JSON-B deserialization")
                        .build();
                classBuilder.addMethod(constructor);

                for (Field scalarField : type.getFields().stream().filter(f -> f.getTypeRef().isScalar()).toList()) {
                    classBuilder.addField(ClassName.bestGuess(scalarField.getTypeRef().formatOutput()), scalarField.getName(), Modifier.PRIVATE);
                }
            }

            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addParameter(ClassName.bestGuess("QueryContext"), "queryContext")
                    .addCode("this.queryContext = queryContext;")
                    .build();
            classBuilder.addMethod(constructor);

            for (Field field: type.getFields())
            {
                if (field.hasOptionalArgs()) {
                    buildFieldArgumentsHelpers(classBuilder, field, type);
                }

                buildFieldMethod(classBuilder, field, type, schema);
            }

            JavaFile javaFile = JavaFile.builder("org.chelonix.dagger.sdk.client", classBuilder.build())
                    .build();

            javaFile.writeTo(writer);
            Template tmpl = Mustache.compiler().escapeHTML(false).compile(reader);
            //com.github.jknack.handlebars.Template tmpl = new Handlebars(new ClassPathTemplateLoader("/templates", ".mustache")).compile("object");
            Map<String, Object> data = new HashMap<>(){{
                put("packageName", "org.chelonix.dagger.sdk.client");
                put("className", formatTypeName(type));
                put("isClientClass", "Query".equals(type.getName()));
                put("isArgument", customScalar.containsValue(type.getName()));
                put("classDescription", type.getDescription());
                put("scalarFields", type.getFields().stream().filter(f -> f.getTypeRef().isScalar()).map(TypeScalarFieldContext::new).toList());
                put("fields", type.getFields().stream().map(TypeFieldContext::new).toList());
            }};
            //writer.write(tmpl.execute(data));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void buildFieldMethod(TypeSpec.Builder classBuilder, Field field, Type type, Schema schema) {
        String fieldAlias = field.getName();
        // FIXME Reserved Word
        if ("import".equals(fieldAlias)) {
            fieldAlias = "importTarball";
        }
        MethodSpec.Builder fieldMethodBuilder = MethodSpec.methodBuilder(fieldAlias).addModifiers(Modifier.PUBLIC);
        TypeName returnType = ClassName.bestGuess("id".equals(field.getName()) ? field.getTypeRef().formatOutput() : field.getTypeRef().formatInput());
        fieldMethodBuilder.returns(returnType);
        List<ParameterSpec> mandatoryParams = field.getRequiredArgs().stream()
                .map(arg -> ParameterSpec.builder(
                        ClassName.bestGuess(arg.getType().formatInput()),
                        arg.getName()).build())
                .toList();
        fieldMethodBuilder.addParameters(mandatoryParams);
        if (field.hasOptionalArgs()) {
            fieldMethodBuilder.addParameter(ClassName.bestGuess(capitalize(fieldAlias) + "Arguments"), "optArgs");
        }
        fieldMethodBuilder.addJavadoc(field.getDescription().replace("$", "$$"));
        field.getRequiredArgs().forEach(arg -> fieldMethodBuilder.addJavadoc("\n@param $L $L", arg.getName(), arg.getDescription()));

        if (field.getTypeRef().isScalar() && !isIdToConvert(field) && !"Query".equals(field.getParentObject().getName())) {
            fieldMethodBuilder.beginControlFlow("if (this.$L != null)", fieldAlias);
            fieldMethodBuilder.addStatement("return $L", fieldAlias);
            fieldMethodBuilder.endControlFlow();
        }
        if (field.hasArgs()) {
            fieldMethodBuilder.addStatement("Map<String, ArgValue> argMap = new HashMap<>()");
        }
        field.getRequiredArgs().forEach(arg -> fieldMethodBuilder.addStatement("argMap.put(\"$1L\", ArgValue.arg($1L))", arg.getName()));
        if (field.hasOptionalArgs()) {
            fieldMethodBuilder.addStatement("argMap.putAll(optArgs.toArguments())");
        }
        if (field.hasArgs()) {
            fieldMethodBuilder.addStatement("QueryContext ctx = this.queryContext.chain(\"$L\", optArgs)", field.getName());
        } else {
            fieldMethodBuilder.addStatement("QueryContext ctx = this.queryContext.chain(\"$L\")", field.getName());
        }

        if (field.getTypeRef().isListOfObject()) {
            // FIXME return List
            List<Field> arrayFields = Helpers.getArrayField(field, schema);
            CodeBlock block = arrayFields.stream().map(f -> CodeBlock.of("$S", f.getName())).collect(CodeBlock.joining(",","List.of(", ")"));
            fieldMethodBuilder.addStatement("ctx = ctx.chain($L)", block);
            fieldMethodBuilder.addStatement("return ctx.executeListQuery($L.class)", returnType);
        } else if (isIdToConvert(field)) {
            fieldMethodBuilder.addStatement("ctx.executeQuery()");
            fieldMethodBuilder.addStatement("return this");
        } else if (field.getTypeRef().isObject()) {
            fieldMethodBuilder.addStatement("return new $L(ctx)", returnType);
        } else {
            fieldMethodBuilder.addStatement("return ctx.executeQuery($L.class)", returnType);
        }

        classBuilder.addMethod(fieldMethodBuilder.build());
    }

    /**
     * Builds the class containing the optional arguments and the associated builder.
     * @param classBuilder
     * @param field
     * @param type
     */
    private static void buildFieldArgumentsHelpers(TypeSpec.Builder classBuilder, Field field, Type type) {
        String fieldArgumentsClassName = capitalize(field.getName())+"Arguments";
        String fieldArgumentsBuilderClassName = capitalize(field.getName())+"ArgumentsBuilder";
        TypeSpec.Builder fieldArgumentsClassBuilder = TypeSpec
                .classBuilder(fieldArgumentsClassName)
                .addModifiers(Modifier.PUBLIC);
        List<FieldSpec> optionalArgFields = field.getOptionalArgs().stream()
                .map(arg -> FieldSpec.builder(
                        ClassName.bestGuess("id".equals(arg.getName()) && "Query".equals(type.getName()) ?
                                arg.getType().formatOutput() : arg.getType().formatInput()),
                        arg.getName()).build())
                .toList();
        fieldArgumentsClassBuilder.addFields(optionalArgFields);
        MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
        fieldArgumentsClassBuilder.addMethod(constructor);
        MethodSpec newBuilder = MethodSpec.methodBuilder("newBuilder")
                .addModifiers(Modifier.STATIC)
                .returns(ClassName.bestGuess(fieldArgumentsBuilderClassName))
                .addCode("$1L args = new $1L()\nreturn new $2L(args);", fieldArgumentsClassName, fieldArgumentsBuilderClassName)
                .build();
        fieldArgumentsClassBuilder.addMethod(newBuilder);
        List<CodeBlock> blocks = field.getOptionalArgs().stream()
                .map(arg -> CodeBlock.of("map.put($1S), ArgValue.arg(this.$1L));", arg.getName()))
                .toList();
        List<MethodSpec> optionalArgFieldGetter = field.getOptionalArgs().stream()
                .map(arg -> getter(arg.getName(), ClassName.bestGuess("id".equals(arg.getName()) && "Query".equals(type.getName()) ?
                        arg.getType().formatOutput() :
                        arg.getType().formatInput())))
                .toList();
        fieldArgumentsClassBuilder.addMethods(optionalArgFieldGetter);

        MethodSpec toArguments = MethodSpec.methodBuilder("toArguments").addModifiers(Modifier.PUBLIC)
                .returns(ClassName.bestGuess("java.util.Map<String, ArgValue>"))
                .addStatement("HashMap<String, ArgValue> map = new HashMap<>()")
                .addCode(CodeBlock.join(blocks, "\n"))
                .addStatement("\nreturn map")
                .build();
        fieldArgumentsClassBuilder.addMethod(toArguments);
        classBuilder.addType(fieldArgumentsClassBuilder.build());

        TypeSpec.Builder fieldArgumentsBuilderClassBuilder = TypeSpec
                .classBuilder(fieldArgumentsBuilderClassName)
                .addModifiers(Modifier.PUBLIC);
        fieldArgumentsBuilderClassBuilder.addField(ClassName.bestGuess(fieldArgumentsClassName), "arguments", Modifier.PRIVATE);
        constructor = MethodSpec.constructorBuilder()
                .addParameter(ClassName.bestGuess(fieldArgumentsClassName), "arguments")
                .addStatement("this.arguments = arguments")
                .build();
        fieldArgumentsBuilderClassBuilder.addMethod(constructor);
        List<MethodSpec> optionalArgFieldWithMethods = field.getOptionalArgs().stream()
                .map(arg -> withMethod(
                        arg.getName(),
                        ClassName.bestGuess("id".equals(arg.getName()) && "Query".equals(type.getName()) ?
                                arg.getType().formatOutput() : arg.getType().formatInput()),
                        ClassName.bestGuess(fieldArgumentsBuilderClassName)))
                .toList();
        fieldArgumentsBuilderClassBuilder.addMethods(optionalArgFieldWithMethods);
        MethodSpec build = MethodSpec.methodBuilder("build").addModifiers(Modifier.PUBLIC)
                .returns(ClassName.bestGuess(fieldArgumentsClassName))
                .addStatement("return this.arguments")
                .build();
        fieldArgumentsBuilderClassBuilder.addMethod(build);

        classBuilder.addType(fieldArgumentsBuilderClassBuilder.build());
    }

    private static String formatTypeName(Type type) {
        if ("Query".equals(type.getName())) {
            return "Client";
        } else {
            return capitalize(type.getName());
        }
    }



    static final class InputValueContext {
        public String name;
        public String getter;
        public String setter;
        public String description;
        public String type;

        public InputValueContext(String name, String description, String type) {
            this.name = name;
            this.getter = ("Boolean".equals(type) ? "is" : "get") + capitalize(name);
            this.setter = "set" + capitalize(name);
            this.description = description;
            this.type = type;
        }
    }

    @Override
    public void visitInput(Type type) {
        try (Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("templates/input.mustache")))
        {
            Template tmpl = Mustache.compiler().escapeHTML(false).compile(reader);
            Map<String, Object> data = new HashMap<>(){{
                put("packageName", "org.chelonix.dagger.sdk.client");
                put("className", formatTypeName(type));
                put("classDescription", type.getDescription());
                put("fields", type.getInputFields().stream()
                        .map(v -> new InputValueContext(v.getName(), v.getDescription(), v.getType().formatOutput()))
                        .toList());
            }};
            System.out.println(tmpl.execute(data));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void visitEnum(Type type) {
        try (Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("templates/enum.mustache")))
        {
            Template tmpl = Mustache.compiler().escapeHTML(false).compile(reader);
            Map<String, Object> data = new HashMap<>(){{
                put("packageName", "org.chelonix.dagger.sdk.client");
                put("className", formatTypeName(type));
                put("classDescription", type.getDescription());
                put("fields", type.getEnumValues().stream().map(v -> v.getName().toUpperCase()).sorted().toList());
            }};
            System.out.println(tmpl.execute(data));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
