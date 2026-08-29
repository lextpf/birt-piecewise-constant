# Contributing Guide

This guide defines contribution standards for the project's human authors, co-authors, and AI agents acting as contributing entities.

There is no automatic formatter in this repository. Match the layout of the file you are editing instead of manually debating whitespace, wrapping, brace placement, or similar layout rules in review.

---

## Getting Started

1. Fork the repository on GitHub.
2. Clone your fork locally.
3. Run `.\setup.ps1`. It writes the git-ignored `.env` with the paths to your JDK 21, your Maven installation, and, optionally, an unpacked BIRT runtime.
4. Build the project with `.\build.ps1`.
5. Before submitting changes, run `.\test.ps1`.

---

## Language & Build

|            Item | Standard                                            |
|-----------------|-----------------------------------------------------|
|        Language | Java 21 (`maven.compiler.release` 21)               |
|    Build system | Apache Maven 3.9.x, one module, output in `build/`  |
|    Chart engine | Eclipse BIRT 4.24, `provided` scope                 |
| Model framework | EMF 2.43 (`org.eclipse.emf.ecore`)                  |
|         Testing | JUnit 5 (`junit-bom`)                               |
|         Scripts | PowerShell 7: `setup.ps1`, `build.ps1`, `test.ps1`  |
|          Layout | Root `src/` and `test/`; no `src/main/java` nesting |

The Maven build points `sourceDirectory` at `src/` and `testSourceDirectory` at `test/`. Do not "fix" this into the standard Maven layout.

---

## Core Principle

Prefer code that is:

- easy to read
- easy to review
- easy to debug
- easy to extend safely

Consistency matters more than personal preference.

---

## Formatting

There is no formatter configuration, no formatting check in the build, and no formatting step in `build.ps1`. The mechanical style is therefore carried by the existing sources. Match the file you are editing:

- tabs for indentation; spaces only for alignment inside a line
- Eclipse-style wrapping at about 120 columns
- one statement per line, and one declaration per statement
- the opening brace on the same line as the statement that opens it
- one blank line between members, never two

Do not reflow lines that your change does not touch. A diff that rewraps a whole file hides the real change inside it, and it makes the file harder to review, not prettier.

---

## Naming Conventions

| Element               | Style                              | Examples                                                 |
|-----------------------|------------------------------------|----------------------------------------------------------|
| Packages              | lowercase, no underscores          | `io.github.lextpf.birt.chart.piecewiseconstant.render`   |
| Classes / interfaces  | PascalCase                         | `PiecewiseConstantLine`, `PiecewiseConstantSeries`       |
| EMF implementations   | interface name + `Impl`            | `PiecewiseConstantSeriesImpl`                            |
| EMF package / factory | model name + `Package` / `Factory` | `PiecewiseConstantPackage`, `PiecewiseConstantFactory`   |
| EMF enum literals     | UPPER_SNAKE_CASE + `_LITERAL`      | `StepMode.CENTER_LITERAL`, `StepMode.AFTER_LITERAL`      |
| EMF feature constants | UPPER_SNAKE_CASE                   | `PIECEWISE_CONSTANT_SERIES__STEP_MODE`                   |
| Methods               | camelCase, verb first              | `setStepMode()`, `registerStandalone()`                  |
| Fields and locals     | camelCase, no prefix               | `stepMode`, `expandedLocations`                          |
| Constants             | `static final`, UPPER_SNAKE_CASE   | `static final String NAMESPACE_URI`                      |
| Unit tests            | class under test + `Test`          | `PiecewiseConstantExpanderTest`                          |
| Integration tests     | subject + `IT`                     | `RuntimeSmokeIT`                                         |

### Names that EMF owns

The `_LITERAL` suffix and the `*Impl` / `*Package` / `*Factory` triple are not this project's taste. They come from EMF, and the chart serializer resolves the model through exactly those names. Do not rename them to something prettier, and do not "modernize" a literal to a plain enum constant.

The same applies to the feature constants. `PIECEWISE_CONSTANT_SERIES__STEP_MODE` reads like shouting, but the double underscore is the EMF convention for *classifier* + *feature*, and the generated metadata is indexed by it.

### Prefer named data over positional data

Prefer a small record with named fields over an array of parallel results, a two-element array, or any other multi-value convention that relies on positional meaning.

Use a positional form only when the meaning is already obvious and local.

---

## Source File Organization

### Layout

Production code lives under `src/`, test code under `test/`, and the folder path mirrors the package name exactly. The test tree mirrors the source tree, plus one `test/` sub-package that holds the shared fixtures.

### License header

Every `.java` file starts with the EPL-2.0 header block, unchanged, before the `package` statement:

```java
/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
```

A new file without this header does not pass review. Do not add a year range, an author line, or a file name to it.

