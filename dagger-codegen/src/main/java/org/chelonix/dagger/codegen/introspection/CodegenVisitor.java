package org.chelonix.dagger.codegen.introspection;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.capitalize;

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
            this.fieldType = formatType(field.getTypeRef(), false);
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
            this.returnType = isId ? formatOutputType(field.getTypeRef()) : formatInputType(field.getTypeRef());
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
            this.argType = formatInputType(arg.getType());
            this.argName = arg.getName();
            this.argNameCapitalized = capitalize(argName);
            this.argDescription = arg.getDescription().replace("\n", "<br/>");
            this.isOptional = arg.getType().isOptional();
        }
    }

    @Override
    public void visitObject(Type type) {
        try (Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("templates/object.mustache"));
             Writer writer = writerProvider.apply(String.format("org/chelonix/dagger/sdk/client/%s.java", type.getName())))
        {
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
            writer.write(tmpl.execute(data));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static String formatTypeName(Type type) {
        if ("Query".equals(type.getName())) {
            return "Client";
        } else {
            return capitalize(type.getName());
        }
    }

    private static String formatOutputType(TypeRef typeRef) {
        return formatType(typeRef, false);
    }

    private static String formatInputType(TypeRef typeRef) {
        return formatType(typeRef, true);
    }

    private static String formatType(TypeRef typeRef, boolean isInput) {
        if (typeRef == null) {
            return "void";
        }
        if ("Query".equals(typeRef.getName())) {
            return "Client";
        }
        switch (typeRef.getKind()) {
            case SCALAR -> {
                switch (typeRef.getName()) {
                    case "String" -> {
                        return "String";
                    }
                    case "Boolean" -> {
                        return "Boolean";
                    }
                    case "Int" -> {
                        return "Integer";
                    }
                    default -> {
                        if (typeRef.getName().endsWith("ID") && isInput) {
                            return typeRef.getName().substring(0, typeRef.getName().length() - 2);
                        }
                        return typeRef.getName();
                    }
                }
            }
            case OBJECT, ENUM, INPUT_OBJECT -> {
                return typeRef.getName();
            }
            case LIST -> {
                return String.format("List<%s>", formatType(typeRef.getOfType(), isInput));
            }
            default -> {
                return formatType(typeRef.getOfType(), isInput);
            }
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
                        .map(v -> new InputValueContext(v.getName(), v.getDescription(), formatType(v.getType(), false)))
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
