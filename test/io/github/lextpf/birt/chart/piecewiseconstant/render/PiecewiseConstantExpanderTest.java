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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;

/**
 * Verifies the expansion of data points into the vertices of a piecewise
 * constant line.
 * <p>
 * The tests cover the vertex geometry of every {@link StepMode}. They also cover
 * the treatment of a missing value under both <code>connectMissingValue</code>
 * settings, and the rules that the stock line renderer depends on.
 * <p>
 * Constraints: the expansion is pure arithmetic, so these tests need no chart
 * engine and no runtime.
 */
class PiecewiseConstantExpanderTest {

	private static final double[] BASE = { 0, 10, 20 };

	private static final double[] VALUE = { 5, 9, 3 };

	private static boolean[] noNulls(int n) {
		return new boolean[n];
	}

	/**
	 * Compares one expansion with the four expected arrays, and prints all four
	 * actual arrays if one comparison fails.
	 *
	 * @param actual the expansion under test
	 * @param base   the expected base coordinates
	 * @param value  the expected value coordinates
	 * @param owner  the expected owner index per vertex
	 * @param real   the expected real flag per vertex
	 */
	private static void assertExpansion(PiecewiseConstantExpansion actual, double[] base, double[] value, int[] owner,
			boolean[] real) {
		String context = "\nactual base   = " + Arrays.toString(actual.base) //
				+ "\nactual value  = " + Arrays.toString(actual.value) //
				+ "\nactual owner  = " + Arrays.toString(actual.owner) //
				+ "\nactual real   = " + Arrays.toString(actual.real);
		assertArrayEquals(base, actual.base, "base" + context);
		assertArrayEquals(value, actual.value, "value" + context);
		assertArrayEquals(owner, actual.owner, "owner" + context);
		assertArrayEquals(real, actual.real, "real" + context);
	}

