package org.chelonix.dagger;

import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import org.chelonix.dagger.client.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv("DAGGER_SESSION_PORT"));
        // int port = 0;
        String token = System.getenv("DAGGER_SESSION_TOKEN");
        System.out.println(token);
        System.out.println(System.getProperty("os.name"));
        System.out.println(System.getProperty("os.arch"));

        String encodedToken = Base64.getEncoder().encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));
        DynamicGraphQLClient dynamicGraphQLClient = DynamicGraphQLClientBuilder.newBuilder().url(String.format("http://127.0.0.1:%d/query", port))
                .header("authorization", "Basic " + encodedToken).build();

//        String query = """
//            query {
//              container {
//                from (address: "alpine:latest") {
//                  withExec(args:["uname", "-nrio"]) {
//                    stdout
//                  }
//                }
//              }
//            }""";

//        Document query = document(
//                operation(
//                        field("container",
//                                field("from", args(arg("address", "alpine:latest")),
//                                        field("envVariables",
//                                            field("name"), field("value")
//                                        )
//                                )
//                        )
//                )
//        );
//
//        System.out.println(query.build());
//        Response r = dynamicGraphQLClient.executeSync(query.build());
//        System.out.println(r);

        Client client = new Client(dynamicGraphQLClient);

//        String stdout = client.container().from("alpine:latest").withExec(List.of("uname", "-nrio")).stdout();
//        System.out.println(stdout);
//
//        String defaultPlatform = client.defaultPlatform();
//        System.out.println(defaultPlatform);


//        Container container = client.container()
//                .from("alpine")
//                .withExec("apk", "add", "curl")
//                .withExec("curl", "https://example.com");

        try {
            // String result = container.stdout();

            // System.out.println(result);

            // listEnvVariables(client);

            // gitVersionContainerID(client);

            // runCommandWithSync(client);

            // listHostDirectoryContents(client);

            // mountHostDirectoryInContainer(client);

            buildTimeVariables(client);
        } catch (DaggerQueryException dqe) {
            dqe.printStackTrace();
            Arrays.stream(dqe.getErrors()).forEach(System.out::println);
        }

        System.exit(0);
    }

    private static void listEnvVariables(Client client) throws Exception {
        List<EnvVariable> env = client.container().from("alpine").envVariables();
        env.stream().map(var -> {
            try {
                return String.format("%s=%s", var.name(), var.value());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (DaggerQueryException e) {
                throw new RuntimeException(e);
            }
        }).forEach(System.out::println);
    }

    private static void gitVersionContainerID(Client client) throws ExecutionException, InterruptedException, DaggerQueryException {
        Directory repo = client.git("https://github.com/dagger/dagger").tag("v0.3.0").tree();
        Container daggerImg = client.container().build(repo);
        String stdout = daggerImg.withExec(List.of("version")).stdout();
        System.out.println(stdout);
    }

    private static void runCommandWithSync(Client client) throws ExecutionException, InterruptedException, DaggerQueryException {
        String stdout = client.container()
                .from("alpine")
                .withExec(List.of("apk", "add", "curl"))
                .sync()
                .withExec(List.of("curl", "https://example.com"))
                .stdout();
        System.out.println(stdout);
    }

    private static void listHostDirectoryContents(Client client) throws ExecutionException, InterruptedException, DaggerQueryException {
        List<String> entries = client.host().directory(".").entries();
        System.out.println(entries);
    }

    private static void mountHostDirectoryInContainer(Client client) throws ExecutionException, InterruptedException, DaggerQueryException {
        String contents = client.container().from("alpine").
                withDirectory("/host", client.host().directory(".")).
                withExec(List.of("ls", "/host")).
                stdout();
        System.out.println(contents);
    }

    private static void buildTimeVariables(Client client) throws ExecutionException, InterruptedException, DaggerQueryException {
        List<String> oses = List.of("linux", "darwin");
        List<String> arches = List.of("amd64", "arm64");

        Directory src = client.host().directory(".");
        Directory outputs = client.directory();

        Container golang = client.container()
                // get golang image
                .from("golang:latest")
                // mount source code into golang image
                .withDirectory("/src", src)
                .withWorkdir("/src");

        for (String os: oses) {
            for (String arch: arches) {
                // create a directory for each OS and architecture
                String path = String.format("target/%s/%s/", os, arch);

               Container build = golang
                        // set GOARCH and GOOS in the build environment
                        .withEnvVariable("GOOS", os)
                        .withEnvVariable("GOARCH", arch)
                        .withExec(List.of("go", "build", "-o", path));

                // add build to outputs
                outputs = outputs.withDirectory(path, build.directory(path));
            }
        }

        // write build artifacts to host
        outputs.export(".");
    }
}
