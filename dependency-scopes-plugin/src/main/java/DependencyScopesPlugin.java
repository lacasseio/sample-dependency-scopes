import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Provider;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.CppLibrary;
import org.gradle.language.cpp.CppSharedLibrary;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;

import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;

public class DependencyScopesPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {
        settings.getGradle().allprojects(this::apply);
    }

    private void apply(Project project) {
        // Patch C++ library's shared binary to prevent exposing transitive link libraries.
        //   A shared library act as a link boundary, meaning consumers should not be aware of what the library is exposing
        project.getComponents().withType(CppLibrary.class).configureEach(component -> {
            component.getBinaries().configureEach(CppSharedLibrary.class, new Action<>() {
                @Override
                public void execute(CppSharedLibrary binary) {
                    final NamedDomainObjectProvider<Configuration> linkElements = project.getConfigurations().named(linkElementsConfigurationName(binary));
                    linkElements.configure(it -> it.setExtendsFrom(Collections.singletonList(component.getApiDependencies())));
                }

                private String linkElementsConfigurationName(CppSharedLibrary binary) {
                    return Optional.of(qualifyingName(binary)).filter(it -> !it.isEmpty())
                            .map(it -> it + "LinkElements").orElse("linkElements");
                }
            });
        });

        project.getComponents().withType(CppComponent.class)
                .configureEach(new CreateBucket<>(project, new BucketName("linkOnly", "link only"))
                        .andThen(nativeLinkExtendsFromBucket(project)));
        project.getComponents().withType(CppLibrary.class)
                .configureEach(new CreateBucket<>(project, new BucketName("linkOnlyApi", "link only API"))
                        .andThen(nativeLinkExtendsFromBucket(project).andThen(linkElementsExtendsFromBucket(project))));

        project.getComponents().withType(CppComponent.class)
                .configureEach(new CreateBucket<>(project, new BucketName("compileOnly", "compile only"))
                        .andThen(cppCompileExtendsFromBucket(project)));
        project.getComponents().withType(CppLibrary.class)
                .configureEach(new CreateBucket<CppLibrary>(project, new BucketName("compileOnlyApi", "compile only API"))
                        .andThen(DependencyScopesPlugin.<CppLibrary>cppCompileExtendsFromBucket(project).andThen(apiElementsExtendsFromBucket(project))));
    }

    private static <T extends CppComponent> BiConsumer<T, Provider<Configuration>> cppCompileExtendsFromBucket(Project project) {
        return (component, compileOnlyBucket) -> {
            component.getBinaries().configureEach(CppBinary.class, binary -> {
                final NamedDomainObjectProvider<Configuration> cppCompile = project.getConfigurations().named("cppCompile" + capitalize(qualifyingName(binary)));
                cppCompile.configure(it -> it.extendsFrom(compileOnlyBucket.get()));
            });
        };
    }

    private static BiConsumer<CppLibrary, Provider<Configuration>> apiElementsExtendsFromBucket(Project project) {
        return new BiConsumer<>() {
            @Override
            public void accept(CppLibrary component, Provider<Configuration> compileOnlyApi) {
                final NamedDomainObjectProvider<Configuration> apiElements = project.getConfigurations().named(apiElementsConfigurationName(component));
                apiElements.configure(it -> it.extendsFrom(compileOnlyApi.get()));
            }

            private String apiElementsConfigurationName(CppLibrary component) {
                return Optional.of(component.getName()).filter(it -> !"main".equals(it)).map(it -> it + "CppApiElements").orElse("cppApiElements");
            }
        };
    }

    private static <T extends CppComponent> BiConsumer<T, Provider<Configuration>> nativeLinkExtendsFromBucket(Project project) {
        return (component, linkOnlyBucket) -> {
            component.getBinaries().configureEach(CppBinary.class, binary -> {
                final NamedDomainObjectProvider<Configuration> nativeLink = project.getConfigurations().named("nativeLink" + capitalize(qualifyingName(binary)));
                nativeLink.configure(it -> it.extendsFrom(linkOnlyBucket.get()));
            });
        };
    }

    private static <T extends CppComponent> BiConsumer<T, Provider<Configuration>> linkElementsExtendsFromBucket(Project project) {
        return (component, linkOnlyApi) -> {
            component.getBinaries().configureEach(CppSharedLibrary.class, new Action<>() {
                @Override
                public void execute(CppSharedLibrary binary) {
                    final NamedDomainObjectProvider<Configuration> linkElements = project.getConfigurations().named(linkElementsConfigurationName(binary));
                    linkElements.configure(it -> it.extendsFrom(linkOnlyApi.get()));
                }

                private String linkElementsConfigurationName(CppSharedLibrary binary) {
                    return Optional.of(qualifyingName(binary)).filter(it -> !it.isEmpty())
                            .map(it -> it + "LinkElements").orElse("linkElements");
                }
            });
        };
    }

    private static final class CreateBucket<T extends CppComponent> implements Action<T> {
        private final Project project;
        private final BucketName bucketName;

        private CreateBucket(Project project, BucketName bucketName) {
            this.project = project;
            this.bucketName = bucketName;
        }

        @Override
        public void execute(T component) {
            final NamedDomainObjectProvider<Configuration> compileOnlyApi = project.getConfigurations().register(bucketName.asConfigurationName(component));
            compileOnlyApi.configure(it -> {
                it.setCanBeConsumed(false);
                it.setCanBeResolved(false);
                it.setDescription(String.format("%s of %s", bucketName.asDescription(), component));
            });
        }

        public Action<T> andThen(BiConsumer<T, Provider<Configuration>> nextAction) {
            return component -> {
                execute(component);
                nextAction.accept(component, project.getConfigurations().named(bucketName.asConfigurationName(component)));
            };
        }
    }

    private static final class BucketName {
        private final String name;
        private final String displayName;

        private BucketName(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        public String asDescription() {
            return capitalize(displayName) + " dependencies";
        }

        public String asConfigurationName(CppComponent component) {
            // Use 'implementation' configuration to infer new configuration name
            return component.getImplementationDependencies().getName()
                    .replace("implementation", name)
                    .replace("Implementation", capitalize(name));
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    //region Names
    private static String qualifyingName(CppBinary binary) {
        // The binary name follow the pattern <componentName><variantName>[Executable]
        String result = binary.getName();
        if (result.startsWith("main")) {
            result = result.substring("main".length());
        }

        // CppTestExecutable
        if (binary instanceof CppTestExecutable) {
            result = result.substring(0, binary.getName().length() - "Executable".length());
        }

        return uncapitalize(result);
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String uncapitalize(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
    //endregion
}
