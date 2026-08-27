# BIRT Chart Step Line Series

`io.github.lextpf.birt.chart.stepline` is an Eclipse BIRT chart engine extension that adds a
**StepLineSeries**: a line series rendered as a piecewise-constant (staircase) curve instead of
straight interpolated segments. Each data point holds its value until the next point, with the
step transition placed `After`, `Before` or at the `Center` of the interval.

The bundle plugs into the BIRT chart engine through the standard `modelrenderers`,
`datasetprocessors` and `charttypes` extension points, targets **BIRT 4.24.0** and contains no UI
code. `StepLineSeries` is a subclass of BIRT's own `LineSeries`, so markers, data point labels,
shadows, stacking, percent axes, transposed (horizontal) charts, 3D and the legend all keep
working; only the path between the points changes.

> **Scope:** the Designer's chart wizard is explicitly **out of scope**. There is no wizard page
> for the step line, so a step line chart cannot be *authored* through the Designer UI - it is
> authored through the Java API or by editing the chart XML in the `.rptdesign` (see below).
> Rendering was verified for the POJO runtime path only, by this module's test suite; with the jar
> deployed, the Designer's preview and an OSGi runtime are *expected* to render it the same way,
> through the standard extension points, but that has not been verified here.

## Step modes

| Mode | XML literal | Where the line jumps | Shape between two points *p* and *q* |
| --- | --- | --- | --- |
| `StepMode.AFTER_LITERAL` | `After` | at the **next** point | hold *p*'s value up to *q*'s position, then jump (zero-order hold) |
| `StepMode.BEFORE_LITERAL` | `Before` | at the **current** point | jump to *q*'s value at *p*'s position, then hold |
| `StepMode.CENTER_LITERAL` | `Center` | **halfway** between them | hold, jump at the midpoint, hold - on a category axis the midpoint is the category boundary |

`After` is the default: a series whose `StepMode` was never set behaves as `After` and writes no
`<StepMode>` element.

## Build

```powershell
.\build.ps1 clean install
```

`build.ps1` pins **JDK 21** (`C:\Program Files\Java\jdk-21.0.12.1`) and the local Maven install,
then forwards all arguments to Maven - the project does not build on a newer JDK's default
settings, and Maven is not expected to be on `PATH`. The bundle jar is produced at

```
build\io.github.lextpf.birt.chart.stepline_1.0.0.jar
```

with `plugin.xml` and an OSGi `META-INF/MANIFEST.MF` at its root, which is what makes it a BIRT
plug-in in both of BIRT's environments.

Run the test suite with:

```powershell
.\build.ps1 clean verify
```

## Deployment

### (a) POJO report engine / `birt-runtime` distribution

