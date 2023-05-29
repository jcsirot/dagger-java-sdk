package org.chelonix.dagger.model;

import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

import java.util.concurrent.ExecutionException;

import static org.chelonix.dagger.model.ArgType.arg;

public class Client {

    private QueryContext context;

    public Client(DynamicGraphQLClient graphQLClient) {
        this.context = new QueryContext(graphQLClient);
    }

    public Container container() {
        return new Container(context.chain(new QueryPart("container")));
    }
    public Container container(ContainerID id) {
        return new Container(context.chain(new QueryPart("container", "containerID", arg(id))));
    }
    public Container container(String platform) {
        return new Container(context.chain(new QueryPart("container", "platform", arg(platform))));
    }
//    public Container container(String id, String platform) {
//        return new Container(context.chain(new QueryPart("container", "containerID", id, "platform", platform)));
//    }

    public GitRepository git(String url) {
        QueryContext ctx = context.chain(new QueryPart("git", "url", arg(url)));
        return new GitRepository(ctx);
        // return ...
    }

    public String defaultPlatform() throws ExecutionException, InterruptedException {
        return context.chain(new QueryPart("defaultPlatform")).executeQuery(String.class);
    }
}
