package org.chelonix.dagger.model;

import io.smallrye.graphql.client.core.Field;

import static io.smallrye.graphql.client.core.Field.field;

class QueryPart {

    private String fieldName;
    private Arguments arguments;

    QueryPart(String fieldName) {
        this(fieldName, Arguments.noArgs());
    }

    QueryPart(String fieldName, Arguments arguments) {
        this.fieldName = fieldName;
        this.arguments = arguments;
    }

    String getOperation() {
        return fieldName;
    }

    Field toField() {
        //List<Argument> argList = arguments.entrySet().stream().map(e -> arg(e.getKey(), e.getValue().serialize())).toList();
        return Field.field(fieldName, arguments.toList());
    }
}
