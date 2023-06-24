package org.chelonix.dagger.client;

import io.smallrye.graphql.client.GraphQLError;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DaggerQueryException extends Exception {

    private GraphQLError[] errors;

    public DaggerQueryException(GraphQLError... errors) {
        super(Arrays.stream(errors).map(GraphQLError::getMessage).collect(Collectors.joining(" ")));
        this.errors = errors;
    }

    public GraphQLError[] getErrors() {
        return errors;
    }
}
