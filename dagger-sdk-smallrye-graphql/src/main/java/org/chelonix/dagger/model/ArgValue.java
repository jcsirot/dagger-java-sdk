package org.chelonix.dagger.model;

import java.util.List;
import java.util.Map;

interface ArgValue {
    Object serialize() throws RuntimeException;

    static ArgValue arg(Object o) {
        if (o instanceof ArgValue) {
            return (ArgValue)o;
        } else {
            return new ArgValue() {
                @Override
                public Object serialize() throws RuntimeException {
                    return o;
                }
            };
        }
    }
}
