package org.chelonix.dagger.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.chelonix.dagger.model.ArgValue.*;

public class Container implements ArgValue {

    private QueryContext queryCtx;

    Container(QueryContext queryCtx) {
        this.queryCtx = queryCtx;
    }

    public Container from(String address) {
        QueryContext ctx = queryCtx.chain("from", "address", arg(address));
        return new Container(ctx);
    }

    public Container withExec(List<String> args) {
        QueryContext ctx = queryCtx.chain("withExec", "args", arg(args));
        return new Container(ctx);
    }

    public Container withExec(String ...args) {
        QueryContext ctx = queryCtx.chain("withExec", "args", arg(Arrays.asList(args)));
        return new Container(ctx);
    }

    public Container build(Directory context) {
        QueryContext ctx = this.queryCtx.chain("build", "context", context);
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

    public ContainerID id() throws Exception {
        QueryContext ctx = queryCtx.chain("id");
        return ctx.executeQuery(ContainerID.class);
    }

    public List<EnvVariable> envVariables() throws Exception {
        QueryContext ctx = queryCtx.chain("envVariables");
        ctx = ctx.chain(List.of("name", "value"));
        return ctx.executeListQuery(EnvVariable.class);
    }

    @Override
    public Object serialize() throws RuntimeException {
        try {
            return this.id().convert();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
