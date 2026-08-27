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
 * Checks the geometry {@link PiecewiseConstantLine} actually puts on the device.
 * <p>
 * The oracle is differential: the same chart is rendered twice, once with a
 * stock line series and once with a piecewise constant series, and the
 * piecewise constant series' vertices must be exactly the stock line's vertices
 * with the staircase corners inserted. Because both charts are built from the
 * same fixture with the same options, the layout math is identical, so the
 * coordinates are compared with exact <code>double</code> equality.
 */
@ExtendWith(ChartPlatformExtension.class)
class PiecewiseConstantGeometryTest {

	/** Three points on categories A, B, C - two steps, one per mode variant. */
	private static final Double[] VALUES = { 5.0, 9.0, 3.0 };

	/** The values of the second series of the stacked and percent charts. */
	private static final Double[] SECOND_VALUES = { 3.0, 4.0, 2.0 };

	/** @return options every geometry test starts from: nothing but the line. */
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

		// The base axis now runs down the device, so the AFTER corner takes its y
		// from the right point and its x from the left one - treads are vertical and
		// risers horizontal, the mirror image of the untransposed case.
		double[] p0 = oracle.stock.get(0);
		double[] p1 = oracle.stock.get(1);
		double[] corner = oracle.step.get(1);
		assertEquals(p0[0], corner[0], "the corner keeps the value coordinate of the left point");
		assertEquals(p1[1], corner[1], "the corner takes the base coordinate of the right point");
	}

	// ----------------------------------------------------------- 3./4. missing

	@Test
	void aBrokenRunDrawsIsolatedPointsAndNoLine() throws Exception {
		Options options = bare().connectMissingValue(false);
		Double[] values = { 5.0, null, 3.0 };

		CapturingPngRenderer stock = render(ChartFixtures.lineChart(values, options), "geometry-null-gap-oracle");
		CapturingPngRenderer step = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, values, options),
				"geometry-null-gap");

		assertEquals(0, stock.segments.size(), "the stock line does not bridge the gap either");
		assertEquals(0, step.segments.size(), "a broken run has no segment to put a corner on");
		assertTrue(stock.seriesPointOvals > 0, "the stock line draws its isolated points as ovals");
		assertEquals(stock.seriesPointOvals, step.seriesPointOvals,
				"the piecewise constant series draws exactly the isolated points the stock line draws");
	}

	@Test
	void aBridgedRunStepsStraightOverTheMissingPoint() throws Exception {
		Options options = bare().connectMissingValue(true);
		Double[] values = { 5.0, null, 3.0 };

		Oracle oracle = assertStaircase(options, StepMode.AFTER_LITERAL, values, 2, "geometry-null-connected");

		assertEquals(2, oracle.stock.size(), "the stock line bridges the gap with a single segment");
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
		// A percent axis reads its scale from the stacked group of a series, so
		// BIRT only computes one for stacked series - an unstacked percent chart
		// dies with a null AxisSubUnit in PlotWith2DAxes.getSeriesRenderingHints.
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
		// The stock three dimensional data point renderer hard casts the location
		// array to Location3D[], so an expansion that allocated a plain Location[]
		// would blow up with a ClassCastException before drawing anything. There is
		// nothing else to assert here: 3D lines are drawn as Line3DRenderEvents
		// through the z-sorted deferred cache, not as the LineRenderEvents the
		// capturing device records.
		Options options = bare().dimension(ChartDimension.THREE_DIMENSIONAL_LITERAL);
		assertDoesNotThrow(() -> render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES, options),
				"geometry-3d"));
	}

	// ------------------------------------------------------------- 10. curve

	@Test
	void aCurveSeriesStillSteps() throws Exception {
		// Layout does not depend on the curve flag, so the plain chart is a valid
		// oracle for the curved one; the assertion is that the curve path in
		// Line.renderSeries is bypassed and the staircase comes out unchanged.
		List<double[]> stock = vertices(render(ChartFixtures.lineChart(VALUES, bare()), "geometry-curve-oracle").segments);

		List<Seg> steps = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES, bare().curve(true)),
				"geometry-curve").segments;

		assertEquals(4, steps.size(), "a curved piecewise constant series is still a staircase, not a spline");
		assertAxisParallel(steps);
		assertVertices(staircase(stock, StepMode.AFTER_LITERAL, false), vertices(steps));
	}

	// ------------------------------------------------------------ 11. shadow

	@Test
	void theShadowIsTheStaircaseOffsetDownwards() throws Exception {
		Options options = bare().shadowColor(ColorDefinitionImpl.GREY());

		List<Seg> steps = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES, options),
				"geometry-shadow").segments;

		assertEquals(8, steps.size(), "the shadow doubles the segment count of the four step staircase");
		// Line renders the shadow before the line itself, so the first half of the
		// captured segments is the shadow copy.
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

		assertEquals(4, step.segments.size(), "markers and labels add no line segments");
		assertAxisParallel(step.segments);
		assertEquals(0, step.seriesPointOvals,
				"a connected run has no isolated point, and the circle markers are filled but never"
						+ " stroked, so nothing counts as a stroked oval");
	}

	// ------------------------------------------------------- 13. degenerate

	@Test
	void aSinglePointDrawsNoSegment() throws Exception {
		// Only the seeker that breaks runs at a missing value detects isolated
		// points, so this is the variant that draws the lone point at all.
		Options options = bare().connectMissingValue(false);
		Double[] values = { 5.0 };

		CapturingPngRenderer stock = render(ChartFixtures.lineChart(values, options), "geometry-single-oracle");
		CapturingPngRenderer step = render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, values, options),
				"geometry-single");

		assertEquals(0, stock.segments.size(), "one point cannot make a segment");
		assertEquals(0, step.segments.size(), "one point cannot make a step either");
		assertEquals(stock.seriesPointOvals, step.seriesPointOvals, "the lone point is drawn the same way");
		assertEquals(1, step.seriesPointOvals, "the lone point is drawn as one oval");
	}

	@Test
	void anEmptySeriesFailsExactlyWhereAStockLineFails() {
		// An empty data set never reaches a renderer: BIRT rejects it while computing
		// the axis scale, for a piecewise constant series exactly as for a stock
		// line. The point of the test is that the piecewise constant series does not
		// turn that clean precondition failure into something worse.
		Options options = bare().connectMissingValue(false);

		ChartException stock = assertThrows(ChartException.class,
				() -> render(ChartFixtures.lineChart(new Double[0], options), "geometry-empty-oracle"));
		ChartException step = assertThrows(ChartException.class,
				() -> render(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, new Double[0], options),
						"geometry-empty"));

		assertEquals(stock.getMessage(), step.getMessage(), "both series fail the same way");
	}

	// ---------------------------------------------------------------- helpers

	/** The vertex lists of a differential comparison. */
	private record Oracle(List<double[]> stock, List<double[]> step) {
	}

	/**
	 * Renders the stock line chart and the piecewise constant chart for the same
	 * options and asserts that the piecewise constant series is the stock line with
	 * the staircase corners of <code>mode</code> inserted.
	 *
	 * @param options          the fixture options both charts are built with
	 * @param mode             the step mode
	 * @param values           the value series
	 * @param expectedSegments how many segments the staircase must consist of
	 * @param name             the base name of the PNG files written on the way
	 * @return both vertex lists
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
	 * The same differential comparison as {@link #assertStaircase}, but for a chart
	 * with more than one value series: the captured segments are split per series
	 * by the identity of the {@code StructureSource} they carry, and every series
	 * is compared with its counterpart in the stock rendering.
	 */
	private void assertStaircasePerSeries(Options options, StepMode mode, String name) throws Exception {
		List<List<Seg>> stock = render(ChartFixtures.lineChart(VALUES, options), name + "-oracle").segmentGroups();

		List<List<Seg>> steps = render(ChartFixtures.piecewiseConstantChart(mode, VALUES, options), name).segmentGroups();

		assertEquals(2, stock.size(), "the stock chart draws two series");
		assertEquals(stock.size(), steps.size(), "the piecewise constant chart draws the same series");
		for (int series = 0; series < stock.size(); series++) {
			assertAxisParallel(steps.get(series));
			assertVertices(staircase(vertices(stock.get(series)), mode, options.isTransposed()),
					vertices(steps.get(series)));
		}
	}

	/**
	 * Renders a chart to <code>&lt;name&gt;.png</code> in the test output directory
	 * and returns the device that captured it.
	 */
	private static CapturingPngRenderer render(ChartWithAxes chart, String name) throws Exception {
		CapturingPngRenderer device = new CapturingPngRenderer();
		CapturingPngRenderer.render(chart, new File(CapturingPngRenderer.outputDirectory(), name + ".png"), device);
		return device;
	}

	/**
	 * Turns a run of segments into its vertex list, asserting on the way that the
	 * run is connected: every segment starts exactly where the previous one ended.
	 */
	private static List<double[]> vertices(List<Seg> segments) {
		assertTrue(!segments.isEmpty(), "expected at least one segment");

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

	/** Every staircase segment is either a tread or a riser, never a diagonal. */
	private static void assertAxisParallel(List<Seg> segments) {
		for (Seg segment : segments) {
			assertTrue((segment.x1() == segment.x2()) ^ (segment.y1() == segment.y2()),
					"segment is not axis parallel: " + segment);
		}
	}

	/**
	 * The staircase expected over a run of data point vertices.
	 *
	 * @param points     the vertices of the stock line rendering
	 * @param mode       the step mode
	 * @param transposed whether base and value swap device axes
	 */
	private static List<double[]> staircase(List<double[]> points, StepMode mode, boolean transposed) {
		// In a transposed chart the base axis runs down the device y axis and the
		// value axis across the device x axis, so the roles of the two coordinates
		// of a device point swap.
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
				// A flat step needs no corner.
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

	private static double[] corner(double baseCoordinate, double valueCoordinate, int base, int value) {
		double[] point = new double[2];
		point[base] = baseCoordinate;
		point[value] = valueCoordinate;
		return point;
	}

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
