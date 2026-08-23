/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.chart.exception.ChartException;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.ChartDimension;
import org.eclipse.birt.chart.model.attribute.impl.ColorDefinitionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;
import io.github.lextpf.birt.chart.piecewiseconstant.test.CapturingPngRenderer;
import io.github.lextpf.birt.chart.piecewiseconstant.test.CapturingPngRenderer.Seg;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartFixtures;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartFixtures.Options;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartPlatformExtension;

/**
 * Checks the geometry that {@link PiecewiseConstantLine} puts on the device.
 * <p>
 * Intent: each test renders the same chart twice, once with a stock
 * {@code LineSeries} and once with a piecewise constant series. The vertices of
 * the piecewise constant series must be the vertices of the reference chart
 * with the corner vertices inserted.
 * <p>
 * Non-obvious behaviour: the fixture builds both charts from the same options,
 * so the layout calculations are identical. The device coordinates of the real
 * data points are therefore equal bit for bit, and the tests compare them with
 * exact <code>double</code> equality.
 */
@ExtendWith(ChartPlatformExtension.class)
class PiecewiseConstantGeometryTest {

	/** Three data points on the categories A, B and C. The line makes two steps. */
	private static final Double[] VALUES = { 5.0, 9.0, 3.0 };

	/**
	 * The values of the second value series of the stacked chart and of the
	 * percent chart.
	 */
	private static final Double[] SECOND_VALUES = { 3.0, 4.0, 2.0 };

	/**
	 * @return the options that every geometry test starts from. The chart draws
	 *         the line only, without markers.
	 */
	private static Options bare() {
		return new Options().markersVisible(false);
	}

	// ------------------------------------------------------------ 1. the modes

	@Test
	void afterModePutsTheCornerAtTheBaseCoordinateOfTheRightPoint() throws Exception {
		assertStaircase(bare(), StepMode.AFTER_LITERAL, VALUES, 4, "geometry-after");
	}

	@Test
	void beforeModePutsTheCornerAtTheBaseCoordinateOfTheLeftPoint() throws Exception {
		assertStaircase(bare(), StepMode.BEFORE_LITERAL, VALUES, 4, "geometry-before");
	}

	@Test
	void centerModePutsTwoCornersOnTheMidpointBetweenThePoints() throws Exception {
		assertStaircase(bare(), StepMode.CENTER_LITERAL, VALUES, 6, "geometry-center");
	}

	// ------------------------------------------------------------ 2. transposed

	@Test
	void transposedChartsStepAlongTheDeviceYAxis() throws Exception {
		Options options = bare().transposed(true);
		Oracle oracle = assertStaircase(options, StepMode.AFTER_LITERAL, VALUES, 4, "geometry-after-transposed");

		// The category axis now runs down the device. The corner vertex of the AFTER
		// mode therefore takes its y from the right data point and its x from the
		// left one. The device draws each tread as a vertical line and each step as a
		// horizontal line. The result is the mirror image of the chart that is not
		// transposed.
		double[] p0 = oracle.stock.get(0);
		double[] p1 = oracle.stock.get(1);
		double[] corner = oracle.step.get(1);
		assertEquals(p0[0], corner[0], "the corner vertex keeps the value coordinate of the left data point");
		assertEquals(p1[1], corner[1], "the corner vertex takes the base coordinate of the right data point");
	}

	// ----------------------------------------------------------- 3./4. missing

