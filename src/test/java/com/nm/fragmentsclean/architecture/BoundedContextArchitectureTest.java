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
            "articleContext",
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
    void platform_eventing_metadata_resolution_does_not_import_bounded_contexts() throws IOException {
        List<String> violations = javaFiles(MAIN_JAVA.resolve("platform/eventing")).stream()
                .filter(file -> file.getFileName().toString().contains("OutboxEventMetadata"))
                .flatMap(file -> importsFrom(file).stream()
                        .filter(imported -> CONTEXTS.contains(imported.context()))
                        .map(imported -> violation(file, imported)))
                .sorted()
                .toList();

        assertThat(violations)
                .as("platform/eventing orchestrates contracts and contributors; event metadata must be declared by producer contexts")
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

    @Test
    void write_side_does_not_publish_projection_sync_events() throws IOException {
        List<String> violations = CONTEXTS.stream()
                .map(context -> MAIN_JAVA.resolve(context).resolve("write"))
                .filter(Files::exists)
                .flatMap(path -> javaFilesUnchecked(path).stream())
                .flatMap(file -> importsFrom(file).stream()
                        .filter(imported -> imported.context().equals("sharedKernel"))
                        .filter(imported -> imported.importPath().startsWith("businesslogic.projectionSync."))
                        .map(imported -> violation(file, imported)))
                .sorted()
                .toList();

        assertThat(violations)
                .as("ProjectionSyncEvent belongs to read/projection boundaries, not write-side decisions")
                .isEmpty();
    }

    @Test
    void main_code_does_not_use_system_out_logging() throws IOException {
        List<String> violations = javaFiles(MAIN_JAVA).stream()
                .filter(file -> fileContains(file, "System.out."))
                .map(BoundedContextArchitectureTest::normalize)
                .sorted()
                .toList();

        assertThat(violations)
                .as("main code must use structured logging and must not print payloads through System.out")
                .isEmpty();
    }

    @Test
    void admin_primary_adapters_do_not_import_article_context() throws IOException {
        List<String> violations = javaFiles(MAIN_JAVA.resolve("adminImportContext/adapters/primary")).stream()
                .flatMap(file -> importsFrom(file).stream()
                        .filter(imported -> imported.context().equals("articleContext"))
                        .map(imported -> violation(file, imported)))
                .sorted().toList();

        assertThat(violations)
                .as("Studio controllers must call adminImportContext use cases and ports")
                .isEmpty();
    }

    @Test
    void article_domain_has_no_lombok_data_or_public_setters() throws IOException {
        List<String> violations = javaFiles(MAIN_JAVA.resolve("articleContext/write/businesslogic/models")).stream()
                .filter(file -> fileContains(file, "@Data") || fileMatches(file, "public\\s+void\\s+set[A-Z]"))
                .map(BoundedContextArchitectureTest::normalize).sorted().toList();

        assertThat(violations)
                .as("article domain objects expose behavior, not generic mutation")
                .isEmpty();
    }

    @Test
    void sqs_consumers_never_deserialize_domain_event_classes() throws IOException {
        List<String> violations = javaFiles(MAIN_JAVA).stream()
                .filter(file -> fileContains(file, "payloadReader.read"))
                .flatMap(file -> importsFrom(file).stream()
                        .filter(imported -> imported.importPath().contains(".write."))
                        .filter(imported -> imported.importPath().contains(".models."))
                        .filter(imported -> {
                            String simpleName = imported.importPath()
                                    .substring(imported.importPath().lastIndexOf('.') + 1);
                            return fileMatches(file, "payloadReader\\.read\\s*\\([^;]*\\b"
                                    + Pattern.quote(simpleName) + "\\.class");
                        })
                        .map(imported -> violation(file, imported)))
                .sorted()
                .toList();

        assertThat(violations)
                .as("SQS primary adapters must deserialize platform integration contracts, then cross a BC-local ACL")
                .isEmpty();
    }

    private static boolean isAllowedIntegrationEdge(String sourceContext, Path sourceFile, ImportedType imported) {
        String sourcePath = normalize(sourceFile);
        String importPath = imported.context() + "." + imported.importPath();

        if ("adminImportContext".equals(sourceContext)) {
            if (sourcePath.contains("adminImportContext/adapters/secondary/gateways/article/")) {
                return "articleContext".equals(imported.context());
            }
            return importPath.equals("coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand")
                    || importPath.equals("coffeeContext.write.businessLogic.gateways.CoffeeGooglePlaceLookupPort")
                    || (sourcePath.endsWith("adminImportContext/adapters/secondary/gateways/article/CommandBusArticleAuthoringPort.java")
                            && importPath.equals("articleContext.write.businesslogic.usecases.article.CreateArticleCommand"));
        }

        return false;
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

    private static List<Path> javaFilesUnchecked(Path root) {
        try {
            return javaFiles(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan " + root, exception);
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

    private static boolean fileContains(Path file, String needle) {
        try {
            return Files.readString(file).contains(needle);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + file, exception);
        }
    }

    private static boolean fileMatches(Path file, String regex) {
        try {
            return Pattern.compile(regex).matcher(Files.readString(file)).find();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + file, exception);
        }
    }

    private record ImportedType(String context, String importPath) {
    }
}
