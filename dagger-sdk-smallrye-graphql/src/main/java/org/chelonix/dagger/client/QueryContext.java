package org.chelonix.dagger.client;

import com.jayway.jsonpath.JsonPath;
import io.smallrye.graphql.client.GraphQLError;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.core.Document;
import io.smallrye.graphql.client.core.Field;
import io.smallrye.graphql.client.core.FieldOrFragment;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.Operation.operation;

class QueryContext {

    private final DynamicGraphQLClient client;
    private final Deque<QueryPart> parts;
    private List<QueryPart> leaves;

    QueryContext(DynamicGraphQLClient client) {
        this(client, new LinkedList<>());
    }

    private QueryContext(DynamicGraphQLClient client, Deque<QueryPart> parts) {
        this(client, parts, new ArrayList<>());
    }

    private QueryContext(DynamicGraphQLClient client, Deque<QueryPart> parts, List<String> finalFields) {
        this.client = client;
        this.parts = parts;
        this.leaves = finalFields.stream().map(QueryPart::new).toList();
    }

    QueryContext chain(String operation) {
        return chain(operation, Arguments.noArgs());
    }

    QueryContext chain(String operation, Arguments arguments) {
        if (leaves != null && !leaves.isEmpty()) {
            throw new IllegalStateException("A new field cannot be chained");
        }
        Deque<QueryPart> list = new LinkedList<>();
        list.addAll(this.parts);
        list.push(new QueryPart(operation, arguments));
        return new QueryContext(client, list);
    }

    public QueryContext chain(String operation, List<String> leaves) {
        if (!this.leaves.isEmpty()) {
            throw new IllegalStateException("A new field cannot be chained");
        }
        Deque<QueryPart> list = new LinkedList<>();
        list.addAll(this.parts);
        list.push(new QueryPart(operation));
        return new QueryContext(client, list, leaves);
    }

    public QueryContext chain(List<String> leaves) {
        if (!this.leaves.isEmpty()) {
            throw new IllegalStateException("A new field cannot be chained");
        }
        Deque<QueryPart> list = new LinkedList<>();
        list.addAll(this.parts);
        return new QueryContext(client, list, leaves);
    }

    <T> T executeQuery(Class<T> klass) throws ExecutionException, InterruptedException, DaggerQueryException {
        String path = StreamSupport.stream(Spliterators.spliteratorUnknownSize(parts.descendingIterator(), 0), false)
                .map(QueryPart::getOperation)
                .collect(Collectors.joining("."));
        System.out.println(path);
        Response response = executeQuery();
        if (response.hasError()) {
            List<GraphQLError> errors = response.getErrors();
            throw new DaggerQueryException(errors.toArray(new GraphQLError[0]));
        }
        if (Scalar.class.isAssignableFrom(klass)) {
            // FIXME scalar could not be other types than String in the future
            String value = JsonPath.parse(response.getData().toString()).read(path, String.class);
            try {
                return klass.getConstructor(String.class).newInstance(value);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException nsme) {
                // FIXME
                throw new RuntimeException(nsme);
            }
        } else {
            return JsonPath.parse(response.getData().toString()).read(path, klass);
        }
    }

    Response executeQuery() throws ExecutionException, InterruptedException, DaggerQueryException {
        Field leafField = parts.pop().toField();
        leafField.setFields(leaves.stream().<FieldOrFragment>map(qp -> field(qp.getOperation())).toList());
        List<Field> fields = new ArrayList<>();
        for (QueryPart qp: parts) {
            fields.add(qp.toField());
        }
        Field operation = fields.stream().reduce(leafField, (acc, field) -> {
            field.setFields(List.of(acc));
            return field;
        });
        Document query = document(operation(operation));
        System.out.println(String.format("Running query: %s", query.build()));
        return client.executeSync(query);
    }

    <T> List<T> executeListQuery(Class<T> klass) throws ExecutionException, InterruptedException, DaggerQueryException {
        List<String> pathElts = StreamSupport.stream(Spliterators.spliteratorUnknownSize(parts.descendingIterator(), 0), false)
                .map(QueryPart::getOperation)
                .toList();
        Field leafField = parts.pop().toField();
        leafField.setFields(leaves.stream().<FieldOrFragment>map(qp -> field(qp.getOperation())).toList());
        List<Field> fields = new ArrayList<>();
        for (QueryPart qp: parts) {
            fields.add(qp.toField());
        }
        Field operation = fields.stream().reduce(leafField, (acc, field) -> {
            field.setFields(List.of(acc));
            return field;
        });
        Document query = document(operation(operation));
        System.out.println(query.build());
        Response response = client.executeSync(query);
        System.out.println(response);

        JsonObject obj = response.getData();
        for (int i = 0; i < pathElts.size() - 1; i++) {
            obj = obj.getJsonObject(pathElts.get(i));
        }
        JsonArray array = obj.getJsonArray(pathElts.get(pathElts.size() - 1));
        JsonbConfig config = new JsonbConfig().withPropertyVisibilityStrategy(new PrivateVisibilityStrategy());
        Jsonb jsonb = JsonbBuilder.newBuilder().withConfig(config).build();
        List<T> rv = jsonb.fromJson(array.toString(), TypeUtils.parameterize(List.class, klass));
        return rv;
    }
}
