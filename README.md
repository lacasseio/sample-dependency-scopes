# Dependency Scope

## App use :lib1 as compileOnly

In this Gradle project, the application serves as the primary executable component, relying on a static library named `lib1` for specific functionalities.
The relationship between the application and `lib1` is configured as a compile-only dependency.
This means that during the build process of the application, only the public headers of `lib1` are accessible, allowing the application to leverage the interfaces provided by the library while keeping its implementation details abstracted and separate.
This separation ensures that the application maintains a level of independence from the internal workings of `lib1`, facilitating a modular architecture.

```
$ ./gradlew :app-use-lib1-as-compile-only:dependencies
> Task :app-use-lib1-as-compile-only:dependencies

------------------------------------------------------------
Project ':app-use-lib1-as-compile-only'
------------------------------------------------------------

compileOnly - Compile only dependencies (n)
\--- project lib1 (n)

cppCompile<Variant>
\--- project :lib1

<variant>RuntimeElements (n)
No dependencies

implementation (n)
No dependencies

linkOnly - Link only dependencies (n)
No dependencies

main<Variant>Implementation (n)
No dependencies

nativeLink<Variant>
No dependencies

nativeRuntime<Variant>
No dependencies

(n) - Not resolved (configuration is not meant to be resolved)

A web-based, searchable dependency report is available by adding the --scan option.

BUILD SUCCESSFUL
```

## App use :lib1 as linkOnly

In this Gradle project, an application relies on a static library named `lib1` through a link-only dependency.
This configuration ensures that `lib1` is only available to the application during the linking phase of the build process.
Unlike typical dependencies, the public headers of `lib1` are not accessible, meaning the application cannot include or directly invoke the functionalities of `lib1` at compile time.
This approach typically implies that the application uses an intermediary or an adapter pattern to interact with the library functionalities at runtime, promoting a highly modular and loosely coupled architecture.

```
$ ./gradlew :app-use-lib1-as-link-only:dependencies
> Task :app-use-lib1-as-link-only:dependencies

------------------------------------------------------------
Project ':app-use-lib1-as-link-only'
------------------------------------------------------------

compileOnly - Compile only dependencies (n)
No dependencies

cppCompile<Variant>
No dependencies

<variant>RuntimeElements (n)
No dependencies

implementation (n)
No dependencies

linkOnly - Link only dependencies (n)
\--- project lib1 (n)

main<Variant>Implementation (n)
No dependencies

nativeLink<Variant>
\--- project :lib1

nativeRuntime<Variant>
No dependencies

(n) - Not resolved (configuration is not meant to be resolved)

A web-based, searchable dependency report is available by adding the --scan option.

BUILD SUCCESSFUL
```

## App use :lib2 as implementation

In this Gradle project, an application depends on a shared library named `lib2`.
The dependency on `lib2` is configured to be non-transitive for both compile and link phases.
This is because `lib2` itself only includes an implementation dependency on another library, `lib1`, which is a static library.
Since `lib1` is already linked into `lib2`, there is no need to expose `lib1` directly to the application.
This setup ensures that `lib1` remains encapsulated within `lib2`, simplifying the application's dependency graph and shielding it from the complexities of `lib1`.
To make `lib1` transitively available to the application, `lib2` would need to declare `lib1` as an `api` dependency, but in this scenario, it remains hidden, promoting a cleaner and more maintainable build configuration.
The application, therefore, interacts with `lib2` during both compilation and linking, benefiting from the functionalities provided without direct exposure to the underlying static library `lib1`.

```
$ ./gradlew :app-use-lib2-as-implementation:dependencies
> Task :app-use-lib2-as-implementation:dependencies

------------------------------------------------------------
Project ':app-use-lib2-as-implementation'
------------------------------------------------------------

compileOnly - Compile only dependencies (n)
No dependencies

cppCompile<Variant>
\--- project :lib2

<variant>RuntimeElements (n)
No dependencies

implementation (n)
\--- project lib2 (n)

linkOnly - Link only dependencies (n)
No dependencies

main<Variant>Implementation (n)
No dependencies

nativeLink<Variant>
\--- project :lib2

nativeRuntime<Variant>
\--- project :lib2
     \--- project :lib1

(n) - Not resolved (configuration is not meant to be resolved)

A web-based, searchable dependency report is available by adding the --scan option.

BUILD SUCCESSFUL
```

## App use :lib3 as implementation

In this Gradle project, an application relies on `lib3`, a shared library linked through an implementation dependency, which uniquely uses `compileOnlyApi` to expose `lib1` as an API dependency.
This setup allows the application to access the API of `lib1` directly while maintaining a modular architecture.
Conversely, the application's interaction with `lib2` is limited to the functionalities provided by `lib2` itself, as `lib2` includes `lib1` only as an implementation dependency, thus not exposing `lib1`'s functionalities transitively to the application.
This configuration effectively encapsulates `lib1` within `lib2`, ensuring that the application can depend on `lib3` and `lib2` for their respective capabilities without direct access to the underlying static library, `lib1`.

```
$ ./gradlew :app-use-lib3-as-implementation:dependencies
> Task :app-use-lib3-as-implementation:dependencies

------------------------------------------------------------
Project ':app-use-lib3-as-implementation'
------------------------------------------------------------

compileOnly - Compile only dependencies (n)
No dependencies

cppCompile<Variant>
\--- project :lib3
     +--- project :lib2
     \--- project :lib1

<variant>RuntimeElements (n)
No dependencies

implementation (n)
\--- project lib3 (n)

linkOnly - Link only dependencies (n)
No dependencies

main<Variant>Implementation (n)
No dependencies

nativeLink<Variant>
\--- project :lib3
     \--- project :lib2

nativeRuntime<Variant>
\--- project :lib3
     \--- project :lib2
          \--- project :lib1

(n) - Not resolved (configuration is not meant to be resolved)

A web-based, searchable dependency report is available by adding the --scan option.

BUILD SUCCESSFUL
```

## App use :lib4 as implementation

In this Gradle project, an application relies on `lib4`, a shared library, connected via an `implementation` dependency.
The project utilizes `lib2` through a `linkOnlyApi` dependency, which means `lib2` is included solely during the linking stage of the application.
This setup allows the application to benefit from the functionalities of `lib4` while leveraging `lib2` only at the linking phase, thereby optimizing performance and reducing runtime dependencies.

```
$ ./gradlew :
> Task :app-use-lib4-as-implementation:dependencies

------------------------------------------------------------
Project ':app-use-lib4-as-implementation'
------------------------------------------------------------

compileOnly - Compile only dependencies (n)
No dependencies

cppCompile<Variant>
\--- project :lib4

<variant>RuntimeElements (n)
No dependencies

implementation (n)
\--- project lib4 (n)

linkOnly - Link only dependencies (n)
No dependencies

main<Variant>Implementation (n)
No dependencies

nativeLink<Variant>
\--- project :lib4
     \--- project :lib2

nativeRuntime<Variant>
\--- project :lib4

(n) - Not resolved (configuration is not meant to be resolved)

A web-based, searchable dependency report is available by adding the --scan option.

BUILD SUCCESSFUL
```