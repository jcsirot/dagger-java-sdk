package org.chelonix.dagger;

import com.ongres.process.FluentProcess;
import org.chelonix.dagger.codegen.introspection.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class Main {

    public static final String VERSION = "0.6.2";

    public static void main(String[] args) throws Exception {
        Path dest = Paths.get("target", "gen");
        // Main.class.getClassLoader().getResourceAsStream("introspection2.json");
        try (InputStream in = daggerSchema("dagger", VERSION)) {
            Schema schema = Schema.initialize(in);
            //System.out.println(schema);
            SchemaVisitor codegen = new CodegenVisitor(schema, dest, StandardCharsets.UTF_8);
            schema.visit(new SchemaVisitor() {
                @Override
                public void visitScalar(Type type) {
                    System.out.println(String.format("Generating scalar type %s", type.getName()));
                    codegen.visitScalar(type);
                }

                @Override
                public void visitObject(Type type) {
                    System.out.println(String.format("Generating object type %s", type.getName()));
                    codegen.visitObject(type);
                }

                @Override
                public void visitInput(Type type) {
                    System.out.println(String.format("Generating input %s", type.getName()));
                    codegen.visitInput(type);

                }

                @Override
                public void visitEnum(Type type) {
                    System.out.println(String.format("Generating enum %s", type.getName()));
                    codegen.visitEnum(type);
                }
            });        }
    }

    private static InputStream daggerSchema(String binPath, String version) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        URL url = new URL(String.format("https://raw.githubusercontent.com/dagger/dagger/v%s/codegen/introspection/introspection.graphql", version));
        FluentProcess.start(binPath, "query")
                .withTimeout(Duration.of(60, ChronoUnit.SECONDS))
                .inputStream(url.openStream())
                .writeToOutputStream(out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
