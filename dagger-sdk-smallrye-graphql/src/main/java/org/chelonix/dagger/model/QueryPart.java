package org.chelonix.dagger.model;

import io.smallrye.graphql.client.core.Argument;
import io.smallrye.graphql.client.core.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.smallrye.graphql.client.core.Argument.arg;
import static io.smallrye.graphql.client.core.Field.field;

class QueryPart {

    private String fieldName;
    private Map<String, ArgValue> arguments;

    QueryPart(String fieldName) {
        this(fieldName, new HashMap<>());
    }

    QueryPart(String fieldName, String argName, ArgValue argValue) {
        this.fieldName = fieldName;
        this.arguments = new HashMap<>() {{
            put(argName, argValue);
        }};
    }

    QueryPart(String fieldName, Map<String, ArgValue> arguments) {
        this.fieldName = fieldName;
        this.arguments = arguments;
    }

    String getOperation() {
        return fieldName;
    }

    Field toField() {
        List<Argument> argList = arguments.entrySet().stream().map(e -> arg(e.getKey(), e.getValue().serialize())).toList();
        return field(fieldName, argList);
    }
}
