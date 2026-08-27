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
import org.eclipse.birt.chart.model.attribute.IntersectionType;
import org.eclipse.birt.chart.model.attribute.Orientation;
import org.eclipse.birt.chart.model.attribute.Position;
import org.eclipse.birt.chart.model.attribute.TickStyle;
import org.eclipse.birt.chart.model.attribute.impl.ColorDefinitionImpl;
import org.eclipse.birt.chart.model.component.Axis;
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
 * Every chart built here carries the same neutral theme (see
 * {@link #applyTheme(ChartWithAxes)}), so the PNGs written to
 * <code>build/test-output</code> can be looked at on a light and on a dark page
 * alike. The theme is purely a colour choice - it changes no size, no position
 * and no visibility, so the geometry oracle is unaffected.
 */
public final class ChartFixtures {

	/** Category labels; the first {@code values.length} of them are used. */
	private static final String[] CATEGORIES = { "A", "B", "C", "D", "E" };

	private static final Double[] DEFAULT_VALUES = { 12.5, 19.6, 18.3, 13.2, 26.5 };

	/**
	 * The grey everything BIRT would otherwise paint black is painted in: dark
	 * enough to read on white, light enough to read on black.
	 */
	private static final int NEUTRAL_GREY = 128;

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
		chart.getTitle().getLabel().getCaption().setValue("Step line");

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

		applyTheme(chart);

		return chart;
	}

	/**
	 * Paints the chart so that it is readable on a light and on a dark background:
	 * every block background stays transparent - the PNG device writes ARGB, so
	 * whatever the page is shows through - and everything BIRT would draw in black
	 * (the title, the legend text and outline, the axis lines, their labels and
	 * grid ticks, the data point labels and the data point marker outlines - the
	 * last two in {@link #configure}) becomes a mid grey. The series palette and
	 * the marker fill keep BIRT's defaults; those are saturated mid tones and read
	 * on both.
	 * <p>
	 * This runs for the step chart and for the stock line chart alike, so the two
	 * stay identical in everything but the series type - which is what makes the
	 * differential geometry oracle sound. Colours carry no layout information, so
	 * no device coordinate moves.
	 *
	 * @param chart the chart to theme
	 */
	private static void applyTheme(ChartWithAxes chart) {
		chart.getBlock().setBackground(transparent());
		chart.getPlot().setBackground(transparent());
		chart.getPlot().getClientArea().setBackground(transparent());
		chart.getLegend().setBackground(transparent());
		chart.getLegend().getClientArea().setBackground(transparent());
		chart.getTitle().setBackground(transparent());

		chart.getTitle().getLabel().getCaption().setColor(neutral());
		chart.getLegend().getText().setColor(neutral());
		chart.getLegend().getOutline().setColor(neutral());

		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		themeAxis(xAxis);
		themeAxis(chart.getPrimaryOrthogonalAxis(xAxis));
		// The ancillary (depth) axis only exists on a three dimensional chart.
		for (Axis ancillary : xAxis.getAncillaryAxes()) {
			themeAxis(ancillary);
		}
	}

	/**
	 * Recolours one axis - its line, its labels, its major grid ticks and, if it is
	 * shown at all, its title.
	 *
	 * @param axis the axis to theme
	 */
	private static void themeAxis(Axis axis) {
		axis.getLineAttributes().setColor(neutral());
		axis.getLabel().getCaption().setColor(neutral());
		axis.getMajorGrid().getTickAttributes().setColor(neutral());
		if (axis.getTitle().isVisible()) {
			axis.getTitle().getCaption().setColor(neutral());
		}
	}

	/**
	 * @return a fresh mid grey; every call returns its own instance because an EMF
	 *         containment reference cannot hold the same object twice
	 */
	private static ColorDefinition neutral() {
		return ColorDefinitionImpl.create(NEUTRAL_GREY, NEUTRAL_GREY, NEUTRAL_GREY);
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
			series.getLabel().getCaption().setColor(neutral());
		}
		if (options.shadowColor != null) {
			series.setShadowColor(options.shadowColor);
		}
		// The default BOX marker is drawn with a black outline around its palette
		// coloured fill, which vanishes on a dark page. The outline colour is *not*
		// Marker.getOutline().getColor(): Line.renderSeries passes the series' own
		// line attributes to BaseRenderer.renderMarker, and MarkerRenderer copies
		// those and takes only the visibility flag from Marker.getOutline(). The
		// drawn series line is unaffected - LineSeries.isPaletteLineColor() is true
		// by default, so Line's DataPointsRenderer copies the line attributes and
		// overrides the colour with the palette entry. Marker fill and visibility
		// stay BIRT's.
		series.getLineAttributes().setColor(neutral());
		if (!options.markersVisible) {
			// LineSeriesImpl.initialize() installs one visible BOX marker; the geometry
			// tests hide it so that markers add neither ovals nor layout padding.
			series.getMarkers().get(0).setVisible(false);
		}
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
	}
}
