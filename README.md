# BIRT Chart Piecewise Constant Series

`io.github.lextpf.birt.chart.piecewiseconstant` is a plug-in for the Eclipse BIRT chart engine. It
adds a `PiecewiseConstantSeries`. The renderer holds the value of each data point until the next
data point. The line is a run of treads and steps (step line, staircase, zero-order hold). The
series is a subclass of BIRT's `LineSeries`, so markers, labels, stacking, transposed charts and the
legend continue to work. The plug-in contains no UI code.

| Step mode | Where the renderer draws the step | Shape between point *p* and point *q* |
| --- | --- | --- |
| `After` | at the next data point | tread at the value of *p* up to the position of *q*, then step |
| `Before` | at the current data point | step at the position of *p* to the value of *q*, then tread |
| `Center` | at the midpoint between the two data points | tread, step at the midpoint, tread |

`After` is the default step mode. The chart wizard of the Designer has no page for this series:
create the chart with the Java API, or edit the chart XML in the `.rptdesign` file.

## Getting the jar

### Download

Download `io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar` and the sources jar from
[the latest release](https://github.com/lextpf/birt-piecewise-constant/releases/latest). The jar
contains `plugin.xml` and an OSGi `META-INF/MANIFEST.MF` at its root.

### Maven

```xml
<dependencies>
  <dependency>
    <groupId>io.github.lextpf.birt</groupId>
    <artifactId>io.github.lextpf.birt.chart.piecewiseconstant</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/lextpf/birt-piecewise-constant</url>
  </repository>
</repositories>
```

GitHub Packages needs a `<server>` entry in `~/.m2/settings.xml` with your GitHub user name in
`<username>` and a personal access token with the `read:packages` scope in `<password>`. The server
`<id>` must match the repository `<id>`.

### Build from source

Precondition: Windows with PowerShell 7, a JDK 21 and Apache Maven.

```powershell
.\setup.ps1   # writes the git-ignored .env with the tool paths
.\build.ps1   # builds build\io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar, runs no test
.\test.ps1    # runs the test suite and prints the results
```

`.\build.ps1 install` puts the jar into `~/.m2`. On another platform, set `JAVA_HOME` to a JDK 21
and run `mvn -B -ntp -DskipTests clean package`. The jar is then in `build/` on that platform too.

## Installing the plug-in into a BIRT engine

Put the jar where the class loader or plug-in loader of the engine finds it. The plug-in needs no
configuration and no registration call. Its three extension points (`modelrenderers`,
`datasetprocessors`, `charttypes`) register the renderer, the data set processor and the model
loader.

| Engine type | Where the jar goes |
| --- | --- |
| POJO runtime: the `birt-runtime` distribution, `ReportRunner`, `Platform.startup`, a servlet with the engine on its classpath | `ReportEngine/lib/`, or another classpath entry |
| BIRT Viewer, `birt.war` | `WEB-INF/lib/` |
| OSGi runtime, or any application that sets `BIRT_HOME` | `<BIRT_HOME>/platform/plugins/` |
| Eclipse BIRT Designer (it renders and previews) | `<eclipse>/dropins/plugins/`, then restart with `-clean` |

If you use the POJO runtime, then do not set `BIRT_HOME`. A set `BIRT_HOME` starts Equinox.

Requirements: Java 21 or newer. Under the OSGi runtime, BIRT 4.24 up to (not including) 5.0
(`Require-Bundle: org.eclipse.birt.chart.engine;bundle-version="[4.24.0,5.0.0)"`). On a POJO
classpath there is no version check. The build compiled the jar against BIRT 4.24.0.

**Troubleshooting.**

- If the engine does not load the plug-in, then the report run fails with `EngineException: Error
  happened while running the report`, caused by `NullPointerException: Cannot invoke
  "Object.getClass()" because "series" is null` in `AbstractChartBaseQueryGenerator`. `ReportRunner`
  exits with -1 and writes an empty HTML file. Causes: the jar is in the wrong folder, `BIRT_HOME`
  is set for the wrong engine type, or an unpacked jar has no `plugin.xml`.
- If your own code calls `SerializerImpl.read` and it throws `PackageNotFoundException: Package with
  uri 'http://lextpf.github.io/birt/chart/PiecewiseConstantModelType' not found`, then the plug-in
  is not on the classpath, or the JVM loaded the serializer before the platform started (see
  [With the Java API](#with-the-java-api)).
- `UnsupportedClassVersionError ... class file version 65.0` means that the JRE is older than 21.

## Rendering a chart stepwise

### In a `.rptdesign`

The report holds the chart as the `xmlRepresentation` CDATA of an
`<extended-item extensionName="Chart">`. Three edits turn a line chart into a step chart:

1. Add `xmlns:piecewise="http://lextpf.github.io/birt/chart/PiecewiseConstantModelType"` to the
   `<model:ChartWithAxes ...>` root element.
2. Change the value series from `xsi:type="type:LineSeries"` to `xsi:type="piecewise:PiecewiseConstantSeries"`.
3. Optionally add the child element `<StepMode>`. It is an element and not an attribute. Its only
   legal values are `After`, `Before` and `Center`. An absent element means `After`. A different
   value, for example `Centre`, stops the chart XML from loading.

```xml
<model:ChartWithAxes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xmlns:model="http://www.birt.eclipse.org/ChartModel"
                     xmlns:piecewise="http://lextpf.github.io/birt/chart/PiecewiseConstantModelType" ...>
  ...
        <Series xsi:type="piecewise:PiecewiseConstantSeries">
          ...
          <StepMode>After</StepMode>
        </Series>
  ...
</model:ChartWithAxes>
```

[`test/piecewise-constant-sample.rptdesign`](test/piecewise-constant-sample.rptdesign) is a runnable
example with a scripted five-row data set (`category`, `value`). The engine that runs the report
must have [the plug-in installed](#installing-the-plug-in-into-a-birt-engine).

### With the Java API

```java
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.data.impl.SeriesDefinitionImpl;
import io.github.lextpf.birt.chart.piecewiseconstant.PiecewiseConstantSetup;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl;

// Only for the STANDALONE chart engine - see below.
PiecewiseConstantSetup.registerStandalone();

PiecewiseConstantSeries series = (PiecewiseConstantSeries) PiecewiseConstantSeriesImpl.create();
series.setStepMode(StepMode.CENTER_LITERAL);
series.setDataSet(NumberDataSetImpl.create(new Double[] { 12.5, 19.6, 18.3, 13.2, 26.5 }));
SeriesDefinition valueDefinition = SeriesDefinitionImpl.create();
chart.getPrimaryOrthogonalAxis(chart.getPrimaryBaseAxes()[0]).getSeriesDefinitions().add(valueDefinition);
valueDefinition.getSeries().add(series);
```

`chart` is a `ChartWithAxes`. `PiecewiseConstantSeriesImpl.create()` sets `StepMode` to `After`
explicitly, with the usual BIRT line series defaults. `createDefault()` leaves the step mode unset,
and such a series serializes without a `<StepMode>` element.

The standalone chart engine (`config.setProperty(PluginSettings.PROP_STANDALONE, "true")`) skips
`Platform.startup`, and it resolves each renderer from hard-coded arrays. In that engine, call
`PiecewiseConstantSetup.registerStandalone()` one time, after the `PluginSettings` singleton exists
and before the chart engine builds the first chart. If nothing calls
`PiecewiseConstantSetup.registerStandalone()`, then `PluginSettings` logs `(STANDALONE-ENV) Could
not find series renderer impl for ...PiecewiseConstantSeriesImpl`, `Generator.build` throws a
`ChartException`, and the engine writes no image. The method is idempotent, and it is harmless
under the OSGi runtime and the POJO runtime.

The chart serializer (`SerializerImpl`) caches the chart model packages in a static initializer.
Start the platform, or call `registerStandalone()`, before the JVM loads the serializer. Otherwise
`SerializerImpl.read` throws `PackageNotFoundException` for the namespace of this plug-in.

### Behaviour

- Category axis: the treads go from centre to centre. A `Center` step is on the category boundary.
- Missing values: with `connectMissingValue = true` (the default) the line steps across the gap.
  With `false` the run breaks, and BIRT draws the isolated points as markers.
- Two equal neighbours form one tread and produce no corner.
- The renderer ignores `curve = true`. A piecewise constant line has no spline.

## License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/) (SPDX: `EPL-2.0`).