### Import order

Group imports in this order, separated by a blank line between groups:

1. `static` imports (in tests, the JUnit assertions)
2. `java.*` and `javax.*`
3. `org.eclipse.*` and other third-party packages
4. `io.github.lextpf.*`

Keep imports explicit, minimal, and local to actual usage. Do not use wildcard imports, not even for the JUnit assertions. Do not add an import for a type that you only name in prose; import it when you link it with `{@link}`, and write `{@code}` otherwise.

### Exported packages

`META-INF/MANIFEST.MF` lists the bundle's exported packages. When you add a package under `src/`, add it to `Export-Package`, and check that `build.properties` still covers the bundle contents. A package that is missing from `Export-Package` compiles, ships inside the jar, and is then invisible to the OSGi runtime.

---

## Scoping & Lifetime

### Local variables

* Declare variables in the narrowest practical scope.
* Initialize variables when declared.
* Do not separate declaration from first meaningful value unless there is a clear reason.
* Prefer loop-local variables inside the loop statement.

### Visibility

* Give every type and member the narrowest visibility that works.
* A helper that one class uses is `private`. A helper that the package uses is package-private. `public` is for the API that the chart engine or a report author calls.
* Do not widen visibility for a test. Put the test in the same package instead; the test tree mirrors the source tree for this reason.

### Final

Mark a field `final` when it is assigned once. Mark a local or a parameter `final` only when it helps the reader, for example when a long method reuses a name. Do not decorate every declaration with `final` as a matter of habit.

### Static and mutable state

Avoid mutable static state.

The two exceptions are documented one-shot registration guards, and they exist because the frameworks below the plug-in are themselves global:

* `PiecewiseConstantSetup.registered` - the standalone chart engine resolves renderers from arrays that are built once per JVM.
* `PiecewiseConstantPackageImpl.isInited` - the EMF package registry is a JVM-wide singleton.

Both guards make their operation idempotent. Do not add a third one without the same kind of reason, and do not turn either into a general-purpose cache.

---

## Control Flow

### Always use braces

Use braces for all control-flow bodies, even single statements.

This is a project rule even if formatting could make a one-liner look acceptable.

### Prefer early exits

Reduce nesting when possible:

* return early on invalid state
* continue early in loops
* keep the main path visually obvious

### Switch statements

* Prefer an enum over integer or string constants.
* Handle all constants explicitly when practical.
* Use `default` only when it is actually desired behavior, not as a way to suppress missing-case thinking.

A switch over `StepMode` is the one place where the EMF literals appear in bulk. Cover `AFTER_LITERAL`, `BEFORE_LITERAL`, and `CENTER_LITERAL` explicitly, so that a fourth mode fails visibly instead of falling into the behavior of `After`.

---

## Classes & Types

### Records and value types

Use a `record` for a passive data holder: a group of values that travel together, with no invariants beyond the values themselves.

```java
private record Expanded(DataPointHints[] dpha, Location[] loa) {
}
```

Use a `class` for types with invariants, encapsulation, lifecycle, or behavior.

### Utility classes

A class that only holds static helpers is `final` and has a private constructor. Do not let it be instantiated or subclassed by accident.

### The EMF triple

The interface, the `Impl`, the `Package`, and the `Factory` keep the shape that EMF expects, including the parts that look like boilerplate: the `eStaticClass()` override, the feature identifiers, and the `eGet` / `eSet` / `eUnset` / `eIsSet` dispatch. Extend them by following the existing pattern. Do not refactor them into something smaller.

### Constructors

* Avoid doing heavy work in constructors when failure is possible.
* Avoid calling overridable methods from a constructor.
* If initialization can fail meaningfully, prefer a factory method or a separate initialization step. The model classes already do this: `create()` and `createDefault()` are the supported entry points.

### Immutability

Prefer types that cannot change after construction. An immutable value is safe to pass to the engine, safe to hand to another thread, and impossible to corrupt from a hook that runs later.

Where a type must expose an array - and the renderer hooks deal in arrays, because BIRT's own contracts do - be explicit about whether the array is shared or copied, and never mutate an array that the caller still owns.

---

## Functions

### Prefer clear interfaces

* Prefer return values over output parameters.
* Keep parameter lists short and meaningful.
* Put inputs before outputs.
* Prefer strong, descriptive types over ambiguous booleans or loosely related parameter packs.

### Boolean parameters

Avoid multiple boolean parameters in one method signature.

This is hard to read:

```java
createWidget(true, false, true);
```

Prefer an options record, an enum, or separate methods when intent is not obvious.

### Function size

Keep functions focused.

A function that needs multiple screens, many nested branches, or several unrelated responsibilities should usually be split.

---

## Resources

