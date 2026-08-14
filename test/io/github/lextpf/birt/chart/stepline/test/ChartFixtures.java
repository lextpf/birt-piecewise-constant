/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline.test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.AxisType;
import org.eclipse.birt.chart.model.attribute.ChartDimension;
import org.eclipse.birt.chart.model.attribute.ColorDefinition;
import org.eclipse.birt.chart.model.attribute.Fill;
import org.eclipse.birt.chart.model.attribute.FontDefinition;
import org.eclipse.birt.chart.model.attribute.IntersectionType;
import org.eclipse.birt.chart.model.attribute.LineAttributes;
import org.eclipse.birt.chart.model.attribute.LineStyle;
import org.eclipse.birt.chart.model.attribute.Marker;
import org.eclipse.birt.chart.model.attribute.MarkerType;
import org.eclipse.birt.chart.model.attribute.Orientation;
import org.eclipse.birt.chart.model.attribute.Position;
import org.eclipse.birt.chart.model.attribute.Text;
import org.eclipse.birt.chart.model.attribute.TickStyle;
import org.eclipse.birt.chart.model.attribute.impl.ColorDefinitionImpl;
import org.eclipse.birt.chart.model.component.Axis;
import org.eclipse.birt.chart.model.component.Label;
import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.component.impl.AxisImpl;
import org.eclipse.birt.chart.model.component.impl.SeriesImpl;
import org.eclipse.birt.chart.model.data.NumberDataSet;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.TextDataSet;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.data.impl.SeriesDefinitionImpl;
import org.eclipse.birt.chart.model.data.impl.TextDataSetImpl;
import org.eclipse.birt.chart.model.impl.ChartWithAxesImpl;
import org.eclipse.birt.chart.model.layout.Legend;
import org.eclipse.birt.chart.model.type.LineSeries;
import org.eclipse.birt.chart.model.type.impl.LineSeriesImpl;

import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;
import io.github.lextpf.birt.chart.stepline.model.type.impl.StepLineSeriesImpl;

/**
 * Builds the chart models the tests run against.
 * <p>
 * The step chart and the stock line chart are built by the very same code, only
 * the value series differs. That is what makes the differential oracle of the
 * geometry tests sound: with identical options both charts run through
 * identical layout math, so the device coordinates of the original data points
 * are bit-for-bit equal and the step chart's extra vertices can be predicted
 * exactly.
 * <p>
 * Every chart built here carries the same theme (see
 * {@link #applyTheme(ChartWithAxes, Options)}), so the PNGs written to
 * <code>build/test-output</code> can be looked at on a light and on a dark page
 * alike. The theme is a colour, font and thickness choice; it changes no
 * position that the two charts do not share, because it is applied to both of
 * them from this one place. That is the only property the geometry oracle
 * needs - it never compares against absolute coordinates, only the step chart
 * against its stock counterpart.
 */
public final class ChartFixtures {

	/** Category labels; the first {@code values.length} of them are used. */
	private static final String[] CATEGORIES = { "A", "B", "C", "D", "E" };

	private static final Double[] DEFAULT_VALUES = { 12.5, 19.6, 18.3, 13.2, 26.5 };

	/**
	 * The ink of the theme - <code>#7f7f7f</code>. Everything BIRT would otherwise
	 * paint black (captions, axis lines, ticks) is painted in it: dark enough to
	 * read on white, light enough to read on black.
	 */
	private static final int INK_CHANNEL = 127;

	/**
	 * The alpha of the major grid lines, out of 255 - about 30 %, enough to guide
	 * the eye across the plot without competing with the series.
	 */
	private static final int GRID_ALPHA = 77;

	/** Series slot 1 - <code>#2a78d6</code>. */
	private static final int[] SERIES_BLUE = { 42, 120, 214 };

	/** Series slot 2 - <code>#d95926</code>. */
	private static final int[] SERIES_ORANGE = { 217, 89, 38 };

	/**
	 * A Java logical font name, so the JDK maps it to whatever grotesque the
	 * platform has (Arial or Segoe UI on Windows) instead of falling back to
	 * something with serifs when the named family is missing.
	 */
	private static final String FONT_NAME = "SansSerif";

	private static final float TITLE_FONT_SIZE = 14;

	/** Axis labels and legend entries. */
	private static final float LABEL_FONT_SIZE = 11;

	private static final float DATA_LABEL_FONT_SIZE = 10;