	@Test
	void aBrokenRunDrawsIsolatedPointsAndNoLine() throws Exception {
		Options options = bare().connectMissingValue(false);
		Double[] values = { 5.0, null, 3.0 };

		CapturingPngRenderer stock = render(ChartFixtures.lineChart(values, options), "geometry-null-gap-oracle");
		CapturingPngRenderer step = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, values, options),
				"geometry-null-gap");

		assertEquals(0, stock.segments.size(), "the reference chart does not bridge the missing value either");
		assertEquals(0, step.segments.size(), "a broken run has no segment that can carry a corner vertex");
		assertTrue(stock.seriesPointOvals > 0, "the reference chart draws its isolated data points as ovals");
		assertEquals(stock.seriesPointOvals, step.seriesPointOvals,
				"the piecewise constant series draws the isolated data points that the reference chart draws");
	}

	@Test
	void aBridgedRunStepsStraightOverTheMissingPoint() throws Exception {
		Options options = bare().connectMissingValue(true);
		Double[] values = { 5.0, null, 3.0 };

		Oracle oracle = assertStaircase(options, StepMode.AFTER_LITERAL, values, 2, "geometry-null-connected");

		assertEquals(2, oracle.stock.size(), "the reference chart bridges the missing value with one segment");
	}

	// --------------------------------------------------------------- 5. flat

	@Test
	void twoEqualValuesFormAFlatStepWithoutACorner() throws Exception {
		assertStaircase(bare(), StepMode.AFTER_LITERAL, new Double[] { 5.0, 5.0, 3.0 }, 3, "geometry-flat");
	}

	// ------------------------------------------------------- 6./7. many series

	@Test
	void everyStackedSeriesFormsItsOwnStaircase() throws Exception {
		assertStaircasePerSeries(bare().stacked(true).secondValues(SECOND_VALUES), StepMode.AFTER_LITERAL,
				"geometry-stacked");
	}

	@Test
	void aPercentAxisStillProducesAStaircase() throws Exception {
		// A percent axis reads its scale from the stacked group of a series, and the
		// chart engine computes that group for stacked series only. A percent chart
		// that is not stacked fails with a null AxisSubUnit in
		// PlotWith2DAxes.getSeriesRenderingHints.
		assertStaircasePerSeries(bare().percent(true).stacked(true).secondValues(SECOND_VALUES), StepMode.AFTER_LITERAL,
				"geometry-percent");
	}

	// ------------------------------------------------------------ 8./9. depth

	@Test
	void twoDimensionalWithDepthStepsLikeThePlainTwoDimensionalChart() throws Exception {
		assertStaircase(bare().dimension(ChartDimension.TWO_DIMENSIONAL_WITH_DEPTH_LITERAL), StepMode.AFTER_LITERAL,
				VALUES, 4, "geometry-depth");
	}

	@Test
	void threeDimensionalRenderingGetsALocation3DArray() {
		// The stock three dimensional data point renderer casts the location array to
		// Location3D[] without a check. If the expansion allocated a plain
		// Location[], then the cast would fail with a ClassCastException before any
		// drawing. The test asserts nothing else, because the chart engine draws a
		// three dimensional line as a Line3DRenderEvent through the z-sorted deferred
		// cache, and the device records LineRenderEvents only.
		Options options = bare().dimension(ChartDimension.THREE_DIMENSIONAL_LITERAL);
		assertDoesNotThrow(() -> render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES, options),
				"geometry-3d"));
	}

	// ------------------------------------------------------------- 10. curve

	@Test
	void aCurveSeriesStillSteps() throws Exception {
		// The layout does not depend on the curve flag, so the plain chart is a valid
		// reference chart for the curved one. The test asserts that the renderer
		// skips the curve path of Line.renderSeries and draws the same vertices.
		List<double[]> stock = vertices(render(ChartFixtures.lineChart(VALUES, bare()), "geometry-curve-oracle").segments);

		List<Seg> steps = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES, bare().curve(true)),
				"geometry-curve").segments;

		assertEquals(4, steps.size(), "a curved piecewise constant series stays piecewise constant and is no spline");
		assertAxisParallel(steps);
		assertVertices(staircase(stock, StepMode.AFTER_LITERAL, false), vertices(steps));
	}

	// ------------------------------------------------------------ 11. shadow

	@Test
	void theShadowIsTheStaircaseOffsetDownwards() throws Exception {
		Options options = bare().shadowColor(ColorDefinitionImpl.GREY());

		List<Seg> steps = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES, options),
				"geometry-shadow").segments;

		assertEquals(8, steps.size(), "the shadow doubles the four segments of the piecewise constant line");
		// Line draws the shadow before the line, so the first half of the recorded
		// segments is the shadow copy.
		for (int k = 0; k < 4; k++) {
			Seg shadow = steps.get(k);
			Seg main = steps.get(k + 4);
			assertEquals(main.x1(), shadow.x1(), "shadow segment " + k + " start x");
			assertEquals(main.y1() + 3, shadow.y1(), "shadow segment " + k + " start y");
			assertEquals(main.x2(), shadow.x2(), "shadow segment " + k + " end x");
			assertEquals(main.y2() + 3, shadow.y2(), "shadow segment " + k + " end y");
		}
		assertAxisParallel(steps);
	}

	// ------------------------------------------------- 12. markers and labels

	@Test
	void markersAndLabelsDoNotChangeTheLineGeometry() throws Exception {
		Options options = new Options().markersVisible(true).labelsVisible(true);

		CapturingPngRenderer step = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES, options),
				"geometry-markers");

		assertEquals(4, step.segments.size(), "markers and data point labels add no line segment");
		assertAxisParallel(step.segments);
		assertEquals(0, step.seriesPointOvals,
				"a connected run has no isolated data point, and the device fills the circle markers but"
						+ " never strokes them, so it strokes no oval at all");
	}

	// ------------------------------------------------------- 13. degenerate

	@Test
	void aSinglePointDrawsNoSegment() throws Exception {
		// Only the data point seeker that breaks a run at a missing value detects an
		// isolated data point. That seeker runs if connectMissingValue is false, and
		// only then does the chart engine draw the single data point.
		Options options = bare().connectMissingValue(false);
		Double[] values = { 5.0 };

		CapturingPngRenderer stock = render(ChartFixtures.lineChart(values, options), "geometry-single-oracle");
		CapturingPngRenderer step = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, values, options),
				"geometry-single");

		assertEquals(0, stock.segments.size(), "one data point cannot make a segment");
		assertEquals(0, step.segments.size(), "one data point cannot make a step either");
		assertEquals(stock.seriesPointOvals, step.seriesPointOvals,
				"both charts draw the single data point the same way");
		assertEquals(1, step.seriesPointOvals, "the chart draws the single data point as one oval");
	}

	@Test
	void anEmptySeriesFailsExactlyWhereAStockLineFails() {
		// An empty data set never reaches a renderer. The chart engine rejects it
		// while it computes the axis scale, for a piecewise constant series and for a
		// stock LineSeries alike. This test proves that the piecewise constant series
		// keeps that precondition failure and does not replace it with a worse one.
		Options options = bare().connectMissingValue(false);

		ChartException stock = assertThrows(ChartException.class,
				() -> render(ChartFixtures.lineChart(new Double[0], options), "geometry-empty-oracle"));
		ChartException step = assertThrows(ChartException.class,
				() -> render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, new Double[0], options),
						"geometry-empty"));

		assertEquals(stock.getMessage(), step.getMessage(), "both series must fail with the same message");
	}

	// ---------------------------------------------------------------- helpers

	/**
	 * The two vertex lists of one comparison: {@code stock} contains the vertices of
	 * the reference chart, and {@code step} contains the vertices of the piecewise
	 * constant chart.
	 */
	private record Oracle(List<double[]> stock, List<double[]> step) {
	}

	/**
	 * Renders the reference chart and the piecewise constant chart with the same
	 * options. The method then asserts that the piecewise constant chart contains
	 * the vertices of the reference chart plus the corner vertices of
	 * <code>mode</code>.
	 * <p>
	 * Side effects: the method writes two PNG files into the test output
	 * directory.
	 *
	 * @param options          the fixture options of both charts
	 * @param mode             the step mode
	 * @param values           the values of the value series
	 * @param expectedSegments the number of segments the piecewise constant line
	 *                         must contain
	 * @param name             the base name of the two PNG files
	 * @return both vertex lists
	 * @throws Exception if the chart engine fails
	 */
	private Oracle assertStaircase(Options options, StepMode mode, Double[] values, int expectedSegments, String name)
			throws Exception {
		List<double[]> stock = vertices(render(ChartFixtures.lineChart(values, options), name + "-oracle").segments);

		List<Seg> steps = render(ChartFixtures.piecewiseConstantChart(mode, values, options), name).segments;

		assertEquals(expectedSegments, steps.size(), "segment count");
		assertAxisParallel(steps);
		List<double[]> stepVertices = vertices(steps);
		assertVertices(staircase(stock, mode, options.isTransposed()), stepVertices);
		return new Oracle(stock, stepVertices);
	}

	/**
	 * Runs the comparison of {@link #assertStaircase} for a chart with more than
	 * one value series.
	 * <p>
	 * The method splits the recorded segments per series by the identity of the
	 * {@code StructureSource} that each segment carries. It then compares every
	 * value series with the same value series of the reference chart.
	 * <p>
	 * Side effects: the method writes two PNG files into the test output
	 * directory.
	 *
	 * @param options the fixture options of both charts
	 * @param mode    the step mode
	 * @param name    the base name of the two PNG files
	 * @throws Exception if the chart engine fails
	 */
	private void assertStaircasePerSeries(Options options, StepMode mode, String name) throws Exception {
		List<List<Seg>> stock = render(ChartFixtures.lineChart(VALUES, options), name + "-oracle").segmentGroups();

		List<List<Seg>> steps = render(ChartFixtures.piecewiseConstantChart(mode, VALUES, options), name).segmentGroups();

		assertEquals(2, stock.size(), "the reference chart draws two value series");
		assertEquals(stock.size(), steps.size(), "the piecewise constant chart draws the same value series");
		for (int series = 0; series < stock.size(); series++) {
			assertAxisParallel(steps.get(series));
			assertVertices(staircase(vertices(stock.get(series)), mode, options.isTransposed()),
					vertices(steps.get(series)));
		}
	}

	/**
	 * Renders one chart to <code>&lt;name&gt;.png</code> in the test output
	 * directory.
	 *
	 * @param chart the chart to render
	 * @param name  the base name of the PNG file
	 * @return the device that recorded the render
	 * @throws Exception if the chart engine fails
	 */
	private static CapturingPngRenderer render(ChartWithAxes chart, String name) throws Exception {
		CapturingPngRenderer device = new CapturingPngRenderer();
		CapturingPngRenderer.render(chart, new File(CapturingPngRenderer.outputDirectory(), name + ".png"), device);
		return device;
	}

	/**
	 * Turns a run of segments into its vertex list. The method also asserts that
	 * the run is connected: every segment starts where the segment before it
	 * ended.
	 *
	 * @param segments the recorded segments of one value series
	 * @return the vertices of the run, in drawing order
	 */
	private static List<double[]> vertices(List<Seg> segments) {
		assertTrue(!segments.isEmpty(), "the value series drew no segment");

		List<double[]> points = new ArrayList<>();
		points.add(segments.get(0).start());
		for (int k = 0; k < segments.size(); k++) {
			Seg segment = segments.get(k);
			double[] previous = points.get(points.size() - 1);
			assertEquals(previous[0], segment.x1(),
					"segment " + k + " does not start where segment " + (k - 1) + " ended (x): " + segments);
			assertEquals(previous[1], segment.y1(),
					"segment " + k + " does not start where segment " + (k - 1) + " ended (y): " + segments);
			points.add(segment.end());
		}
		return points;
	}

	/**
	 * Asserts that every segment is a tread or a step, and never a diagonal.
	 *
	 * @param segments the recorded segments of one value series
	 */
	private static void assertAxisParallel(List<Seg> segments) {
		for (Seg segment : segments) {
			assertTrue((segment.x1() == segment.x2()) ^ (segment.y1() == segment.y2()),
					"segment is not axis parallel: " + segment);
		}
	}

	/**
	 * Computes the vertices that the piecewise constant line must contain over a run
	 * of data point vertices.
	 *
	 * @param points     the vertices of the reference chart
	 * @param mode       the step mode
	 * @param transposed {@code true} if the base coordinate and the value
	 *                   coordinate swap device axes
	 * @return the expected vertices, in drawing order
	 */
	private static List<double[]> staircase(List<double[]> points, StepMode mode, boolean transposed) {
		// In a transposed chart the category axis runs down the device y axis and the
		// value axis runs across the device x axis. The two coordinates of a device
		// point therefore swap their roles.
		int base = transposed ? 1 : 0;
		int value = transposed ? 0 : 1;

		List<double[]> out = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			double[] left = points.get(i);
			out.add(left);
			if (i + 1 >= points.size()) {
				break;
			}

			double[] right = points.get(i + 1);
			if (left[value] == right[value]) {
				// Two equal values make one tread and need no corner vertex.
				continue;
			}

			switch (mode) {
			case AFTER_LITERAL -> out.add(corner(right[base], left[value], base, value));
			case BEFORE_LITERAL -> out.add(corner(left[base], right[value], base, value));
			case CENTER_LITERAL -> {
				double middle = (left[base] + right[base]) / 2;
				out.add(corner(middle, left[value], base, value));
				out.add(corner(middle, right[value], base, value));
			}
			}
		}
		return out;
	}

	/**
	 * @param baseCoordinate  the coordinate along the category axis
	 * @param valueCoordinate the coordinate along the value axis
	 * @param base            the index of the base coordinate in a device point
	 * @param value           the index of the value coordinate in a device point
	 * @return one corner vertex as a device point
	 */
	private static double[] corner(double baseCoordinate, double valueCoordinate, int base, int value) {
		double[] point = new double[2];
		point[base] = baseCoordinate;
		point[value] = valueCoordinate;
		return point;
	}

	/**
	 * Compares two vertex lists as text, so that a failure shows both lists.
	 *
	 * @param expected the expected vertices
	 * @param actual   the recorded vertices
	 */
	private static void assertVertices(List<double[]> expected, List<double[]> actual) {
		assertEquals(format(expected), format(actual), "vertex list");
	}

	private static String format(List<double[]> points) {
		StringBuilder text = new StringBuilder();
		for (double[] point : points) {
			text.append('(').append(point[0]).append(", ").append(point[1]).append(")\n");
		}
		return text.toString();
	}
}
