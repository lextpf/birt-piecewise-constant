/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant.test;

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

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl;

/**
 * Builds the chart models that the tests render.
 * <p>
 * Intent: one code path builds the piecewise constant chart and the reference
 * chart. Only the type of the value series and the default caption differ.
 * <p>
 * Non-obvious behaviour: identical options make both charts run through
 * identical layout calculations. The device coordinates of the real data points
 * are therefore equal bit for bit. The geometry tests can then predict the
 * corner vertices of the piecewise constant chart exactly.
 */
public final class ChartFixtures {

	/** The category labels. A chart uses the first {@code values.length} of them. */
	private static final String[] CATEGORIES = { "A", "B", "C", "D", "E" };

	private static final Double[] DEFAULT_VALUES = { 12.5, 19.6, 18.3, 13.2, 26.5 };

	/**
	 * The caption of a piecewise constant chart. {@link Options#title(String)}
	 * replaces it.
	 */
	static final String PIECEWISE_CONSTANT_TITLE = "Piecewise constant";

	/** The caption of the reference chart. {@link Options#title(String)} replaces it. */
	static final String LINE_ORACLE_TITLE = "Linear interpolation (oracle)";

	/** The grey value of the theme ink, <code>#7f7f7f</code>. */
	private static final int INK_CHANNEL = 127;

	/** The alpha value of the major grid lines, out of 255. */
	private static final int GRID_ALPHA = 77;

	/** The colour of the first value series, <code>#2a78d6</code>. */
	private static final int[] SERIES_BLUE = { 42, 120, 214 };

	/** The colour of the second value series, <code>#d95926</code>. */
	private static final int[] SERIES_ORANGE = { 217, 89, 38 };

	/** A Java logical font name. */
	private static final String FONT_NAME = "SansSerif";

	private static final float TITLE_FONT_SIZE = 14;

	private static final float LABEL_FONT_SIZE = 11;

	private static final float DATA_LABEL_FONT_SIZE = 10;

	/**
	 * The factor that this class applies to every stroke width below.
	 *
	 * @see CapturingPngRenderer#EXPORT_DPI
	 */
	private static final int STROKE_SCALE = CapturingPngRenderer.EXPORT_DPI / 72;

	/** The stroke width of a value series. The design states two pixels at 1x. */
	private static final int SERIES_LINE_THICKNESS = 2 * STROKE_SCALE;

	/** The stroke width of the axis lines, of their tick marks and of the grid lines. */
	private static final int HAIRLINE_THICKNESS = 1 * STROKE_SCALE;

	/**
	 * The size of a data point marker, in points.
	 * <p>
	 * Non-obvious behaviour: {@code MarkerRenderer} reads this size as a radius.
	 * It computes {@code iSize = size * dpi / 72} and draws the marker from
	 * {@code location - iSize} to {@code location + iSize}. The diameter of the
	 * dot is therefore twice this value, that is 8 pixels at 1x. The marker scales
	 * with the export resolution inside {@code MarkerRenderer}. The line thickness
	 * that a series renderer reads does not scale.
	 */
	private static final int MARKER_SIZE = 4;

	private ChartFixtures() {
	}

	public static Double[] defaultValues() {
		return DEFAULT_VALUES.clone();
	}

	/** Builds a chart with a category axis and a value axis. */
	public static ChartWithAxes piecewiseConstantChart(StepMode mode, boolean transposed, Double[] values) {
		return piecewiseConstantChart(mode, values, new Options().transposed(transposed));
	}

	/**
	 * @see #lineChart(Double[], Options)
	 */
	public static ChartWithAxes lineChart(boolean transposed, Double[] values) {
		return lineChart(values, new Options().transposed(transposed));
	}

	/**
	 * Builds a chart with a category axis and a value axis. Every value series of
	 * the chart is a {@link PiecewiseConstantSeries}.
	 */
	public static ChartWithAxes piecewiseConstantChart(StepMode mode, Double[] values, Options options) {
		return build(() -> {
			PiecewiseConstantSeries series = (PiecewiseConstantSeries) PiecewiseConstantSeriesImpl.create();
			series.setStepMode(mode);
			return series;
		}, values, options, PIECEWISE_CONSTANT_TITLE);
	}