	/**
	 * What every stroke width below is multiplied by.
	 * <p>
	 * A {@code LineAttributes} thickness is device pixels, not points, and nothing
	 * scales it on the way to the device - unlike the font size and the marker
	 * size, which the display server and {@code MarkerRenderer} multiply by
	 * {@code dpi / 72} themselves - so a width meant as "n pixels in the 1x
	 * design" has to be multiplied here to keep its proportion in the export.
	 */
	private static final int STROKE_SCALE = CapturingPngRenderer.EXPORT_DPI / 72;

	/** The stroke width of a value series - two pixels in the 1x design. */
	private static final int SERIES_LINE_THICKNESS = 2 * STROKE_SCALE;

	/**
	 * The stroke width of the axis lines, their tick marks and the grid lines -
	 * one pixel in the 1x design.
	 */
	private static final int HAIRLINE_THICKNESS = 1 * STROKE_SCALE;

	/**
	 * The data point marker size, in points.
	 * <p>
	 * {@code MarkerRenderer} treats it as a <em>radius</em>: it computes
	 * {@code iSize = size * dpi / 72} and then draws the marker from
	 * {@code location - iSize} to {@code location + iSize}, so the dot's diameter
	 * is twice this value. Four points therefore is an 8 px dot at 1x, and scales
	 * with the export resolution - unlike line thickness, which
	 * {@code Generator.render} scales by hand, the marker scales inside the
	 * renderer.
	 */
	private static final int MARKER_SIZE = 4;

	private ChartFixtures() {
	}

	/**
	 * @return a fresh copy of the default value series {12.5, 19.6, 18.3, 13.2,
	 *         26.5}.
	 */
	public static Double[] defaultValues() {
		return DEFAULT_VALUES.clone();
	}

	/**
	 * Builds a category/value chart whose single value series is a
	 * {@link StepLineSeries}.
	 *
	 * @param mode       the step mode to set on the value series
	 * @param transposed whether the chart axes are transposed (horizontal chart)
	 * @param values     the orthogonal values; {@code null} entries mean missing
	 *                   data
	 * @return the chart
	 */
	public static ChartWithAxes stepLineChart(StepMode mode, boolean transposed, Double[] values) {
		return stepLineChart(mode, values, new Options().transposed(transposed));
	}

	/**
	 * Builds the chart {@link #stepLineChart(StepMode, boolean, Double[])} builds,
	 * but with a stock {@link LineSeries} value series - the reference rendering
	 * the step line is compared against.
	 *
	 * @param transposed whether the chart axes are transposed (horizontal chart)
	 * @param values     the orthogonal values; {@code null} entries mean missing
	 *                   data
	 * @return the chart
	 */
	public static ChartWithAxes lineChart(boolean transposed, Double[] values) {
		return lineChart(values, new Options().transposed(transposed));
	}

	/**
	 * Builds a category/value chart whose value series are {@link StepLineSeries}.
	 *
	 * @param mode    the step mode to set on every value series
	 * @param values  the orthogonal values of the first series; {@code null}
	 *                entries mean missing data
	 * @param options the chart and series options
	 * @return the chart
	 */
	public static ChartWithAxes stepLineChart(StepMode mode, Double[] values, Options options) {
		return build(() -> {
			StepLineSeries series = (StepLineSeries) StepLineSeriesImpl.create();
			series.setStepMode(mode);
			return series;
		}, values, options);
	}

	/**
	 * Builds the chart {@link #stepLineChart(StepMode, Double[], Options)} builds,
	 * but with stock {@link LineSeries} value series - the reference rendering the
	 * step line is compared against.
	 *
	 * @param values  the orthogonal values of the first series; {@code null}
	 *                entries mean missing data
	 * @param options the chart and series options
	 * @return the chart
	 */
	public static ChartWithAxes lineChart(Double[] values, Options options) {
		return build(() -> (LineSeries) LineSeriesImpl.create(), values, options);
	}

	/**
	 * @param chart a chart built by this class
	 * @return the value series of the chart, in the order they were added
	 */
	public static List<Series> valueSeries(ChartWithAxes chart) {
		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		return chart.getPrimaryOrthogonalAxis(xAxis).getSeriesDefinitions().get(0).getSeries();
	}

