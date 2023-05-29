package org.chelonix.dagger.model;

import static org.chelonix.dagger.model.ArgType.arg;

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
        QueryContext ctx = queryContext.chain(new QueryPart("tag", "name", arg(name)));
        return new GitRef(ctx);
        // return ...
    }

}
