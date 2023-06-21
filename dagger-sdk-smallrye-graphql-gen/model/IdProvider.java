package org.chelonix.dagger.model;

import java.util.concurrent.ExecutionException;

public interface IdProvider<S> {

    S id() throws ExecutionException, InterruptedException;
}