	private static ChartWithAxes build(Supplier<LineSeries> valueSeriesFactory, Double[] values, Options options) {
		if (values.length > CATEGORIES.length) {
			throw new IllegalArgumentException(
					"at most " + CATEGORIES.length + " values are supported, got " + values.length);
		}

		ChartWithAxes chart = ChartWithAxesImpl.create();
		chart.setTransposed(options.transposed);

		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		xAxis.setType(AxisType.TEXT_LITERAL);
		xAxis.getMajorGrid().setTickStyle(TickStyle.BELOW_LITERAL);
		xAxis.getOrigin().setType(IntersectionType.VALUE_LITERAL);

		Axis yAxis = chart.getPrimaryOrthogonalAxis(xAxis);

		// Only touch an optional feature when it differs from the model default, so
		// that the plain fixture keeps BIRT's own defaults for everything the tests
		// do not exercise.
		if (options.dimension != ChartDimension.TWO_DIMENSIONAL_LITERAL) {
			chart.setDimension(options.dimension);
		}
		if (options.percent) {
			yAxis.setPercent(true);
		}
		if (options.dimension == ChartDimension.THREE_DIMENSIONAL_LITERAL) {
			addAncillaryAxis(xAxis);
		}

		TextDataSet categoryData = TextDataSetImpl.create(Arrays.copyOf(CATEGORIES, values.length));

		Series baseSeries = SeriesImpl.create();
		baseSeries.setDataSet(categoryData);
		SeriesDefinition baseDefinition = SeriesDefinitionImpl.create();
		xAxis.getSeriesDefinitions().add(baseDefinition);
		baseDefinition.getSeries().add(baseSeries);

		SeriesDefinition valueDefinition = SeriesDefinitionImpl.create();
		yAxis.getSeriesDefinitions().add(valueDefinition);

		valueDefinition.getSeries().add(configure(valueSeriesFactory.get(), "step", values, options));
		if (options.secondValues != null) {
			valueDefinition.getSeries()
					.add(configure(valueSeriesFactory.get(), "step2", options.secondValues, options));
		}

		applyPalette(valueDefinition);
		applyTheme(chart, options);

		return chart;
	}

	/**
	 * Pins the colours of the value series instead of letting BIRT fill the empty
	 * palette of a fresh series definition with its own default one.
	 * <p>
	 * {@code Line.renderSeries} looks the fill up as
	 * {@code palette.getEntries().get(seriesIndex % size)}, so the first entry is
	 * the colour of the first series and the second of the second. Two entries is
	 * all the fixtures need - they never build a third value series - and adding
	 * no more makes a third one fail loudly by reusing blue rather than quietly
	 * picking an unreviewed colour.
	 *
	 * @param valueDefinition the series definition holding the value series
	 */
	private static void applyPalette(SeriesDefinition valueDefinition) {
		List<Fill> entries = valueDefinition.getSeriesPalette().getEntries();
		entries.clear();
		entries.add(color(SERIES_BLUE));
		entries.add(color(SERIES_ORANGE));
	}

	/**
	 * Paints the chart so that it is readable on a light and on a dark background:
	 * every block background stays transparent - the PNG device writes ARGB, so
	 * whatever the page is shows through - and everything BIRT would draw in black
	 * (the title, the legend text, the axis lines, their labels and grid ticks and
	 * the data point labels, the last in {@link #configure}) becomes the mid grey
	 * ink. The series themselves take the fixed palette of
	 * {@link #applyPalette(SeriesDefinition)}, whose blue and orange are saturated
	 * enough to hold against both grounds.
	 * <p>
	 * This runs for the step chart and for the stock line chart alike, so the two
	 * stay identical in everything but the series type - which is what makes the
	 * differential geometry oracle sound.
	 *
	 * @param chart   the chart to theme
	 * @param options the options the chart was built with
	 */
	private static void applyTheme(ChartWithAxes chart, Options options) {
		chart.getBlock().setBackground(transparent());
		chart.getBlock().getOutline().setVisible(false);
		chart.getPlot().setBackground(transparent());
		chart.getPlot().getOutline().setVisible(false);
		chart.getPlot().getClientArea().setBackground(transparent());
		chart.getPlot().getClientArea().getOutline().setVisible(false);

		chart.getTitle().setBackground(transparent());
		chart.getTitle().getOutline().setVisible(false);
		Label title = chart.getTitle().getLabel();
		title.getCaption().setValue(options.title);
		themeText(title.getCaption(), TITLE_FONT_SIZE, true);

		themeLegend(chart.getLegend(), options);

		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		// Gridlines belong to the value axis only: they carry the reading of a
		// magnitude across the plot, whereas one per category would just fence the
		// data points in.
		themeAxis(xAxis, false);
		themeAxis(chart.getPrimaryOrthogonalAxis(xAxis), true);
		// The ancillary (depth) axis only exists on a three dimensional chart.
		for (Axis ancillary : xAxis.getAncillaryAxes()) {
			themeAxis(ancillary, false);
		}
	}

