package org.chelonix.dagger.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Container implements IdProvider<ContainerID> {

    private QueryContext queryCtx;

    Container(QueryContext queryCtx) {
        this.queryCtx = queryCtx;
    }

    public Container from(String address) {
        QueryContext ctx = queryCtx.chain("from",
                Arguments.newBuilder().add("address", address).build());
        return new Container(ctx);
    }

    public Container withExec(List<String> args) {
        QueryContext ctx = queryCtx.chain("withExec",
                Arguments.newBuilder().add("args", args).build());
        return new Container(ctx);
    }

    public Container withExec(String ...args) {
        QueryContext ctx = queryCtx.chain("withExec",
                Arguments.newBuilder().add("args", Arrays.asList(args)).build());
        return new Container(ctx);
    }

    public Container build(Directory context) {
        QueryContext ctx = this.queryCtx.chain("build",
                Arguments.newBuilder().add("context", context).build());
        return new Container(ctx);
    }

    public String stdout() throws ExecutionException, InterruptedException {
        QueryContext ctx = queryCtx.chain("stdout");
        return ctx.executeQuery(String.class);
    }

    public Container sync() throws ExecutionException, InterruptedException {
        QueryContext ctx = queryCtx.chain("sync");
        ctx.executeQuery();
        return this;
    }

    public ContainerID id() throws ExecutionException, InterruptedException {
        QueryContext ctx = queryCtx.chain("id");
        return ctx.executeQuery(ContainerID.class);
    }

    public List<EnvVariable> envVariables() throws Exception {
        QueryContext ctx = queryCtx.chain("envVariables");
        ctx = ctx.chain(List.of("name", "value"));
        return ctx.executeListQuery(EnvVariable.class);
    }
}