	@Test
	void afterModeJumpsAtTheNextPoint() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(BASE, VALUE, noNulls(3), StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10, 10, 20, 20 }, new double[] { 5, 5, 9, 9, 3 },
				new int[] { 0, 0, 1, 1, 2 }, new boolean[] { true, false, true, false, true });
	}

	@Test
	void beforeModeJumpsAtTheCurrentPoint() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(BASE, VALUE, noNulls(3), StepMode.BEFORE_LITERAL, false);

		assertExpansion(out, new double[] { 0, 0, 10, 10, 20 }, new double[] { 5, 9, 9, 3, 3 },
				new int[] { 0, 1, 1, 2, 2 }, new boolean[] { true, false, true, false, true });
	}

	@Test
	void centerModeJumpsHalfwayBetweenPoints() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(BASE, VALUE, noNulls(3), StepMode.CENTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 5, 5, 10, 15, 15, 20 }, new double[] { 5, 5, 9, 9, 9, 3, 3 },
				new int[] { 0, 0, 1, 1, 1, 2, 2 },
				new boolean[] { true, false, false, true, false, false, true });
	}

	@Test
	void equalValuesMakeAFlatStepWithoutACorner() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(new double[] { 0, 10 }, new double[] { 5, 5 }, noNulls(2),
				StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10 }, new double[] { 5, 5 }, new int[] { 0, 1 },
				new boolean[] { true, true });
	}

	@Test
	void aDuplicateBaseDropsTheDegenerateCorner() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(new double[] { 0, 0 }, new double[] { 5, 9 }, noNulls(2),
				StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 0 }, new double[] { 5, 9 }, new int[] { 0, 1 },
				new boolean[] { true, true });
	}

	@Test
	void aDuplicateBaseAlsoDropsTheCornerThatWouldRepeatTheRightPoint() {
		PiecewiseConstantExpansion before = PiecewiseConstantExpander
				.expand(new double[] { 0, 0 }, new double[] { 5, 9 }, noNulls(2),
				StepMode.BEFORE_LITERAL, false);
		PiecewiseConstantExpansion center = PiecewiseConstantExpander
				.expand(new double[] { 0, 0 }, new double[] { 5, 9 }, noNulls(2),
				StepMode.CENTER_LITERAL, false);

		assertExpansion(before, new double[] { 0, 0 }, new double[] { 5, 9 }, new int[] { 0, 1 },
				new boolean[] { true, true });
		assertExpansion(center, new double[] { 0, 0 }, new double[] { 5, 9 }, new int[] { 0, 1 },
				new boolean[] { true, true });
	}

	@Test
	void aSinglePointIsCopiedThrough() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(new double[] { 0 }, new double[] { 5 }, noNulls(1),
				StepMode.CENTER_LITERAL, false);

		assertExpansion(out, new double[] { 0 }, new double[] { 5 }, new int[] { 0 }, new boolean[] { true });
	}

	@Test
	void emptyInputYieldsEmptyArrays() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(new double[0], new double[0], new boolean[0],
				StepMode.AFTER_LITERAL, true);

		assertExpansion(out, new double[0], new double[0], new int[0], new boolean[0]);
	}

	@Test
	void twoPointsGetASingleCornerInAfterMode() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(new double[] { 0, 10 }, new double[] { 5, 9 }, noNulls(2),
				StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10, 10 }, new double[] { 5, 5, 9 }, new int[] { 0, 0, 1 },
				new boolean[] { true, false, true });
	}

	@Test
	void aNullInTheMiddleBreaksTheRunWhenMissingValuesAreNotConnected() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(BASE, new double[] { 5, Double.NaN, 3 },
				new boolean[] { false, true, false }, StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10, 20 }, new double[] { 5, Double.NaN, 3 }, new int[] { 0, 1, 2 },
				new boolean[] { true, true, true });
	}

	@Test
	void aNullInTheMiddleGetsItsCornerBeforeTheNullWhenMissingValuesAreConnected() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(BASE, new double[] { 5, Double.NaN, 3 },
				new boolean[] { false, true, false }, StepMode.AFTER_LITERAL, true);

		assertExpansion(out, new double[] { 0, 20, 10, 20 }, new double[] { 5, 5, Double.NaN, 3 },
				new int[] { 0, 0, 1, 2 }, new boolean[] { true, false, true, true });
	}

	@Test
	void anIsolatedPointBetweenNullsIsLeftAlone() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(BASE, new double[] { Double.NaN, 9, Double.NaN },
				new boolean[] { true, false, true }, StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10, 20 }, new double[] { Double.NaN, 9, Double.NaN },
				new int[] { 0, 1, 2 }, new boolean[] { true, true, true });
	}

	@Test
	void leadingAndTrailingNullsKeepTheirPlaceAroundTheOnlyCorner() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(new double[] { 0, 10, 20, 30 },
				new double[] { Double.NaN, 5, 9, Double.NaN }, new boolean[] { true, false, false, true },
				StepMode.AFTER_LITERAL, true);

		assertExpansion(out, new double[] { 0, 10, 20, 20, 30 }, new double[] { Double.NaN, 5, 5, 9, Double.NaN },
				new int[] { 0, 1, 1, 2, 3 }, new boolean[] { true, true, false, true, true });
	}

	@Test
	void allNullPointsAreCopiedThroughUnchanged() {
		PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(BASE, new double[] { Double.NaN, Double.NaN, Double.NaN },
				new boolean[] { true, true, true }, StepMode.CENTER_LITERAL, true);

		assertExpansion(out, new double[] { 0, 10, 20 }, new double[] { Double.NaN, Double.NaN, Double.NaN },
				new int[] { 0, 1, 2 }, new boolean[] { true, true, true });
	}

	@Test
	void missingValuesAreNullValues() {
		assertTrue(PiecewiseConstantExpander.isNullValue(null), "a null reference is a missing value");
		assertTrue(PiecewiseConstantExpander.isNullValue(Double.NaN), "NaN is a missing value");
		assertTrue(PiecewiseConstantExpander.isNullValue(Double.valueOf(Double.NaN)), "a NaN Double is a missing value");
	}

	@Test
	void presentValuesAreNotNullValues() {
		assertFalse(PiecewiseConstantExpander.isNullValue(Double.valueOf(1)), "1.0 is a present value");
		assertFalse(PiecewiseConstantExpander.isNullValue(Integer.valueOf(0)), "0 is a present value");
		assertFalse(PiecewiseConstantExpander.isNullValue("x"), "a non-number is not a missing value");
	}

	@Test
	void randomInputsKeepTheStepInvariants() {
		Random random = new Random(20260827L);
		int nullPoints = 0;
		int corners = 0;

		for (int iteration = 0; iteration < 1000; iteration++) {
			int n = random.nextInt(13);
			double[] base = new double[n];
			double[] value = new double[n];
			boolean[] isNull = new boolean[n];
			// The base coordinates increase strictly, as they do along a category axis.
			double b = random.nextInt(5);
			for (int i = 0; i < n; i++) {
				base[i] = b;
				b += 1 + random.nextInt(5);
				isNull[i] = random.nextInt(4) == 0;
				value[i] = isNull[i] ? Double.NaN : random.nextInt(4);
				nullPoints += isNull[i] ? 1 : 0;
			}

			for (StepMode mode : StepMode.VALUES) {
				for (boolean connectMissingValue : new boolean[] { false, true }) {
					PiecewiseConstantExpansion out = PiecewiseConstantExpander
				.expand(base, value, isNull, mode, connectMissingValue);
					String context = "iteration " + iteration + ", mode " + mode + ", connectMissingValue "
							+ connectMissingValue //
							+ "\ninput base    = " + Arrays.toString(base) //
							+ "\ninput value   = " + Arrays.toString(value) //
							+ "\ninput isNull  = " + Arrays.toString(isNull) //
							+ "\noutput base   = " + Arrays.toString(out.base) //
							+ "\noutput value  = " + Arrays.toString(out.value) //
							+ "\noutput owner  = " + Arrays.toString(out.owner) //
							+ "\noutput real   = " + Arrays.toString(out.real);

					assertCornersBelongToRealPoints(out, isNull, connectMissingValue, context);
					assertConnectedSegmentsAreAxisParallel(out, isNull, connectMissingValue, context);
					assertRealVerticesAreTheInput(out, base, value, context);
					corners += out.base.length - base.length;
				}
			}
		}

		// The generated data must exercise both cases: many missing values and many
		// corner vertices.
		assertTrue(nullPoints > 1000, "the test generated only " + nullPoints + " missing values");
		assertTrue(corners > 5000, "the test generated only " + corners + " corner vertices");
	}

	/**
	 * Asserts that every corner vertex carries the value of a real data point that
	 * is not a missing value.
	 * <p>
	 * If <code>connectMissingValue</code> is false, then a missing value also
	 * breaks the run. No corner vertex can then stand next to a missing value, and
	 * the detection of isolated data points in the chart engine keeps working. If
	 * <code>connectMissingValue</code> is true, then the data point seeker skips
	 * every missing value. The corner vertex of a bridged pair can then stand
	 * directly before the run of missing values.
	 *
	 * @param out                 the expansion under test
	 * @param isNull              one flag per input point, true for a missing
	 *                            value
	 * @param connectMissingValue the flag the expansion ran with
	 * @param context             the input and output text of the failure message
	 */
	private static void assertCornersBelongToRealPoints(PiecewiseConstantExpansion out, boolean[] isNull,
			boolean connectMissingValue, String context) {
		for (int k = 0; k < out.base.length; k++) {
			assertTrue(out.owner[k] >= 0 && out.owner[k] < isNull.length,
					"vertex " + k + " has an out-of-range owner; " + context);
			if (out.real[k]) {
				continue;
			}
			assertFalse(isNull[out.owner[k]], "corner vertex " + k + " belongs to a missing value; " + context);
			if (connectMissingValue) {
				continue;
			}
			assertFalse(k > 0 && isNullVertex(out, k - 1, isNull),
					"corner vertex " + k + " follows a missing value; " + context);
			assertFalse(k + 1 < out.base.length && isNullVertex(out, k + 1, isNull),
					"corner vertex " + k + " precedes a missing value; " + context);
		}
	}

	/**
	 * Asserts that every pair of vertices that the data point seeker joins is axis
	 * parallel. Two such vertices differ in exactly one coordinate.
	 *
	 * @param out                 the expansion under test
	 * @param isNull              one flag per input point, true for a missing
	 *                            value
	 * @param connectMissingValue the flag the expansion ran with
	 * @param context             the input and output text of the failure message
	 */
	private static void assertConnectedSegmentsAreAxisParallel(PiecewiseConstantExpansion out, boolean[] isNull,
			boolean connectMissingValue, String context) {
		int previous = -1;
		boolean runBroken = false;
		for (int k = 0; k < out.base.length; k++) {
			if (isNullVertex(out, k, isNull)) {
				// The renderer draws no segment to a missing value. If connectMissingValue
				// is false, then a missing value also starts a new run.
				runBroken |= !connectMissingValue;
				continue;
			}
			if (previous >= 0 && !runBroken) {
				boolean sameBase = out.base[previous] == out.base[k];
				boolean sameValue = out.value[previous] == out.value[k];
				assertTrue(sameBase ^ sameValue,
						"segment " + previous + " -> " + k + " is not axis parallel; " + context);
			}
			previous = k;
			runBroken = false;
		}
	}

	/**
	 * Asserts that the real vertices are the input points, unchanged and in the
	 * input order.
	 *
	 * @param out     the expansion under test
	 * @param base    the base coordinates of the input points
	 * @param value   the value coordinates of the input points
	 * @param context the input and output text of the failure message
	 */
	private static void assertRealVerticesAreTheInput(PiecewiseConstantExpansion out, double[] base, double[] value,
			String context) {
		double[] realBase = new double[base.length];
		double[] realValue = new double[base.length];
		int[] realOwner = new int[base.length];
		int[] expectedOwner = new int[base.length];
		int count = 0;
		for (int k = 0; k < out.base.length; k++) {
			if (!out.real[k]) {
				continue;
			}
			assertTrue(count < base.length, "more real vertices than input points; " + context);
			realBase[count] = out.base[k];
			realValue[count] = out.value[k];
			realOwner[count] = out.owner[k];
			expectedOwner[count] = count;
			count++;
		}
		assertEquals(base.length, count, "wrong number of real vertices; " + context);
		assertArrayEquals(base, realBase, "real base sequence; " + context);
		assertArrayEquals(value, realValue, "real value sequence; " + context);
		assertArrayEquals(expectedOwner, realOwner, "real owner sequence; " + context);
	}

	/**
	 * @param out    the expansion under test
	 * @param k      the index of one vertex
	 * @param isNull one flag per input point, true for a missing value
	 * @return {@code true} if the vertex is a real data point and that data point
	 *         is a missing value
	 */
	private static boolean isNullVertex(PiecewiseConstantExpansion out, int k, boolean[] isNull) {
		return out.real[k] && isNull[out.owner[k]];
	}
}