	/**
	 * Shows the legend exactly when it says something - with a single value series
	 * it would only repeat the title - and strips it down to its text: no box, no
	 * background, no separators.
	 *
	 * @param legend  the legend to theme
	 * @param options the options the chart was built with
	 */
	private static void themeLegend(Legend legend, Options options) {
		legend.setVisible(options.secondValues != null);
		legend.setPosition(Position.RIGHT_LITERAL);
		legend.setBackground(transparent());
		legend.getOutline().setVisible(false);
		legend.getClientArea().setBackground(transparent());
		legend.getClientArea().getOutline().setVisible(false);
		legend.getSeparator().setVisible(false);
		themeText(legend.getText(), LABEL_FONT_SIZE, false);
	}

	/**
	 * Recolours one axis - its line, its labels, its major grid ticks and, if it is
	 * shown at all, its title - and turns its major grid lines on or off.
	 *
	 * @param axis      the axis to theme
	 * @param gridLines whether the axis draws major grid lines across the plot
	 */
	private static void themeAxis(Axis axis, boolean gridLines) {
		axis.getLineAttributes().setColor(ink());
		axis.getLineAttributes().setThickness(HAIRLINE_THICKNESS);
		themeText(axis.getLabel().getCaption(), LABEL_FONT_SIZE, false);
		axis.getMajorGrid().getTickAttributes().setColor(ink());
		axis.getMajorGrid().getTickAttributes().setThickness(HAIRLINE_THICKNESS);

		LineAttributes grid = axis.getMajorGrid().getLineAttributes();
		grid.setVisible(gridLines);
		if (gridLines) {
			grid.setStyle(LineStyle.SOLID_LITERAL);
			grid.setThickness(HAIRLINE_THICKNESS);
			ColorDefinition gridInk = ink();
			gridInk.setTransparency(GRID_ALPHA);
			grid.setColor(gridInk);
		}

		if (axis.getTitle().isVisible()) {
			themeText(axis.getTitle().getCaption(), LABEL_FONT_SIZE, false);
		}
	}

	/**
	 * Puts one run of text into the theme: ink coloured, in the theme's font
	 * family at the given size.
	 *
	 * @param text the text to theme
	 * @param size the font size in points
	 * @param bold whether the text is bold
	 */
	private static void themeText(Text text, float size, boolean bold) {
		text.setColor(ink());
		FontDefinition font = text.getFont();
		font.setName(FONT_NAME);
		font.setSize(size);
		font.setBold(bold);
	}

	/**
	 * @return a fresh ink grey; every call returns its own instance because an EMF
	 *         containment reference cannot hold the same object twice
	 */
	private static ColorDefinition ink() {
		return ColorDefinitionImpl.create(INK_CHANNEL, INK_CHANNEL, INK_CHANNEL);
	}

	/**
	 * @param rgb the three channels of a colour
	 * @return a fresh colour; every call returns its own instance because an EMF
	 *         containment reference cannot hold the same object twice
	 */
	private static ColorDefinition color(int[] rgb) {
		return ColorDefinitionImpl.create(rgb[0], rgb[1], rgb[2]);
	}

	/**
	 * @return a fresh fully transparent colour; every call returns its own instance
	 *         because an EMF containment reference cannot hold the same object
	 *         twice
	 */
	private static ColorDefinition transparent() {
		return ColorDefinitionImpl.TRANSPARENT();
	}

	private static LineSeries configure(LineSeries series, String identifier, Double[] values, Options options) {
		NumberDataSet valueData = NumberDataSetImpl.create(values);
		series.setDataSet(valueData);
		series.setSeriesIdentifier(identifier);
		if (options.stacked) {
			series.setStacked(true);
		}
		if (options.curve) {
			series.setCurve(true);
		}
		if (!options.connectMissingValue) {
			series.setConnectMissingValue(false);
		}
		if (options.labelsVisible) {
			series.getLabel().setVisible(true);
			themeText(series.getLabel().getCaption(), DATA_LABEL_FONT_SIZE, false);
		}
		if (options.shadowColor != null) {
			series.setShadowColor(options.shadowColor);
		}
		// The drawn series line takes the palette colour, not this one:
		// LineSeries.isPaletteLineColor() is true by default, so Line's
		// DataPointsRenderer copies the line attributes and overrides the colour
		// with the palette entry. What is set here is the thickness - and the ink
		// colour, which is what a marker outline would be drawn in: Line passes the
		// series' own line attributes to BaseRenderer.renderMarker, and
		// MarkerRenderer copies those and takes only the visibility flag from
		// Marker.getOutline(). The outline is switched off below, so the ink never
		// actually reaches the device; it stays as the sane fallback should a test
		// ever turn the outline back on.
		series.getLineAttributes().setColor(ink());
		series.getLineAttributes().setThickness(SERIES_LINE_THICKNESS);

		// LineSeriesImpl.initialize() installs one visible BOX marker with a visible
		// outline. A filled circle with no outline reads as a data point on any
		// ground, because the fill is the palette colour of its own series.
		Marker marker = series.getMarkers().get(0);
		marker.setType(MarkerType.CIRCLE_LITERAL);
		marker.setSize(MARKER_SIZE);
		marker.getOutline().setVisible(false);
		// The geometry tests hide the marker so that it adds neither ovals nor
		// layout padding.
		marker.setVisible(options.markersVisible);
		return series;
	}

