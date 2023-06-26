package org.chelonix.dagger.client.engineconn;

import com.ongres.process.FluentProcess;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.annotation.JsonbProperty;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

public final class Connection {

    private final DynamicGraphQLClient graphQLClient;
    private final FluentProcess daggerProc;

    Connection(DynamicGraphQLClient graphQLClient, FluentProcess daggerProc) {
        this.graphQLClient = graphQLClient;
        this.daggerProc = daggerProc;
    }

    public DynamicGraphQLClient getGraphQLClient() {
        return this.graphQLClient;
    }

    public void close() throws Exception {
        this.graphQLClient.close();
        this.daggerProc.close();
    }

    public static class ConnectParams {
        private int port;

        @JsonbProperty("session_token")
        private String sessionToken;

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "ConnectParams{" +
                    "port=" + port +
                    ", sessionToken='" + sessionToken + '\'' +
                    '}';
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        public void setSessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
        }
    }

    private static String getCLIPath() throws IOException {
        String cliBinPath = System.getenv("_EXPERIMENTAL_DAGGER_CLI_BIN");
        if (cliBinPath == null) {
            cliBinPath = new CLIDownloader().downloadCLI();
        }
        return cliBinPath;
    }

    public static Connection get(String workingDir) throws IOException {
        String bin = getCLIPath();
        FluentProcess process = FluentProcess.start(bin, "session",
                "--workdir", workingDir,
                "--label", "dagger.io/sdk.name:java",
                "--label", "dagger.io/sdk.version:" + CLIDownloader.CLI_VERSION)
                .withAllowedExitCodes(137);
        Jsonb jsonb = JsonbBuilder.create();
        String output = process.streamStdout().findFirst().get();
        System.out.println(output);
        ConnectParams connectParams = jsonb.fromJson(output, ConnectParams.class);
        // System.out.println(connectParams);
        int port = connectParams.getPort();
        String token = connectParams.getSessionToken();

        String encodedToken = Base64.getEncoder().encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));
        DynamicGraphQLClient dynamicGraphQLClient = DynamicGraphQLClientBuilder.newBuilder()
                .url(String.format("http://127.0.0.1:%d/query", port))
                .header("authorization", "Basic " + encodedToken)
                .build();

        return new Connection(dynamicGraphQLClient, process);
    }
}
