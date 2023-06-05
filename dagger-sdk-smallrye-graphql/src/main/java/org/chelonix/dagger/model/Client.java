package org.chelonix.dagger.model;

import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.chelonix.dagger.model.ArgValue.arg;

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

        Map<String, ArgValue> toArguments() {
            HashMap<String, ArgValue> map = new HashMap<>();
            map.put("id", arg(this.id));
            map.put("platform", arg(this.platform));
            return map;
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
        QueryContext ctx = queryContext.chain("git", "url", arg(url));
        return new GitRepository(ctx);
        // return ...
    }

    public String defaultPlatform() throws ExecutionException, InterruptedException {
        return queryContext.chain("defaultPlatform").executeQuery(String.class);
    }
}
