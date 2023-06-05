package org.chelonix.dagger.codegen.introspection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Helpers {

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

    /**
     * returns true if the field returns an ID that should be converted into an object.
     */
    static boolean isIdToConvert(Field field) {
        return !"id".equals(field.getName()) &&
                field.getTypeRef().isScalar() &&
                field.getParentObject().getName().equals(customScalar.get(field.getTypeRef().getTypeName()));
    }

    static List<Field> getArrayField(Field field, Schema schema) {
        TypeRef fieldType = field.getTypeRef();
        if (! fieldType.isOptional()) {
            fieldType = fieldType.getOfType();
        }
        if (! fieldType.isList()) {
            throw new IllegalArgumentException("field is not a list");
        }
        fieldType = fieldType.getOfType();
        if (! fieldType.isOptional()) {
            fieldType = fieldType.getOfType();
        }
        final String typeName = fieldType.getName();
        Type schemaType = schema.getTypes().stream()
                .filter(t -> typeName.equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Schema type %s not found", typeName)));
        return schemaType.getFields().stream().filter(f -> f.getTypeRef().isScalar()).toList();
    }
}