	/**
	 * Builds the reference chart of
	 * {@link #piecewiseConstantChart(StepMode, Double[], Options)}: the same
	 * chart, but with stock {@link LineSeries} value series. The geometry tests
	 * compare the piecewise constant chart with this chart.
	 */
	public static ChartWithAxes lineChart(Double[] values, Options options) {
		return build(() -> (LineSeries) LineSeriesImpl.create(), values, options, LINE_ORACLE_TITLE);
	}

	/** @return the value series of the chart, in the order in which this class added them */
	public static List<Series> valueSeries(ChartWithAxes chart) {
		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		return chart.getPrimaryOrthogonalAxis(xAxis).getSeriesDefinitions().get(0).getSeries();
	}

	/** Builds one chart of either kind. */
	private static ChartWithAxes build(Supplier<LineSeries> valueSeriesFactory, Double[] values, Options options,
			String defaultTitle) {
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

		// This class writes an optional feature only if the value differs from the
		// model default. The plain fixture therefore keeps the chart engine defaults
		// for every feature that the tests do not exercise.
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

		valueDefinition.getSeries().add(configure(valueSeriesFactory.get(), "series1", values, options));
		if (options.secondValues != null) {
			valueDefinition.getSeries()
					.add(configure(valueSeriesFactory.get(), "series2", options.secondValues, options));
		}

		applyPalette(valueDefinition);
		applyTheme(chart, options, options.title != null ? options.title : defaultTitle);

		return chart;
	}

	/**
	 * Sets the colours of the value series.
	 * <p>
	 * Non-obvious behaviour: {@code Line.renderSeries} reads the fill as
	 * {@code palette.getEntries().get(seriesIndex % size)}. The first entry is
	 * therefore the colour of the first value series, and the second entry is the
	 * colour of the second. The fixtures build at most two value series.
	 */
	private static void applyPalette(SeriesDefinition valueDefinition) {
		List<Fill> entries = valueDefinition.getSeriesPalette().getEntries();
		entries.clear();
		entries.add(color(SERIES_BLUE));
		entries.add(color(SERIES_ORANGE));
	}

	/**
	 * Paints the chart so that it is readable on a light page and on a dark page.
	 */
	private static void applyTheme(ChartWithAxes chart, Options options, String caption) {
		chart.getBlock().setBackground(transparent());
		chart.getBlock().getOutline().setVisible(false);
		chart.getPlot().setBackground(transparent());
		chart.getPlot().getOutline().setVisible(false);
		chart.getPlot().getClientArea().setBackground(transparent());
		chart.getPlot().getClientArea().getOutline().setVisible(false);

		chart.getTitle().setBackground(transparent());
		chart.getTitle().getOutline().setVisible(false);
		Label title = chart.getTitle().getLabel();
		title.getCaption().setValue(caption);
		themeText(title.getCaption(), TITLE_FONT_SIZE, true);

		themeLegend(chart.getLegend(), options);

		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		// Only the value axis draws grid lines. A grid line carries a value across
		// the plot. One grid line per category adds no reading and only fences the
		// data points in.
		themeAxis(xAxis, false);
		themeAxis(chart.getPrimaryOrthogonalAxis(xAxis), true);
		// The ancillary (depth) axis exists only on a three dimensional chart.
		for (Axis ancillary : xAxis.getAncillaryAxes()) {
			themeAxis(ancillary, false);
		}
	}

	/**
	 * Shows the legend only if the chart has two value series, and reduces the
	 * legend to its text: no box, no background and no separator.
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

	/** Paints one axis in the theme ink: its line, its labels and its major grid ticks. */
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

	private static void themeText(Text text, float size, boolean bold) {
		text.setColor(ink());
		FontDefinition font = text.getFont();
		font.setName(FONT_NAME);
		font.setSize(size);
		font.setBold(bold);
	}