Download and unpack
[`birt-runtime-4.24.0-202606100854.zip`](https://download.eclipse.org/birt/updates/release/4.24.0/downloads/birt-runtime-4.24.0-202606100854.zip),
then copy the jar next to the engine's own jars:

```powershell
Copy-Item .\build\io.github.lextpf.birt.chart.stepline_1.0.0.jar `
          C:\birt-runtime-4.24.0\ReportEngine\lib\
```

Nothing else is needed. BIRT's POJO platform (`Platform.startup`, or `ReportRunner`, or a servlet
container with the engine on its classpath) scans the class loader for `META-INF/MANIFEST.MF`
entries and reads the `plugin.xml` next to each of them, so any classpath entry works - a jar in
`ReportEngine/lib`, a Maven dependency, or an exploded `classes` directory.

Make sure `BIRT_HOME` is **not** set when you use the POJO engine; it would make BIRT start Equinox
instead.

### (b) OSGi runtime / Eclipse BIRT Designer

Copy the same jar into the OSGi installation's dropins:

```
<eclipse>\dropins\plugins\io.github.lextpf.birt.chart.stepline_1.0.0.jar
```

and restart with `-clean`. Equinox reads the bundle manifest and registers the three extensions.
Reports containing a step line series are then *expected* to preview and render in the Designer
just as they do in the POJO runtime, since both resolve the series through the same extension
points - this path was not exercised on a Designer install, only the POJO runtime path was, by the
module's test suite. Either way - as stated above - the chart wizard offers no way to create one.

### (c) Java API

```java
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.data.impl.SeriesDefinitionImpl;

import io.github.lextpf.birt.chart.stepline.StepLineSetup;
import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;
import io.github.lextpf.birt.chart.stepline.model.type.impl.StepLineSeriesImpl;

// Only for a chart engine started with the STANDALONE flag - see below.
StepLineSetup.registerStandalone();

StepLineSeries series = (StepLineSeries) StepLineSeriesImpl.create();
series.setStepMode(StepMode.CENTER_LITERAL);
series.setDataSet(NumberDataSetImpl.create(new Double[] { 12.5, 19.6, 18.3, 13.2, 26.5 }));

SeriesDefinition valueDefinition = SeriesDefinitionImpl.create();
chart.getPrimaryOrthogonalAxis(chart.getPrimaryBaseAxes()[0]).getSeriesDefinitions()
        .add(valueDefinition);
valueDefinition.getSeries().add(series);
```

`StepLineSeriesImpl.create()` returns a series with BIRT's usual line-series defaults (one visible
box marker, a line label, `connectMissingValue = true`) and an explicitly set `StepMode` of
`After`; `StepLineSeriesImpl.createDefault()` returns the "unset" variant BIRT uses to compute
defaults with, which serializes without a `<StepMode>` element.

#### `StepLineSetup.registerStandalone()`

There are three ways to run the chart engine, and only one of them needs setup code:

| Environment | How the series is found | Setup needed |
| --- | --- | --- |
| OSGi (Designer, OSGi report engine) | Equinox extension registry | none |
| POJO runtime (`Platform.startup`, `ChartEngine.instance(new PlatformConfig())`, `ReportRunner`) | classpath scan for `plugin.xml` | none |
| Standalone (`config.setProperty(PluginSettings.PROP_STANDALONE, "true")`) | hard-coded arrays inside `PluginSettings` | `StepLineSetup.registerStandalone()` |

The standalone chart engine skips `Platform.startup` entirely and resolves renderers from a fixed
list of the series types BIRT ships with, so it would report *"Could not find series renderer
impl"* and draw nothing. `StepLineSetup.registerStandalone()` appends the step line to those
lists and registers the EMF package. Call it once, after the `PluginSettings` singleton exists and
before the first chart is built. It is idempotent and harmless in the other two environments.

#### Class-loading order

The chart serializer caches the set of chart model packages in a **static initializer**. If
`org.eclipse.birt.chart.model.impl.SerializerImpl` (or `ChartDynamicExtension`) is loaded *before*
the platform is up, that cache is empty for the rest of the JVM's life and a step line series
silently deserializes as a plain `LineSeries`. Start the platform - or call
`StepLineSetup.registerStandalone()` - before touching the serializer.

## In a `.rptdesign`

A chart lives in the report as the `xmlRepresentation` CDATA of an `<extended-item
extensionName="Chart">`. Inside that chart XML the root element declares our namespace and the
value series carries the `xsi:type` and the mode:

```xml
<model:ChartWithAxes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xmlns:attribute="http://www.birt.eclipse.org/ChartModelAttribute"
                     xmlns:data="http://www.birt.eclipse.org/ChartModelData"
                     xmlns:layout="http://www.birt.eclipse.org/ChartModelLayout"
                     xmlns:model="http://www.birt.eclipse.org/ChartModel"
                     xmlns:stepline="http://lextpf.github.io/birt/chart/StepLineModelType">
  ...
        <Series xsi:type="stepline:StepLineSeries">
          <DataDefinition>
            <Definition>row[&quot;value&quot;]</Definition>
          </DataDefinition>
          <SeriesIdentifier>Value</SeriesIdentifier>
          ...
          <StepMode>After</StepMode>
        </Series>
  ...
</model:ChartWithAxes>
```

`StepMode` is a child **element**, not an attribute, and its only legal values are `After`,
`Before` and `Center`. A complete, runnable example is
[`test/stepline-sample.rptdesign`](test/stepline-sample.rptdesign):
a scripted five-row data set (`category`, `value`) with a chart item bound to it.

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
- **`curve`.** Ignored. A piecewise-constant line has no spline, so a series with `curve = true`
  is still drawn as a staircase.
- **Stacking and percent axes.** Each series is expanded after BIRT has stacked it, so every series
  of a stacked group is its own staircase on top of the previous one.
- **3D.** Best effort: the corners are inserted in the depth plane of the point they belong to and
  the stock 3D line renderer draws the tape. It looks right, but the 3D case has far fewer degrees
  of freedom than the 2D one and is not the intended use.
- **Tooltips/hotspots** on line segments are a no-op in the Swing/PNG device - a pre-existing BIRT
  limitation, not specific to this series. They work in the SVG device.

## Running the runtime smoke test

`RuntimeSmokeIT` runs the sample report through a real, unpacked BIRT report runtime and asserts
that the HTML output contains an SVG chart. The distribution is a ~100 MB download and is not part
of the build, so the test is **skipped** unless it is pointed at one:

```powershell
.\build.ps1 clean install
.\build.ps1 test -Dtest=RuntimeSmokeIT -Dbirt.runtime.dir=C:\birt-runtime-4.24.0
```

`birt.runtime.dir` is the directory that contains `ReportEngine\lib`. Build the jar first: the test
puts `build\io.github.lextpf.birt.chart.stepline_1.0.0.jar` on the runtime's class loader (falling
back to `build\classes`, which carries the same `plugin.xml`, when the project has not been
packaged yet).

## License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/) (SPDX: `EPL-2.0`).
