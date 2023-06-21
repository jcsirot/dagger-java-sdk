package org.chelonix.dagger.model;

import io.smallrye.graphql.client.core.Argument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.smallrye.graphql.client.core.Argument.arg;

public class Arguments {

    private Map<String, Object> args;

    static Arguments noArgs() {
        return new Arguments();
    }

    public static Builder newBuilder() {
        return new Arguments().new Builder();
    }

    private Arguments() {
        this(new HashMap<>());
    }

    private Arguments(Map<String, Object> args) {
        this.args = args;
    }

    private Builder builder() {
        return new Builder();
    }

    Arguments merge(Arguments other) {
        HashMap<String, Object> newMap = new HashMap<>(this.args);
        newMap.putAll(other.args);
        return new Arguments(newMap);
    }

    List<Argument> toList()  {
        return args.entrySet().stream().map(e -> Argument.arg(e.getKey(), toArgumentValue(e.getValue()))).toList();
    }

    private Object toArgumentValue(Object value) {
        if (value instanceof Scalar<?>) {
            return ((Scalar<?>) value).convert();
        } else if (value instanceof IdProvider<?>) {
            try {
                Object id = ((IdProvider<?>) value).id();
                if (id instanceof Scalar<?>) {
                    return ((Scalar<?>) id).convert();
                } else {
                    return id;
                }
            } catch (ExecutionException e) {
                throw new ArgumentSerializeException(e);
            } catch (InterruptedException e) {
                throw new ArgumentSerializeException(e);
            }
        } else if (value instanceof InputValue) {
            return ((InputValue) value).toMap();
        } else if (value instanceof String || value instanceof Integer || value instanceof Long || value instanceof Boolean ) {
            return value;
        } else if (value instanceof List<?>) {
            return ((List<?>) value).stream().map(v -> toArgumentValue(v)).toList();
        } else if (value instanceof Enum<?>) {
            return ((Enum<?>) value).toString();
        } else {
            throw new IllegalStateException(
                    String.format(
                            "Argument is not an authorized argument type. Found type is %s",
                            value.getClass()));
        }
    }

    public class Builder {
        private Builder() {
        }

        public Builder add(String name, String value) {
            args.put(name, value);
            return this;
        }

        public Builder add(String name, String... value) {
            args.put(name, value);
            return this;
        }

        public Builder add(String name, boolean value) {
            args.put(name, value);
            return this;
        }

        public Builder add(String name, int value) {
            args.put(name, value);
            return this;
        }

        public <T> Builder add(String name, Scalar<T> value) {
            args.put(name, value);
            return this;
        }

        public <T extends Scalar<?>> Builder add(String name, IdProvider<T> value) {
            args.put(name, value);
            return this;
        }

        public <T> Builder add(String name, List<T> value) {
            args.put(name, value);
            return this;
        }

        public Builder add(String name, InputValue value) {
            args.put(name, value);
            return this;
        }

        public Builder add(String name, Enum value) {
            args.put(name, value);
            return this;
        }

        public Arguments build() {
            return Arguments.this;
        }
    }
}
