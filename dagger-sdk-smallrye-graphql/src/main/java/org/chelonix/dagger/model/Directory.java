package org.chelonix.dagger.model;

public class Directory implements ArgValue {

    private QueryContext queryContext;

    Directory(QueryContext queryContext) {
        this.queryContext = queryContext;
    }

    public DirectoryID id() throws Exception {
        return queryContext.chain("id").executeQuery(DirectoryID.class);
    }

    @Override
    public String serialize() throws RuntimeException {
        try {
            return this.id().convert();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