* Open streams, readers, and writers in a try-with-resources statement. Do not close them by hand, and do not leave a resource to the garbage collector.
* Do not write finalizers, and do not add cleaners. If a type owns something that must be released, release it at a point the code makes obvious.
* Never call `Platform.shutdown()` from a test. Surefire runs with `reuseForks` off, so each test class gets its own JVM and the started platform dies with it. A test that shuts the platform down instead breaks the classes that share its fork.

---

## Error Handling

### Choose one clear strategy per API

Use the most suitable mechanism for the layer:

* an unchecked exception for programmer errors and impossible states, for example `IllegalArgumentException` from a factory that is handed a foreign classifier
* a `ChartException` where the chart engine's own contract declares one; the engine reports it to the report run
* a return value, or an empty result, for normal recoverable failure

Do not mix multiple error-handling strategies in the same small API without a good reason.

### Assumptions

An exception that guards an assumption should mean: if this fails, the code is wrong.

`PiecewiseConstantPackageImpl` is the example. It checks the feature layout that it inherits from the chart engine and fails loudly when that layout has changed, because every later symptom would be a confusing one. State the assumption in the message, and name what the reader has to do about it.

Do not use an exception to report a condition that the report author can legitimately produce. A missing value in a data set is data, not a defect.

---

## Comments & Documentation

Documentation comments are written for **Javadoc**. Prose in this repository is written in **Simplified Technical English**: short declarative sentences, one idea per sentence, `if ... then ...` for conditions, no contractions, no marketing adjectives, and one term per concept. The terms of this project are *tread*, *step*, *run*, and *corner vertex*; use them, and do not introduce synonyms.

Authorship is not recorded in the sources. There is no `@author` tag anywhere in `src/`, and the git history is the record.

### General comment rule

Comment the reason, constraint, or non-obvious behavior. Do not comment what the code already says plainly. Preserve ASCII diagrams and worked-example traces in geometry-heavy code - they are house style, not clutter.

Bad:

```java
index++; // Increment index
```

Better:

```java
index++; // The corner vertex consumed one slot, so the two arrays stay aligned.
```

### Where documentation lives

* Javadoc on a type or a member: the contract that a caller, or the chart engine, depends on.
* `//` inside a method body: why the implementation does what it does.

A `//` comment inside a method never carries Javadoc markup. Write `` `stepMode` `` in backticks or plain, not `{@code stepMode}`.

---

### Javadoc

#### Block form

* A Javadoc comment is a `/** */` block: `/**` on its own line, a leading ` * ` on every continuation line, a bare ` *` for blank separator lines, and ` */` to close.
* Start with a one-line summary that ends in a period. The summary is what the reader sees in an index, so it must stand alone.
* Separate paragraphs with `<p>`, on its own line, in the position the existing files use.

#### Type-level block

The block above a type carries a summary and then only the paragraphs that carry a contract. The house labels are:

1. **Intent** - why the type exists, and what it does for the engine.
2. **Constraints** - what the engine requires of it: the extension point that builds it, the constructor it must keep, the order in which it must be called.
3. **Non-obvious behaviour** - anything a reader would otherwise get wrong.

```java
/**
 * Draws a {@link PiecewiseConstantSeries} as a piecewise constant line.
 * <p>
 * Intent: the stock renderer draws a straight segment between two consecutive
 * entries of its location array. This renderer inserts one corner vertex per
 * step into that array.
 * <p>
 * Constraints: the chart engine creates this renderer through the
 * {@code org.eclipse.birt.chart.engine.modelrenderers} extension point. The
 * class must therefore keep a public constructor without arguments.
 * <p>
 * Non-obvious behaviour: each corner vertex reuses the data point hints of the
 * point whose value it carries, so both arrays keep the same length and the
 * same index meaning.
 */
```

Use only the labels that apply. A type with nothing non-obvious about it gets a summary and an `Intent` paragraph, and that is a complete block.

#### Method documentation

Document a method when its contract is not obvious from its signature: an ordering requirement, an idempotence guarantee, a unit, a range, a null contract, or the index meaning of an array it returns.

Do **not** write:

* `@param stepMode the step mode` or `@return the step mode`. A tag that restates the name is noise. Add `@param`, `@return`, or `@throws` only when it carries a unit, a range, a null contract, an index meaning, or the condition under which the exception is thrown.
* `Side effects: none.` Do not write it. Document a side effect when it exists; a method without one needs no sentence about it.
* Any Javadoc at all on a trivial EMF accessor. `getStepMode()` and `setStepMode()` carry the model's own meaning; repeating the field name above them adds nothing.

#### Members and enum literals

Document a constant or an enum literal with a short `/** */` block above it when the name does not carry the meaning by itself. Java has no trailing documentation comment form, so do not invent one out of `//`.

Do not add `//` section banners between members. The member order and the names carry the structure; a banner that restates the next method name is clutter.

