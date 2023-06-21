package org.chelonix.dagger.model;

import java.util.concurrent.ExecutionException;

public class Directory implements IdProvider {

    private QueryContext queryContext;

    Directory(QueryContext queryContext) {
        this.queryContext = queryContext;
    }

    public DirectoryID id() throws ExecutionException, InterruptedException {
        return queryContext.chain("id").executeQuery(DirectoryID.class);
    }
}
