# BIRT Chart Piecewise Constant Series

[![build](https://github.com/lextpf/birt-piecewise-constant/actions/workflows/build.yml/badge.svg)](https://github.com/lextpf/birt-piecewise-constant/actions/workflows/build.yml)

`io.github.lextpf.birt.chart.piecewiseconstant` is a plug-in for the Eclipse BIRT chart engine. It
adds a **PiecewiseConstantSeries**. The renderer holds the value of each data point until the step
to the next data point. The line is therefore a run of horizontal treads and vertical steps, and
not a run of interpolated segments.

Other names for this series type are step line, staircase, stepped line, zero-order hold and
sample-and-hold. This document uses only the term "piecewise constant series". The renderer draws
each step `After`, `Before` or at the `Center` of the interval.

The plug-in connects to the chart engine through three standard extension points:
`modelrenderers`, `datasetprocessors` and `charttypes`. It targets BIRT 4.24.0 and contains no UI
code. `PiecewiseConstantSeries` is a subclass of BIRT's own `LineSeries`. Markers, data point
labels, shadows, stacking, percent axes, transposed (horizontal) charts, 3D and the legend all
continue to work. Only the path between the data points changes.

> **Scope:** the chart wizard of the Designer is out of scope. The plug-in adds no wizard page for
> this series type. An author therefore cannot create a piecewise constant chart through the
> Designer UI. An author must use the Java API, or must edit the chart XML in the `.rptdesign` file.
> See [Using it](#using-it).
>
> The test suite of this module verifies the POJO runtime path only. The Designer preview and the
> OSGi runtime resolve the series through the same extension points, so they can render it in the
> same way. This module does not verify that.

## Step modes

| Step mode | XML literal | Where the renderer draws the step | Shape between point *p* and point *q* | Mathematical name |
| --- | --- | --- | --- | --- |
| `StepMode.AFTER_LITERAL` | `After` | at the **next** data point | tread at the value of *p* up to the position of *q*, then step | right-continuous (càdlàg) step function |
| `StepMode.BEFORE_LITERAL` | `Before` | at the **current** data point | step to the value of *q* at the position of *p*, then tread | left-continuous step function |
| `StepMode.CENTER_LITERAL` | `Center` | **halfway** between the two data points | tread, step at the midpoint, tread | midpoint step function |

On a category axis the midpoint is the boundary between the two categories.

`After` is the default step mode. A series that has no `StepMode` value behaves as `After`, and the
chart serializer writes no `<StepMode>` element for it.

## Getting the jar

### (a) From the GitHub Releases page

Every `v*` tag publishes a release. Each release carries both jars.

1. Open [the latest release](https://github.com/lextpf/birt-piecewise-constant/releases/latest).
2. Download the plug-in jar:

   ```
   io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar
   ```

The matching `…_1.0.0-sources.jar` is attached next to it. The plug-in jar is the file that you put
into an engine. It carries `plugin.xml` and an OSGi `META-INF/MANIFEST.MF` at its root. These two
files make it a BIRT plug-in in both environments of BIRT.

### (b) From GitHub Packages, for Maven users

The same release publishes the artifact to GitHub Packages under these coordinates:

```xml
<dependency>
  <groupId>io.github.lextpf.birt</groupId>
  <artifactId>io.github.lextpf.birt.chart.piecewiseconstant</artifactId>
  <version>1.0.0</version>
</dependency>
```

GitHub Packages always needs credentials. GitHub rejects an anonymous fetch with
`401 Unauthorized`, also for a public package. You must therefore hold a personal access token
(classic) with the **`read:packages`** scope before you start.

1. Add the repository to the `pom.xml` of the consuming project, or to a profile in `settings.xml`:

   ```xml
   <repositories>
     <repository>
       <id>github</id>
       <name>GitHub Packages</name>
       <url>https://maven.pkg.github.com/lextpf/birt-piecewise-constant</url>
     </repository>
   </repositories>
   ```

2. Add a matching server entry to `~/.m2/settings.xml`:

   ```xml
   <servers>
     <server>
       <id>github</id>
       <username>your-github-username</username>
       <password>ghp_your_personal_access_token</password>
     </server>
   </servers>
   ```

The `<id>` of the server must match the `<id>` of the repository. If you do not want to use a
token, then download the release (a) or build the plug-in locally (c).

### (c) Build it locally

Precondition: the machine runs Windows with PowerShell 7, and it holds a JDK 21 and an Apache Maven
installation. See [Building from source](#building-from-source) for what the scripts do.

1. Write the toolchain configuration:

   ```powershell
   .\setup.ps1
   ```

2. Build the jars:

   ```powershell
   .\build.ps1
   ```

`.\build.ps1` builds the jars and runs no test. It produces
`build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar` and the sources jar. Run
`.\test.ps1` when you want the test suite.

3. Install the jars, if a sibling project must resolve them from the local repository:

   ```powershell
   .\build.ps1 install
   ```

That command puts both jars under `~/.m2/repository`. A sibling project can then use the
coordinates above with no repository entry at all.

## Integrating into a BIRT engine

Deployment is always the same. Put the jar where the class loader of the POJO runtime finds it. For
the OSGi runtime, put the jar where the plug-in loader finds it. The three extension points do the
rest. The plug-in needs no configuration file and no registration call.

| Engine flavour | Where the jar goes | How the engine finds it |
| --- | --- | --- |
| **POJO runtime** - the `birt-runtime` distribution, `Platform.startup`, `ChartEngine.instance(new PlatformConfig())`, `ReportRunner`, or a servlet application with the engine on its classpath | `ReportEngine/lib/`, or any other classpath entry: a Maven dependency, an exploded `classes` directory, an extra `-cp` element | the POJO platform of BIRT scans the class loader for `META-INF/MANIFEST.MF` entries, and reads the `plugin.xml` next to each one |
| **`birt.war` / BIRT Viewer** | `WEB-INF/lib/` | the same classpath scan, over the class loader of the web application |
| **OSGi runtime, or any application that sets `BIRT_HOME`** | `<BIRT_HOME>/platform/plugins/` | Equinox resolves the plug-in from its manifest and registers the three extensions in the extension registry |
| **Eclipse BIRT Designer** | `<eclipse>/dropins/plugins/`, then restart with `-clean` | the same Equinox extension registry; the Designer renders and previews a report that already contains the series, but the chart wizard cannot create one |

For the POJO runtime you must not set `BIRT_HOME`. If `BIRT_HOME` is set, then BIRT starts Equinox.
The jar must then be in `platform/plugins` and not on the classpath.

To copy the jar into an unpacked distribution, run a command of this form:

```powershell
Copy-Item .\build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar `
          C:\birt-runtime-4.24.0\ReportEngine\lib\
```

Only the test suite of this module exercises the POJO path. `RuntimeSmokeIT` runs the sample report
through a real, unpacked runtime. The OSGi runtime and the Designer resolve the series through the
same extension points, so they can behave in the same way. This module does not verify that.

### Compatibility

| Item | Requirement |
| --- | --- |
| Java runtime | **21 or newer.** The build compiles the classes with `release 21` (class file version 65), and the manifest declares `Bundle-RequiredExecutionEnvironment: JavaSE-21`. |
| BIRT, under the OSGi runtime | **4.24 or newer**, and below 5.0. The manifest pins `Require-Bundle: org.eclipse.birt.chart.engine;bundle-version="[4.24.0,5.0.0)"`, and Equinox refuses to resolve the plug-in outside that range. |
| BIRT, on a POJO classpath | There is no version check. The classpath must carry the BIRT version that the build compiled the jar against (4.24.0 as shipped). |

**Older BIRT and older Java.** The BIRT chart API that this plug-in uses is unchanged back to
**BIRT 4.13**. That API is `LineSeries`, the `Line` renderer, `DataPointHints`, the three extension
points and the EMF model plumbing. Only the Java level that BIRT declares differs: BIRT 4.13 to
4.19 declare `JavaSE-11`, and BIRT 4.21 and later declare `JavaSE-21`. A Java 11 build of this
plug-in for an older engine is therefore a small, mechanical rebuild and not a port.

Precondition: you hold the sources and a JDK 11.

1. Lower `<release>` in `pom.xml` from 21 to 11.
2. Replace the `record Expanded(…)` in `render/PiecewiseConstantLine.java`.
3. Replace the pattern-matching `instanceof` in `render/PiecewiseConstantExpander.java`
   (`isNullValue`: `v instanceof Number n`).
4. Replace the pattern-matching `instanceof` in `render/PiecewiseConstantLine.java`
   (`series instanceof PiecewiseConstantSeries step`).
5. Replace the arrow `switch` over `StepMode` in `render/PiecewiseConstantExpander.java`.
6. Widen `Require-Bundle` to `[4.13.0,5.0.0)` in `META-INF/MANIFEST.MF`.
7. Set `Bundle-RequiredExecutionEnvironment: JavaSE-11` in `META-INF/MANIFEST.MF`.
8. Point `birt.version` in `pom.xml` at the BIRT release that you target.
9. Rebuild the plug-in.

Steps 2 to 5 replace the four Java 14 and Java 16 constructs in `src/`. Note: this rebuild is
possible on request. It is not what the published jar is. The released jar is Java 21 and BIRT 4.24
only.

### Troubleshooting

**The chart renders as a plain line with straight segments.** The jar is not on the class loader
that the engine scans. The engine log carries a line like this one:

```
SEVERE: (ECLIPSE-ENV) Could not find series renderer impl for io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl
```

The message key is `error.eclenv.cannot.find.series.renderer`. The standalone chart engine logs the
same text with the `(STANDALONE-ENV)` prefix.

1. Check that the jar really sits in `ReportEngine/lib`, in `WEB-INF/lib`, or in
   `platform/plugins` for the OSGi runtime.
2. Check that nobody unpacked the jar into a plain classes folder without its `plugin.xml`.
3. Check that `BIRT_HOME` matches the engine flavour that you want to run.

**`UnsupportedClassVersionError … class file version 65.0`.** The engine runs on a JRE older than
21. Run the engine on a Java 21 JRE or newer, or rebuild it for Java 11 as described above.

**The series loads, but as a plain `LineSeries`.** The chart shows no steps, and the round trip
loses the `StepMode`. The JVM loaded the chart serializer before the BIRT platform came up. See
[Class-loading order](#class-loading-order).

## Using it

### Java API

```java
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.data.impl.SeriesDefinitionImpl;

import io.github.lextpf.birt.chart.piecewiseconstant.PiecewiseConstantSetup;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl;

// Only for a chart engine started with the STANDALONE flag - see below.
PiecewiseConstantSetup.registerStandalone();

PiecewiseConstantSeries series = (PiecewiseConstantSeries) PiecewiseConstantSeriesImpl.create();
series.setStepMode(StepMode.CENTER_LITERAL);
series.setDataSet(NumberDataSetImpl.create(new Double[] { 12.5, 19.6, 18.3, 13.2, 26.5 }));

SeriesDefinition valueDefinition = SeriesDefinitionImpl.create();
chart.getPrimaryOrthogonalAxis(chart.getPrimaryBaseAxes()[0]).getSeriesDefinitions()
        .add(valueDefinition);
valueDefinition.getSeries().add(series);
```

The snippet adds the series to the value axis (BIRT: `getPrimaryOrthogonalAxis`) of the first
category axis (BIRT: `getPrimaryBaseAxes`).

`PiecewiseConstantSeriesImpl.create()` returns a series with the usual line series defaults of
BIRT: one visible box marker, a line label and `connectMissingValue = true`. It also sets
`StepMode` explicitly to `After`. `PiecewiseConstantSeriesImpl.createDefault()` returns the "unset"
variant that BIRT computes its defaults with. That variant serializes without a `<StepMode>`
element.

### In a `.rptdesign`

The report holds a chart as the `xmlRepresentation` CDATA of an `<extended-item
extensionName="Chart">`. In that chart XML the root element declares the namespace of this plug-in.
The value series carries the `xsi:type` and the step mode:

```xml
<model:ChartWithAxes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xmlns:attribute="http://www.birt.eclipse.org/ChartModelAttribute"
                     xmlns:data="http://www.birt.eclipse.org/ChartModelData"
                     xmlns:layout="http://www.birt.eclipse.org/ChartModelLayout"
                     xmlns:model="http://www.birt.eclipse.org/ChartModel"
                     xmlns:piecewise="http://lextpf.github.io/birt/chart/PiecewiseConstantModelType">
  ...
        <Series xsi:type="piecewise:PiecewiseConstantSeries">
          <DataDefinition>
            <Definition>row[&quot;value&quot;]</Definition>
          </DataDefinition>
          <SeriesIdentifier>series1</SeriesIdentifier>
          ...
          <StepMode>After</StepMode>
        </Series>
  ...
</model:ChartWithAxes>
```

`StepMode` is a child **element** and not an attribute. Its only legal values are `After`, `Before`
and `Center`. [`test/piecewise-constant-sample.rptdesign`](test/piecewise-constant-sample.rptdesign)
is a complete, runnable example. It holds a scripted five-row data set (`category`, `value`) and a
chart item bound to that data set.

### `PiecewiseConstantSetup.registerStandalone()`

There are three ways to run the chart engine. Only one of them needs setup code.

| Environment | How the chart engine finds the series | Setup needed |
| --- | --- | --- |
| OSGi runtime (Designer, OSGi report engine) | Equinox extension registry | none |
| POJO runtime (`Platform.startup`, `ChartEngine.instance(new PlatformConfig())`, `ReportRunner`) | classpath scan for `plugin.xml` | none |
| standalone chart engine (`config.setProperty(PluginSettings.PROP_STANDALONE, "true")`) | hard-coded arrays inside `PluginSettings` | `PiecewiseConstantSetup.registerStandalone()` |

The standalone chart engine skips `Platform.startup` entirely. It resolves renderers from a fixed
list of the series types that BIRT ships with. Without setup code it therefore logs the
`error.stdenv.cannot.find.series.renderer` message and draws nothing. See
[Troubleshooting](#troubleshooting) for the exact log line.
`PiecewiseConstantSetup.registerStandalone()` appends the piecewise constant series to those lists
and registers the EMF package.

The caller must call the method one time. The call must come after the `PluginSettings` singleton
exists. The call must come before the chart engine builds the first chart. Note: the method is
idempotent, and it is harmless in the other two environments.

### Class-loading order

The chart serializer caches the set of chart model packages in a **static initializer**. The JVM can
load `org.eclipse.birt.chart.model.impl.SerializerImpl` (or `ChartDynamicExtension`) before the BIRT
platform is up. That cache then stays empty for the rest of the life of the JVM. A piecewise
constant series then deserializes silently as a plain `LineSeries`. You must start the platform
(`Platform.startup`), or call `PiecewiseConstantSetup.registerStandalone()`, before you touch the
serializer.

## Behaviour notes

- **Category axis (BIRT: base axis).** The steps run centre to centre. The data point of a category
  sits in the middle of its slot. An `After` step or a `Before` step therefore spans from one
  category centre to the next one. A `Center` step turns exactly on the boundary between two
  categories.
- **Missing values.** A missing value never carries a corner vertex. With
  `connectMissingValue = true` (the default of BIRT) the line steps straight across the missing
  value. The line runs from the last real data point to the next real one. With
  `connectMissingValue = false` the run breaks. BIRT then draws the isolated data points as
  markers, exactly as it does for a stock line series.
- **Equal neighbours.** Two equal values form one tread and produce no corner vertex. A constant
  series therefore draws as a single straight line.
- **`curve`.** The renderer ignores `curve`. A piecewise constant line has no spline, so a series
  with `curve = true` still draws as treads and steps.
- **Stacking and percent axes.** The renderer expands each series after BIRT has stacked it. Every
  series of a stacked group is therefore its own piecewise constant line on top of the previous
  one.
- **3D.** The renderer inserts each corner vertex in the depth plane of the data point that owns
  it. The stock 3D line renderer then draws the tape. The test suite checks the 3D case only for
  the absence of an exception: the 3D geometry test asserts that the render completes and that no
  `ClassCastException` occurs. The test suite checks the 2D case vertex by vertex against a
  reference chart. The 3D case is not the intended use of this plug-in.
- **Tooltips and hotspots.** Tooltips and hotspots on line segments do nothing in the Swing device
  and in the PNG device. This is a BIRT limitation and is not specific to this series. They work in
  the SVG device.

## Building from source

Precondition: the machine runs Windows with PowerShell 7.

1. Run the setup script one time:

   ```powershell
   .\setup.ps1
   ```

2. Build the jars:

   ```powershell
   .\build.ps1
   ```

3. Run the tests:

   ```powershell
   .\test.ps1
   ```

Three scripts work together. [`toolchain.ps1`](toolchain.ps1) holds the shared part, and
`build.ps1` and `test.ps1` dot-source it. It reads `.env`, it validates `JAVA_HOME` and
`MAVEN_HOME`, and it runs Maven with the pinned JDK. It starts no build on its own. If a path is
missing or wrong, then it prints a help screen and exits with code 1.

**Step 1.** The script detects a **JDK 21** and an Apache Maven installation, and validates both.
`bin\java.exe` must report Java 21 or newer, and the Maven root must contain `bin\mvn.cmd`. Note:
the script then writes the git-ignored `.env`.

Pass `-JavaHome`, `-MavenHome` or `-BirtRuntimeDir` when the detection finds nothing or finds the
wrong installation. Pass `-NonInteractive` to fail instead of a prompt. CI uses that parameter.
Pass `-Force` to overwrite an existing `.env`. You can also skip `setup.ps1` entirely. Then you
must set the environment variables `JAVA_HOME`, `MAVEN_HOME` and, optionally, `BIRT_RUNTIME_DIR`
yourself.

Git ignores `.env`, so no local path ever reaches the repository.
[`.env.example`](.env.example) documents the keys. When `.env` exists, its values **override the
environment**. `.env` is the pinned toolchain of the project, and a machine-wide `JAVA_HOME` often
points at a different JDK.

**Step 2.** `build.ps1` builds the jars, and it runs **no test**. Without arguments it runs
`mvn -B -ntp -DskipTests clean package`. With arguments it replaces the goals: `.\build.ps1 install`
runs `mvn -B -ntp -DskipTests install`. It forwards every argument unchanged, and it exits
with the exit code of Maven. The project does not build with the default settings of a newer JDK,
and the script does not need Maven on `PATH`. The build produces the jar at:

```
build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar
```

The script always prepends `-DskipTests`. Maven reads the command line from left to right, and the
last value of a property wins. You can therefore switch the tests back on:

```powershell
.\build.ps1 clean install -DskipTests=false
```

**Step 3.** `test.ps1` runs `mvn -B -ntp test`, and it reports the results. It writes the complete
Maven output to `build\test.log`. On the console it shows one line per test class while the run
goes on. It then reads the surefire reports in `build\surefire-reports` and prints a table:

```
Test class                     Tests  Failures  Errors  Skipped   Seconds
-----------------------------------------------------------------------
PiecewiseConstantExpanderTest     17         0       0        0     0.158
PiecewiseConstantGeometryTest     16         0       0        0     3.637
PluginDiscoveryTest                6         0       0        0     0.358
RenderSmokeTest                    6         0       0        0     1.868
RuntimeSmokeIT                     1         0       0        0     1.697
SampleReportTest                   4         0       0        0     0.430
SerializerRoundTripTest            7         0       0        0     0.432
StandaloneFallbackTest             4         0       0        0     0.778
-----------------------------------------------------------------------
TOTAL (8 classes)                 61         0       0        0     9.358

RESULT: PASS (61 tests)
```

For every failed test the script prints the class, the method and the first line of the message.
The last line is `RESULT: PASS (n tests)` in green, or `RESULT: FAIL (n failures, m errors)` in
red. The exit code is 0 only when Maven succeeded **and** no test failed.

The script deletes the reports of the previous run before it starts Maven. The table therefore
shows the current run only. It takes these parameters:

| Parameter | Effect |
| --- | --- |
| `-Test <pattern>` | passes `-Dtest=<pattern>` to surefire. A pattern is a class name, for example `RenderSmokeTest` or `*ExpanderTest`. |
| `-Runtime <dir>` | passes `-Dbirt.runtime.dir=<dir>`. This value overrides `BIRT_RUNTIME_DIR` from `.env`. |
| `-ShowLog` | streams the full Maven output to the console. The log file still gets every line. |

The script forwards every further argument to Maven, for example `-Dsurefire.timeout=300`.

If a `-Test` pattern matches no class, then Maven fails the run and writes no report. The script
then names the pattern, prints the last 30 lines of `build\test.log`, and exits with a non-zero
code. The script does not pass `-Dsurefire.failIfNoSpecifiedTests=false`, because a pattern with a
typo must not report success.

`build.ps1` and `test.ps1` are convenience wrappers for Windows and not a build system. On any
other platform, and in CI, you must point `JAVA_HOME` at a JDK 21. You must then call plain
`mvn -B -ntp clean verify` in the project root.

### Running the runtime smoke test

`RuntimeSmokeIT` runs the sample report through a real, unpacked BIRT report runtime. It asserts
that the HTML output contains an SVG chart. The distribution is a download of about 100 MB and is
not part of the build. The test therefore stays **skipped** until you point it at a distribution.
`birt.runtime.dir` is the directory that contains `ReportEngine\lib`.

The easy way is to configure the directory one time.

Precondition: the machine holds an unpacked BIRT runtime distribution.

1. Set `BIRT_RUNTIME_DIR` in `.env`, or set the environment variable of the same name:

   ```powershell
   .\setup.ps1 -BirtRuntimeDir C:\birt-runtime-4.24.0
   ```

2. Build the jar, then run the whole suite:

   ```powershell
   .\build.ps1
   .\test.ps1
   ```

Note: `test.ps1` appends `-Dbirt.runtime.dir` by itself when `BIRT_RUNTIME_DIR` is configured, so
step 2 runs the test along with the rest of the suite.

You can also name the runtime explicitly. An explicit value overrides the configured one:

```powershell
.\build.ps1
.\test.ps1 -Test RuntimeSmokeIT -Runtime C:\birt-runtime-4.24.0
```

Build the jar first. The test puts
`build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar` on the class loader of the runtime.
If the build has not packaged the project yet, then the test falls back to `build\classes`, which
carries the same `plugin.xml`. CI uses the
[`birt-runtime-4.24.0-202606100854.zip`](https://download.eclipse.org/birt/updates/release/4.24.0/downloads/birt-runtime-4.24.0-202606100854.zip)
distribution.

## Continuous integration

[`.github/workflows/build.yml`](.github/workflows/build.yml) runs on every push to `main`, on every
pull request and on every `v*` tag. It has three jobs:

1. **build** runs `mvn -B -ntp clean verify` on Ubuntu with Temurin 21. It uploads two artifacts:
   the jars (`plugin-jar`) and the PNG files that the render tests produce (`test-renders`).
2. **runtime-smoke** downloads and caches the BIRT 4.24.0 runtime distribution. It then runs
   `RuntimeSmokeIT` against that distribution, the same end-to-end check described above.
3. **release** runs for tags only. See the next section.

## Releasing

This section is for the maintainer. A release is a tag.

Precondition: the working tree is clean, and the whole test suite passes.

1. Bump `<version>` in [`pom.xml`](pom.xml).
2. Set `Bundle-Version` in [`META-INF/MANIFEST.MF`](META-INF/MANIFEST.MF) to the same version,
   because that header is what the OSGi runtime advertises.
3. Commit the version bump.
4. Create the tag:

   ```powershell
   git tag v1.0.1
   ```

5. Push the tag:

   ```powershell
   git push origin v1.0.1
   ```

Note: the pushed tag runs the whole workflow. The workflow builds and tests the plug-in, and runs
the BIRT runtime smoke test. It creates the GitHub Release for the tag only if both jobs pass. The
release carries the jar, the sources jar and generated release notes. The workflow then deploys the
artifact to GitHub Packages.

The release job **refuses a tag that does not match the pom version**. It compares
`mvn help:evaluate -Dexpression=project.version` against the tag name without the leading `v`, and
fails with an explicit message. A forgotten version bump therefore cannot publish a wrong release.

## License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/) (SPDX: `EPL-2.0`).
