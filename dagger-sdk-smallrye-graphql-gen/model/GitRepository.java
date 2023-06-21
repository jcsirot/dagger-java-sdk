package org.chelonix.dagger.model;

public class GitRepository {

    private QueryContext queryContext;

    GitRepository(QueryContext queryContext) {
        this.queryContext = queryContext;
    }

    /**
     * <p>Returns details on one tag.</p>
     *
     * @param name Tag's name (e.g., "v0.3.9").
     */
    public GitRef tag(String name) {
        QueryContext ctx = queryContext.chain("tag",
                Arguments.newBuilder().add("name", name).build());
        return new GitRef(ctx);
        // return ...
    }
}
