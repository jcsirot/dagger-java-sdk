package org.chelonix.dagger.model;

import java.util.List;

interface ArgType {
    Object serialize() throws RuntimeException;

    static <T> List<ArgType> argList(List<T> args) {
        return args.stream().map(o -> {
            return switch (o) {
                case String s -> arg(s);
                case Boolean b -> arg(b);
                case Integer i -> arg(i);
                case ArgType argumentType -> {
                    yield argumentType;
                }
                default -> throw new RuntimeException("Cannot convert type to ArgumentType");
            };
        }).toList();
    }

    static ArgType arg(List<String> args) {
        return new ArgType() {
            @Override
            public Object serialize() throws RuntimeException {
                return args;
            }
        };
    }

    static ArgType arg(String str) {
        return new ArgType() {
            @Override
            public String serialize() throws RuntimeException {
                return str;
            }
        };
    }

    static ArgType arg(Scalar<String> str) {
        return new ArgType() {
            @Override
            public String serialize() throws RuntimeException {
                return str.convert();
            }
        };
    }

    static ArgType arg(boolean bool) {
        return new ArgType() {
            @Override
            public String serialize() throws RuntimeException {
                return Boolean.toString(bool);
            }
        };
    }

    static ArgType arg(int i) {
        return new ArgType() {
            @Override
            public String serialize() throws RuntimeException {
                return Integer.toString(i);
            }
        };
    }
}