	/**
	 * Adds the ancillary (depth) base axis a three dimensional chart with axes
	 * needs.
	 */
	private static void addAncillaryAxis(Axis xAxis) {
		Axis zAxis = AxisImpl.create(Axis.ANCILLARY_BASE);
		zAxis.setType(AxisType.TEXT_LITERAL);
		zAxis.setLabelPosition(Position.BELOW_LITERAL);
		zAxis.setTitlePosition(Position.BELOW_LITERAL);
		zAxis.getMajorGrid().setTickStyle(TickStyle.BELOW_LITERAL);
		zAxis.setOrientation(Orientation.HORIZONTAL_LITERAL);
		xAxis.getAncillaryAxes().add(zAxis);
		zAxis.getSeriesDefinitions().add(SeriesDefinitionImpl.create());
	}

	/**
	 * The knobs the render tests turn. The defaults reproduce the chart
	 * {@link ChartFixtures#stepLineChart(StepMode, boolean, Double[])} has always
	 * built: a plain two dimensional chart with visible markers and no labels.
	 */
	public static final class Options {

		private boolean transposed;

		private ChartDimension dimension = ChartDimension.TWO_DIMENSIONAL_LITERAL;

		private boolean curve;

		private boolean stacked;

		private boolean percent;

		/** The model default of {@code LineSeries.connectMissingValue} is true. */
		private boolean connectMissingValue = true;

		private boolean markersVisible = true;

		private boolean labelsVisible;

		private ColorDefinition shadowColor;

		private Double[] secondValues;

		private String title = "Step line";

		/**
		 * @param value whether the chart axes are transposed (horizontal chart)
		 * @return this
		 */
		public Options transposed(boolean value) {
			this.transposed = value;
			return this;
		}

		/**
		 * @return whether the chart axes are transposed
		 */
		public boolean isTransposed() {
			return transposed;
		}

		/**
		 * @param value the chart dimension
		 * @return this
		 */
		public Options dimension(ChartDimension value) {
			this.dimension = value;
			return this;
		}

		/**
		 * @param value whether the value series are drawn as curves
		 * @return this
		 */
		public Options curve(boolean value) {
			this.curve = value;
			return this;
		}

		/**
		 * @param value whether the value series are stacked
		 * @return this
		 */
		public Options stacked(boolean value) {
			this.stacked = value;
			return this;
		}

		/**
		 * @param value whether the orthogonal axis shows percentages
		 * @return this
		 */
		public Options percent(boolean value) {
			this.percent = value;
			return this;
		}

		/**
		 * @param value whether the renderer bridges missing values instead of breaking
		 *              the line
		 * @return this
		 */
		public Options connectMissingValue(boolean value) {
			this.connectMissingValue = value;
			return this;
		}

		/**
		 * @param value whether the data point markers are drawn
		 * @return this
		 */
		public Options markersVisible(boolean value) {
			this.markersVisible = value;
			return this;
		}

		/**
		 * @param value whether the data point labels are drawn
		 * @return this
		 */
		public Options labelsVisible(boolean value) {
			this.labelsVisible = value;
			return this;
		}

		/**
		 * @param value the shadow colour of the value series, or <code>null</code> for
		 *              no shadow
		 * @return this
		 */
		public Options shadowColor(ColorDefinition value) {
			this.shadowColor = value;
			return this;
		}

		/**
		 * @param value the orthogonal values of a second value series added to the same
		 *              axis, or <code>null</code> for a single series
		 * @return this
		 */
		public Options secondValues(Double[] value) {
			this.secondValues = value == null ? null : value.clone();
			return this;
		}

		/**
		 * Sets the chart title. A comparison must use the same options - and so the
		 * same title - for the step chart and for its stock oracle, or the two would
		 * reserve different amounts of room for the title block and their layouts
		 * would no longer line up.
		 *
		 * @param value the title caption
		 * @return this
		 */
		public Options title(String value) {
			this.title = value;
			return this;
		}
	}
}
