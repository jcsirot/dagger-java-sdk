package org.chelonix.dagger.model;

public class GitRef {

    private QueryContext queryContext;

    GitRef(QueryContext queryContext) {
        this.queryContext = queryContext;
    }

    /**
     * <p>The filesystem tree at this ref.</p>
     *
     */
    public Directory tree() {
        QueryContext ctx = queryContext.chain("tree");
        return new Directory(ctx);
        // return ...
    }

}
