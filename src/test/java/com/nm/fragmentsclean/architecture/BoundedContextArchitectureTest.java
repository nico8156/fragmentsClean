package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class BoundedContextArchitectureTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java/com/nm/fragmentsclean");
    private static final Set<String> CONTEXTS = Set.of(
            "adminImportContext",
            "aticleContext",
            "authenticationContext",
            "coffeeContext",
            "socialContext",
            "ticketContext",
            "userApplicationContext"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import com\\.nm\\.fragmentsclean\\.([A-Za-z0-9_]+)(\\.[^;]+);$");

    @Test
    void shared_kernel_does_not_import_bounded_contexts() throws IOException {
        List<String> violations = javaFiles(MAIN_JAVA.resolve("sharedKernel")).stream()
                .flatMap(file -> importsFrom(file).stream()
                        .filter(imported -> CONTEXTS.contains(imported.context()))
                        .map(imported -> violation(file, imported)))
                .sorted()
                .toList();

        assertThat(violations)
                .as("sharedKernel must stay technical and must not import bounded contexts")
                .isEmpty();
    }

    @Test
    void bounded_contexts_do_not_import_each_other_except_explicit_integration_edges() throws IOException {
        var violations = new java.util.ArrayList<String>();
        for (String context : CONTEXTS) {
            for (Path file : javaFiles(MAIN_JAVA.resolve(context))) {
                importsFrom(file).stream()
                        .filter(imported -> CONTEXTS.contains(imported.context()))
                        .filter(imported -> !context.equals(imported.context()))
                        .filter(imported -> !isAllowedIntegrationEdge(context, file, imported))
                        .map(imported -> violation(file, imported))
                        .forEach(violations::add);
            }
        }
        violations.sort(String::compareTo);

        assertThat(violations)
                .as("bounded contexts must not import each other except documented integration edges")
                .isEmpty();
    }

    private static boolean isAllowedIntegrationEdge(String sourceContext, Path sourceFile, ImportedType imported) {
        String sourcePath = normalize(sourceFile);
        String importPath = imported.context() + "." + imported.importPath();

        if ("adminImportContext".equals(sourceContext)) {
            return importPath.equals("coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand")
                    || importPath.equals("coffeeContext.write.businessLogic.gateways.CoffeeGooglePlaceLookupPort");
        }

        if ("socialContext".equals(sourceContext) && isTransportConsumer(sourcePath)) {
            return importPath.equals("userApplicationContext.write.businesslogic.models.AppUserCreatedEvent")
                    || importPath.equals("userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent");
        }

        if ("userApplicationContext".equals(sourceContext) && isTransportConsumer(sourcePath)) {
            return importPath.equals("authenticationContext.write.businesslogic.models.AuthUserCreatedEvent");
        }

        if ("userApplicationContext".equals(sourceContext)) {
            return sourcePath.endsWith("/userApplicationContext/write/businesslogic/usecases/AuthUserCreatedEventHandler.java")
                    && importPath.equals("authenticationContext.write.businesslogic.models.AuthUserCreatedEvent");
        }

        return false;
    }

    private static boolean isTransportConsumer(String sourcePath) {
        return sourcePath.contains("/adapters/primary/springboot/sqs/")
                || sourcePath.contains("/adapters/primary/springboot/kafka/");
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static List<ImportedType> importsFrom(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .map(String::trim)
                    .map(IMPORT_PATTERN::matcher)
                    .filter(java.util.regex.Matcher::matches)
                    .map(matcher -> new ImportedType(matcher.group(1), matcher.group(2).substring(1)))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + file, exception);
        }
    }

    private static String violation(Path file, ImportedType imported) {
        return normalize(file) + " imports " + imported.context() + "." + imported.importPath();
    }

    private static String normalize(Path file) {
        return file.toString().replace('\\', '/');
    }

    private record ImportedType(String context, String importPath) {
    }
}
