# BIRT Chart Piecewise Constant Series

[![build](https://github.com/lextpf/birt-piecewise-constant/actions/workflows/build.yml/badge.svg)](https://github.com/lextpf/birt-piecewise-constant/actions/workflows/build.yml)

`io.github.lextpf.birt.chart.piecewiseconstant` is an Eclipse BIRT chart engine extension that adds
a **PiecewiseConstantSeries**: a line series drawn as a *piecewise constant* function - the value
of each data point is held until the jump to the next one, so the curve is a run of horizontal
treads and vertical risers instead of straight interpolated segments. The same series is also
known as a **step line**, a **staircase** or a **zero-order-hold** series; those are synonyms for
what this bundle calls piecewise constant. The jump is placed `After`, `Before` or at the `Center`
of the interval.

The bundle plugs into the BIRT chart engine through the standard `modelrenderers`,
`datasetprocessors` and `charttypes` extension points, targets **BIRT 4.24.0** and contains no UI
code. `PiecewiseConstantSeries` is a subclass of BIRT's own `LineSeries`, so markers, data point
labels, shadows, stacking, percent axes, transposed (horizontal) charts, 3D and the legend all
keep working; only the path between the points changes.

> **Scope:** the Designer's chart wizard is explicitly **out of scope**. There is no wizard page
> for this series, so a piecewise constant chart cannot be *authored* through the Designer UI - it
> is authored through the Java API or by editing the chart XML in the `.rptdesign` (see below).
> Rendering was verified for the POJO runtime path only, by this module's test suite; with the jar
> deployed, the Designer's preview and an OSGi runtime are *expected* to render it the same way,
> through the standard extension points, but that has not been verified here.

## Step modes

| Mode | XML literal | Where the line jumps | Shape between two points *p* and *q* | Also called |
| --- | --- | --- | --- | --- |
| `StepMode.AFTER_LITERAL` | `After` | at the **next** point | hold *p*'s value up to *q*'s position, then jump | zero-order hold, sample-and-hold; the right-continuous (càdlàg) step function |
| `StepMode.BEFORE_LITERAL` | `Before` | at the **current** point | jump to *q*'s value at *p*'s position, then hold | left-continuous step |
| `StepMode.CENTER_LITERAL` | `Center` | **halfway** between them | hold, jump at the midpoint, hold - on a category axis the midpoint is the category boundary | midpoint step |

`After` is the default: a series whose `StepMode` was never set behaves as `After` and writes no
`<StepMode>` element.

## Getting the jar

### (a) From the GitHub Releases page

Every `v*` tag publishes a release with both artifacts attached. Download

```
io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar
```

from [the latest release](https://github.com/lextpf/birt-piecewise-constant/releases/latest); the
matching `…_1.0.0-sources.jar` is attached next to it. This is the file you drop into an engine -
it carries `plugin.xml` and an OSGi `META-INF/MANIFEST.MF` at its root, which is what makes it a
BIRT plug-in in both of BIRT's environments.

### (b) From GitHub Packages, for Maven users

The same release publishes the artifact to GitHub Packages under

```xml
<dependency>
  <groupId>io.github.lextpf.birt</groupId>
  <artifactId>io.github.lextpf.birt.chart.piecewiseconstant</artifactId>
  <version>1.0.0</version>
</dependency>
```

Add the repository to the consuming `pom.xml` (or to a profile in `settings.xml`):

```xml
<repositories>
  <repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/lextpf/birt-piecewise-constant</url>
  </repository>
</repositories>
```

> **GitHub Packages always needs credentials** - even for a public package, an anonymous fetch is
> rejected with `401 Unauthorized`. Add a matching server entry to `~/.m2/settings.xml` with a
> personal access token (classic) that has the **`read:packages`** scope:
>
> ```xml
> <servers>
>   <server>
>     <id>github</id>
>     <username>your-github-username</username>
>     <password>ghp_your_personal_access_token</password>
>   </server>
> </servers>
> ```
>
> The `<id>` must match the `<repository>` id. If you would rather not deal with tokens, use the
> release download (a) or build locally (c).

### (c) Build it locally

```powershell
.\setup.ps1
.\build.ps1 clean install
```

produces `build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar` (plus the sources jar) and
installs both into the local repository under `~/.m2/repository`, where a sibling project can
depend on them with the coordinates above and no repository entry at all. See
[Building from source](#building-from-source) for what the two scripts do.

## Integrating into a BIRT engine

Deployment is always the same: put the jar somewhere the engine's class loader (POJO) or bundle
loader (OSGi) looks, and the three extension points do the rest. No configuration file, no
registration call.

| Engine flavour | Where the jar goes | How it is found |
| --- | --- | --- |
| **POJO report engine** - the `birt-runtime` distribution, `Platform.startup`, `ChartEngine.instance(new PlatformConfig())`, `ReportRunner`, or a servlet app with the engine on its classpath | `ReportEngine/lib/` - or any other classpath entry: a Maven dependency, an exploded `classes` directory, an extra `-cp` element | BIRT's POJO platform scans the class loader for `META-INF/MANIFEST.MF` entries and reads the `plugin.xml` next to each one |
| **`birt.war` / BIRT Viewer** | `WEB-INF/lib/` | the same classpath scan, run over the webapp's class loader |
| **OSGi report engine / any app that sets `BIRT_HOME`** | `<BIRT_HOME>/platform/plugins/` | Equinox resolves the bundle from its manifest and registers the three extensions in the extension registry |
| **Eclipse BIRT Designer** | `<eclipse>/dropins/plugins/`, then restart with `-clean` | the same Equinox extension registry; renders and previews a report that already contains the series, but the chart wizard cannot author one |

For the POJO engine make sure `BIRT_HOME` is **not** set - it would make BIRT start Equinox
instead, and then the jar needs to be in `platform/plugins` rather than on the classpath.

Copying the jar into an unpacked distribution, for example:

```powershell
Copy-Item .\build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar `
          C:\birt-runtime-4.24.0\ReportEngine\lib\
```

Only the POJO path is exercised by this module's test suite (`RuntimeSmokeIT` runs the sample
report through a real, unpacked runtime). The OSGi and Designer rows resolve the series through the
very same extension points and are *expected* to behave identically, but that was not verified
here.

### Compatibility

| | Requirement |
| --- | --- |
| Java runtime | **21 or newer.** The classes are compiled with `release 21` (class file version 65) and the manifest declares `Bundle-RequiredExecutionEnvironment: JavaSE-21`. |
| BIRT, under OSGi | **4.24 or newer** (below 5.0): the manifest's `Require-Bundle` pins `org.eclipse.birt.chart.engine;bundle-version="[4.24.0,5.0.0)"`, and Equinox refuses to resolve the bundle outside that range. |
| BIRT, on a POJO classpath | there is no version check at all - the constraint is simply that the engine on the classpath is the one the jar was compiled against (4.24.0 as shipped). |

**Older BIRT / older Java.** The BIRT chart API this extension uses (`LineSeries`, the `Line`
renderer, `DataPointHints`, the three extension points, the EMF model plumbing) is unchanged back
to **BIRT 4.13**. What differs is the Java level BIRT itself declares: 4.13-4.19 are `JavaSE-11`, 4.21 and
later are `JavaSE-21`. So a Java 11 build of this bundle for an older engine is a small, mechanical
rebuild rather than a port:

- lower `<release>` in `pom.xml` from 21 to 11,
- replace the four Java 14+/16+ constructs in `src/`:
  - the `record Expanded(…)` in `render/PiecewiseConstantLine.java`,
  - the pattern-matching `instanceof` in `render/PiecewiseConstantExpander.java`
    (`isNullValue`: `v instanceof Number n`),
  - the pattern-matching `instanceof` in `render/PiecewiseConstantLine.java`
    (`series instanceof PiecewiseConstantSeries step`),
  - the arrow-`switch` over `StepMode` in `render/PiecewiseConstantExpander.java`,
- widen `Require-Bundle` to `[4.13.0,5.0.0)` and set `Bundle-RequiredExecutionEnvironment: JavaSE-11`
  in `META-INF/MANIFEST.MF`,
- point `birt.version` at the BIRT release you are targeting and rebuild.

That is **possible on request / on a rebuild** - it is not what the published jar is. The released
artifact is Java 21 and BIRT 4.24 only.

### Troubleshooting

- **The chart renders as a plain, straight-segment line.** The jar is not on the class loader the
  engine scans. The engine log carries a line like
  `SEVERE: (ECLIPSE-ENV) Could not find series renderer impl for io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl`
  (`error.eclenv.cannot.find.series.renderer`; the standalone engine logs the same text with the
  `(STANDALONE-ENV)` prefix instead).
  Check that the jar really sits in `ReportEngine/lib` (or `WEB-INF/lib`, or `platform/plugins` for
  OSGi), that it was not unpacked into a plain classes folder without its `plugin.xml`, and that
  `BIRT_HOME` matches the flavour you meant to run.
- **`UnsupportedClassVersionError … class file version 65.0`.** The engine is running on a JRE
  older than 21. Either run it on a Java 21+ JRE, or rebuild for Java 11 as described above.
- **The series loads, but as a plain `LineSeries`** - no steps, `StepMode` lost on the round trip.
  The chart serializer was class-loaded before the platform came up; see
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

`PiecewiseConstantSeriesImpl.create()` returns a series with BIRT's usual line-series defaults
(one visible box marker, a line label, `connectMissingValue = true`) and an explicitly set
`StepMode` of `After`; `PiecewiseConstantSeriesImpl.createDefault()` returns the "unset" variant
BIRT uses to compute defaults with, which serializes without a `<StepMode>` element.

### In a `.rptdesign`

A chart lives in the report as the `xmlRepresentation` CDATA of an `<extended-item
extensionName="Chart">`. Inside that chart XML the root element declares our namespace and the
value series carries the `xsi:type` and the mode:

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

`StepMode` is a child **element**, not an attribute, and its only legal values are `After`,
`Before` and `Center`. A complete, runnable example is
[`test/piecewise-constant-sample.rptdesign`](test/piecewise-constant-sample.rptdesign):
a scripted five-row data set (`category`, `value`) with a chart item bound to it.

### `PiecewiseConstantSetup.registerStandalone()`

There are three ways to run the chart engine, and only one of them needs setup code:

| Environment | How the series is found | Setup needed |
| --- | --- | --- |
| OSGi (Designer, OSGi report engine) | Equinox extension registry | none |
| POJO runtime (`Platform.startup`, `ChartEngine.instance(new PlatformConfig())`, `ReportRunner`) | classpath scan for `plugin.xml` | none |
| Standalone (`config.setProperty(PluginSettings.PROP_STANDALONE, "true")`) | hard-coded arrays inside `PluginSettings` | `PiecewiseConstantSetup.registerStandalone()` |

The standalone chart engine skips `Platform.startup` entirely and resolves renderers from a fixed
list of the series types BIRT ships with, so it would report *"Could not find series renderer
impl"* and draw nothing. `PiecewiseConstantSetup.registerStandalone()` appends the piecewise
constant series to those lists and registers the EMF package. Call it once, after the
`PluginSettings` singleton exists and before the first chart is built. It is idempotent and
harmless in the other two environments.

### Class-loading order

The chart serializer caches the set of chart model packages in a **static initializer**. If
`org.eclipse.birt.chart.model.impl.SerializerImpl` (or `ChartDynamicExtension`) is loaded *before*
the platform is up, that cache is empty for the rest of the JVM's life and a piecewise constant
series silently deserializes as a plain `LineSeries`. Start the platform - or call
`PiecewiseConstantSetup.registerStandalone()` - before touching the serializer.

## Behaviour notes

- **Category axes.** The steps run centre-to-centre: a category's data point sits in the middle of
  its slot, so an `After`/`Before` step spans from one category centre to the next, and a `Center`
  step turns exactly on the boundary between two categories.
- **Missing values.** A `null` never carries a corner. With `connectMissingValue = true` (BIRT's
  default) the staircase steps straight across the gap from the last real point to the next one;
  with `false` the run breaks and BIRT draws the isolated points as markers, exactly as it does for
  a stock line series.
- **Equal neighbours.** Two equal values form one flat step and produce no corner, so a constant
  series draws as a single straight line.
- **`curve`.** Ignored. A piecewise constant line has no spline, so a series with `curve = true`
  is still drawn as a staircase.
- **Stacking and percent axes.** Each series is expanded after BIRT has stacked it, so every series
  of a stacked group is its own staircase on top of the previous one.
- **3D.** Best effort: the corners are inserted in the depth plane of the point they belong to and
  the stock 3D line renderer draws the tape. It looks right, but the 3D case has far fewer degrees
  of freedom than the 2D one and is not the intended use.
- **Tooltips/hotspots** on line segments are a no-op in the Swing/PNG device - a pre-existing BIRT
  limitation, not specific to this series. They work in the SVG device.

## Building from source

Run the setup script once - it auto-detects a **JDK 21** and an Apache Maven installation,
validates them (`bin\java.exe` reporting Java 21 or newer, `bin\mvn.cmd`) and writes the
git-ignored `.env`:

```powershell
.\setup.ps1
```

Pass `-JavaHome`, `-MavenHome` or `-BirtRuntimeDir` when the detection guesses wrong or has
nothing to go on, `-NonInteractive` to fail instead of prompting (useful in CI), and `-Force` to
overwrite an existing `.env`. Alternatively, skip `setup.ps1` entirely and set the environment
variables `JAVA_HOME`, `MAVEN_HOME` and (optionally) `BIRT_RUNTIME_DIR` yourself.

`.env` is git-ignored - no local path is ever committed - and [`.env.example`](.env.example)
documents the keys. When `.env` exists its values **win over the environment**: it is the
project's pinned toolchain, and a machine-wide `JAVA_HOME` often points at a different JDK.

Then:

```powershell
.\build.ps1 clean install
```

`build.ps1` reads that configuration, validates it, points Maven at the pinned JDK and forwards
all arguments to Maven - the project does not build on a newer JDK's default settings, and Maven
is not expected to be on `PATH`. The bundle jar is produced at

```
build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar
```

Run the test suite with:

```powershell
.\build.ps1 clean verify
```

`build.ps1` is a Windows convenience wrapper, not a build system: on any other platform (and in
CI) point `JAVA_HOME` at a JDK 21 and call plain `mvn -B -ntp clean verify` in the project root.

### Running the runtime smoke test

`RuntimeSmokeIT` runs the sample report through a real, unpacked BIRT report runtime and asserts
that the HTML output contains an SVG chart. The distribution is a ~100 MB download and is not part
of the build, so the test is **skipped** unless it is pointed at one.

The easy way is to configure it once: set `BIRT_RUNTIME_DIR` in `.env` (`.\setup.ps1 -BirtRuntimeDir
C:\birt-runtime-4.24.0`) or in the environment, and `build.ps1` appends `-Dbirt.runtime.dir`
to every Maven invocation by itself - so

```powershell
.\build.ps1 clean verify
```

then runs the IT along with the rest of the suite. Passing the property explicitly still works and
overrides the configured value:

```powershell
.\build.ps1 clean install
.\build.ps1 test -Dtest=RuntimeSmokeIT -Dbirt.runtime.dir=C:\birt-runtime-4.24.0
```

`birt.runtime.dir` is the directory that contains `ReportEngine\lib`. Build the jar first: the test
puts `build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar` on the runtime's class loader
(falling back to `build\classes`, which carries the same `plugin.xml`, when the project has not
been packaged yet). The
[`birt-runtime-4.24.0-202606100854.zip`](https://download.eclipse.org/birt/updates/release/4.24.0/downloads/birt-runtime-4.24.0-202606100854.zip)
distribution is the one CI uses.

## Continuous integration

[`.github/workflows/build.yml`](.github/workflows/build.yml) runs on every push to `main`, every
pull request and every `v*` tag:

1. **build** - `mvn -B -ntp clean verify` on Ubuntu with Temurin 21, and uploads two artifacts: the
   bundle jars (`plugin-jar`) and the PNGs the render tests produced (`test-renders`).
2. **runtime-smoke** - downloads and caches the BIRT 4.24.0 runtime distribution and runs
   `RuntimeSmokeIT` against it, the same end-to-end check described above.
3. **release** - tags only; see below.

## Releasing

Maintainer note. A release is a tag:

1. Bump `<version>` in [`pom.xml`](pom.xml) - and keep `Bundle-Version` in
   [`META-INF/MANIFEST.MF`](META-INF/MANIFEST.MF) in step with it, since that is what the OSGi
   bundle advertises.
2. Commit the bump.
3. Tag it and push the tag:

   ```powershell
   git tag v1.0.1
   git push origin v1.0.1
   ```

Pushing the tag runs the whole workflow: it builds and tests, runs the BIRT runtime smoke test,
then - only if both passed - creates the GitHub Release for the tag with the bundle jar and the
sources jar attached and generated release notes, and deploys the artifact to GitHub Packages.

The release job **refuses a tag that does not match the pom version**: it compares
`mvn help:evaluate -Dexpression=project.version` against the tag name with the leading `v` stripped
and fails with an explicit message, so a forgotten version bump cannot publish a mislabelled
release.

## License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/) (SPDX: `EPL-2.0`).
