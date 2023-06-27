# dagger-java-sdk

A [Dagger.io](https://dagger.io) SDK written in java for java.

> **Warning**
> This project is still a Work in Progress

## Build

### Requirements

- Java 17

Simply run maven to build the jars and install them in your local `${HOME}/.m2` repository

```bash
./mvnw clean install 
```

To generate the Javadoc (site and jar), use the `javadoc` profile.
The javadoc are built in `./dagger-java-sdk/target/apidocs/index.html`

```bash
./mvnw package -Pjavadoc
```

## Usage

in your project's `pom.xml` add the dependency

```xml
  <dependency>
    <groupId>org.chelonix.dagger</groupId>
    <artifactId>dagger-java-sdk</artifactId>
    <version>0.6.2-SNAPSHOT</version>
  </dependency>
```

An code sample using the Dagger client

```java
package org.chelonix.dagger.sample;

import org.chelonix.dagger.client.Client;
import org.chelonix.dagger.client.Dagger;

import java.util.List;

public class GetDaggerWebsite {
    public static void main(String... args) throws Exception {
        try(Client client = Dagger.connect()) {
            String output = client.pipeline("test")
                    .container()
                    .from("alpine")
                    .withExec(List.of("apk", "add", "curl"))
                    .withExec(List.of("curl", "https://dagger.io"))
                    .stdout();

            System.out.println(output.substring(0, 300));
        }
    }
}
```

Look at the `dagger-java-samples` module for more code samples.