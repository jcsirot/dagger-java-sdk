package org.chelonix.dagger.sample;

import org.chelonix.dagger.client.*;

import java.util.List;

public class CreateAndUseSecret {
    public static void main(String... args) throws Exception {
        try(Client client = Dagger.connect()) {
            Secret secret = client.setSecret("ghApiToken", System.getenv("GH_API_TOKEN"));

            // use secret in container environment
            String out = client.container()
                    .from("alpine:3.17")
                    .withSecretVariable("GITHUB_API_TOKEN", secret)
                    .withExec(List.of("apk", "add", "curl"))
                    .withExec(List.of("sh", "-c", "curl \"https://api.github.com/repos/dagger/dagger/issues\" --header \"Accept: application/vnd.github+json\" --header \"Authorization: Bearer $GITHUB_API_TOKEN\""))
                    .stdout();

            // print result
            System.out.println(out);
        }
    }
}