	/**
	 * @return a new ink grey. Every call returns its own instance, because an EMF
	 *         containment reference cannot contain the same object twice.
	 */
	private static ColorDefinition ink() {
		return ColorDefinitionImpl.create(INK_CHANNEL, INK_CHANNEL, INK_CHANNEL);
	}

	private static ColorDefinition color(int[] rgb) {
		return ColorDefinitionImpl.create(rgb[0], rgb[1], rgb[2]);
	}

	private static ColorDefinition transparent() {
		return ColorDefinitionImpl.TRANSPARENT();
	}

	/**
	 * @param values     the values on the value axis; a {@code null} entry is a
	 *                   missing value
	 */
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
		// The drawn line of the value series takes the palette colour and not the
		// colour set here. LineSeries.isPaletteLineColor() is true by default, so the
		// DataPointsRenderer of Line copies these line attributes and replaces the
		// colour with the palette entry. The thickness set here does reach the
		// device. The ink colour is the colour of a marker outline: Line gives the
		// line attributes of the series to BaseRenderer.renderMarker, and
		// MarkerRenderer copies them and takes only the visibility flag from
		// Marker.getOutline(). The code below makes that outline invisible, so the
		// ink never reaches the device.
		series.getLineAttributes().setColor(ink());
		series.getLineAttributes().setThickness(SERIES_LINE_THICKNESS);

		// LineSeriesImpl.initialize() installs one visible BOX marker with a visible
		// outline. A filled circle without an outline reads as a data point on a
		// light page and on a dark page, because the fill is the palette colour of
		// the series.
		Marker marker = series.getMarkers().get(0);
		marker.setType(MarkerType.CIRCLE_LITERAL);
		marker.setSize(MARKER_SIZE);
		marker.getOutline().setVisible(false);
		// The geometry tests make the marker invisible. An invisible marker adds no
		// oval and no layout padding.
		marker.setVisible(options.markersVisible);
		return series;
	}

	/** Adds the ancillary (depth) axis that a three dimensional chart with axes needs. */
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

	/** The options that the render tests set. */
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

		/**
		 * The caption that the caller asked for. The value is <code>null</code> for
		 * the default caption of the series type; see {@link #title(String)}.
		 */
		private String title;

		public Options transposed(boolean value) {
			this.transposed = value;
			return this;
		}

		public boolean isTransposed() {
			return transposed;
		}

		public Options dimension(ChartDimension value) {
			this.dimension = value;
			return this;
		}

		public Options curve(boolean value) {
			this.curve = value;
			return this;
		}

		public Options stacked(boolean value) {
			this.stacked = value;
			return this;
		}

		public Options percent(boolean value) {
			this.percent = value;
			return this;
		}

		/**
		 * @param value {@code true} if the renderer bridges a missing value.
		 *              {@code false} if the renderer breaks the line at a missing
		 *              value.
		 */
		public Options connectMissingValue(boolean value) {
			this.connectMissingValue = value;
			return this;
		}

		public Options markersVisible(boolean value) {
			this.markersVisible = value;
			return this;
		}

		public Options labelsVisible(boolean value) {
			this.labelsVisible = value;
			return this;
		}

		public Options shadowColor(ColorDefinition value) {
			this.shadowColor = value;
			return this;
		}

		public Options secondValues(Double[] value) {
			this.secondValues = value == null ? null : value.clone();
			return this;
		}

		/**
		 * Sets one caption for both chart types.
		 * <p>
		 * Non-obvious behaviour: two different captions still leave the comparison of
		 * the geometry tests valid. The title block spans the chart width for every
		 * string. For one line of text the height of the block comes from the font
		 * and not from the string. The two charts therefore reserve the same room
		 * for the title, and the two plots start at the same position. The geometry
		 * tests prove that, because they compare the vertices of the two renders
		 * with exact <code>double</code> equality.
		 */
		public Options title(String value) {
			this.title = value;
			return this;
		}
	}
}