#### Tags

Tags to use where useful: `{@link}`, `{@code}`, `@param`, `@return`, `@throws`, and `@see`.

`{@link}` requires the target to be on the compile classpath and imported; use it for this project's own types and for the BIRT and EMF types the module already depends on. Use `{@code}` for everything else: a class the plug-in never imports, an extension point identifier, a system property, an XML element, a file name. A `{@link}` to a type that is not imported is a Javadoc warning, and the name then renders as plain text anyway.

---

### TODO comments

Use `TODO` only for real follow-up work, not vague reminders.

Make them specific and actionable:

```java
// TODO: Drop this fallback once eclipse-birt PR #2480 ships the series upstream.
```

---

## API Design Preferences

### Prefer expressive types

Use enums, records, and dedicated small types when they make interfaces clearer.

Prefer:

```java
record LoadOptions(boolean allowCache, boolean validateSchema) {
}
```

over:

```java
boolean load(boolean allowCache, boolean validateSchema);
```

### Prefer compile-time guarantees

When a rule can be enforced by the type system, by an enum, by a record, or by `final`, prefer that over comments and conventions.

### Avoid hidden work

Methods should not unexpectedly:

* allocate heavily
* block for long periods
* mutate unrelated global state
* register something globally as a side effect

Make expensive or stateful behavior visible in the API. Registration is the clear case: it happens in one named method that says so, not on the way to something else.

---

## Testing

All non-trivial behavior changes should include tests or a clear reason why tests are not practical.

Add or update tests when you change:

* the expander geometry
* the step-mode semantics
* the EMF model or its serialization
* the `plugin.xml` wiring
* the standalone registration path
* bug fixes with reproducible behavior

A bug fix without a regression test should be the exception, not the norm.

### How the suite is written

* Tests use **JUnit 5**. Run the whole suite with `.\test.ps1`, and one class with `.\test.ps1 -Test <name>`.
* Build charts through `ChartFixtures`, and capture the drawn geometry with `CapturingPngRenderer`. Do not hand-roll a chart inside a test method.
* A test that needs a started chart platform carries `@ExtendWith(ChartPlatformExtension.class)`.

### The linear oracle

Geometry tests do not carry expected coordinates as literals. They compare against a **linear oracle**: the fixture renders the same chart twice, once with a stock `LineSeries` and once with the piecewise constant series, and asserts that the piecewise constant vertices are the reference vertices with the corner vertices inserted.

Because both charts come from the same options and the same data, the device coordinates of the real data points are equal bit for bit. Exact `double` comparison is therefore correct there, and a tolerance would only hide a real defect. Keep new geometry tests on this pattern.

### `RuntimeSmokeIT`

`RuntimeSmokeIT` runs the plug-in end to end on a real BIRT distribution, and it needs an unpacked `birt-runtime-4.24.0`. Point it at one with `.\setup.ps1 -BirtRuntimeDir <path>` once, or per run with `.\test.ps1 -Test RuntimeSmokeIT -Runtime <path>`. Without that directory the test is skipped, which is by design and not a failure.

---

## Pull Requests

### Scope

Keep pull requests focused.

Do not mix unrelated refactors, formatting-only churn, feature work, and bug fixes in the same PR unless there is a strong reason.

### What to include

A good PR should explain:

* what changed
* why it changed
* any important tradeoffs
* how it was validated

Say which of `.\build.ps1` and `.\test.ps1` you ran, and whether `RuntimeSmokeIT` ran or was skipped.

### Where a change belongs

This plug-in is a stop-gap. The same feature is proposed upstream in [eclipse-birt PR #2480](https://github.com/eclipse-birt/birt/pull/2480), which fixes [issue #2478](https://github.com/eclipse-birt/birt/issues/2478), and this repository is deprecated once that pull request merges.

A change to the chart engine itself belongs in [eclipse-birt](https://github.com/eclipse-birt/birt), not here: a new hook in `Line`, a change to `PluginSettings`, a fix in the serializer.

This repository takes changes to the series, the expander, the renderer subclass, the build, and the documentation.

### Reviewer expectations

Reviewers should prioritize:

* correctness
* maintainability
* API clarity
* architecture fit
* test coverage

Layout is not a review topic. There is no formatter to re-litigate, so match the file you are editing and spend the review on the list above.

---

## AI-Assisted Contributions

AI assistance is allowed, but the contributor remains fully responsible for the submitted code.

If you use AI, you must still ensure that the result is:

* correct
* project-consistent
* buildable
* testable
* understandable by a human reviewer

Do not submit generated code you do not understand.

Pay extra attention to:

* hallucinated APIs
* incorrect ownership assumptions
* fake imports
* wrong engine/library types
* missing edge cases
* overly generic comments or documentation
