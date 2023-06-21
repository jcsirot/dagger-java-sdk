package org.chelonix.dagger.model;

import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Client {

    private QueryContext queryContext;

    public Client(DynamicGraphQLClient graphQLClient) {
        this.queryContext = new QueryContext(graphQLClient);
    }

    public static class ContainerArguments {
        private ContainerID id;
        private String platform;

        private ContainerArguments() {}

        public static ContainerArgumentsBuilder newBuilder() {
            ContainerArguments args = new ContainerArguments();
            return new ContainerArgumentsBuilder(args);
        }

        public ContainerID getId() {
            return id;
        }

        void setId(ContainerID id) {
            this.id = id;
        }

        public String getPlatform() {
            return platform;
        }

        void setPlatform(String platform) {
            this.platform = platform;
        }

        Arguments toArguments() {
            Arguments.Builder builder = Arguments.newBuilder();
            builder.add("id", this.id);
            builder.add("platform", this.platform);
            return builder.build();
        }
    }

    public static class ContainerArgumentsBuilder {
        private ContainerArguments args;

        ContainerArgumentsBuilder(ContainerArguments args) {
            this.args = args;
        }

        public ContainerArgumentsBuilder withId(ContainerID id) {
            args.setId(id);
            return this;
        }

        public ContainerArgumentsBuilder withPlatform(String platform) {
            args.setPlatform(platform);
            return this;
        }

        public ContainerArguments build() {
            return args;
        }
    }

    public Container container() {
        QueryContext ctx = queryContext.chain("container");
        return new Container(ctx);
    }

    public Container container(ContainerArguments containerArguments) {
        QueryContext ctx = queryContext.chain("container", containerArguments.toArguments());
        return new Container(ctx);
    }

    public GitRepository git(String url) {
        QueryContext ctx = queryContext.chain("git",
                Arguments.newBuilder().add("url", url).build());
        return new GitRepository(ctx);
        // return ...
    }

    public String defaultPlatform() throws ExecutionException, InterruptedException {
        return queryContext.chain("defaultPlatform").executeQuery(String.class);
    }
}
