/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.github.lextpf.birt.chart.stepline.model.type.StepMode;

/**
 * Verifies the pure staircase expansion: the vertex geometry per
 * {@link StepMode}, the treatment of null points under both
 * <code>connectMissingValue</code> settings, and the invariants the BIRT line
 * renderer relies on.
 */
class StepPointExpanderTest {

	private static final double[] BASE = { 0, 10, 20 };

	private static final double[] VALUE = { 5, 9, 3 };

	private static boolean[] noNulls(int n) {
		return new boolean[n];
	}

	private static void assertExpansion(StepExpansion actual, double[] base, double[] value, int[] owner,
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
		StepExpansion out = StepPointExpander.expand(BASE, VALUE, noNulls(3), StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10, 10, 20, 20 }, new double[] { 5, 5, 9, 9, 3 },
				new int[] { 0, 0, 1, 1, 2 }, new boolean[] { true, false, true, false, true });
	}

	@Test
	void beforeModeJumpsAtTheCurrentPoint() {
		StepExpansion out = StepPointExpander.expand(BASE, VALUE, noNulls(3), StepMode.BEFORE_LITERAL, false);

		assertExpansion(out, new double[] { 0, 0, 10, 10, 20 }, new double[] { 5, 9, 9, 3, 3 },
				new int[] { 0, 1, 1, 2, 2 }, new boolean[] { true, false, true, false, true });
	}

	@Test
	void centerModeJumpsHalfwayBetweenPoints() {
		StepExpansion out = StepPointExpander.expand(BASE, VALUE, noNulls(3), StepMode.CENTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 5, 5, 10, 15, 15, 20 }, new double[] { 5, 5, 9, 9, 9, 3, 3 },
				new int[] { 0, 0, 1, 1, 1, 2, 2 },
				new boolean[] { true, false, false, true, false, false, true });
	}

	@Test
	void equalValuesMakeAFlatStepWithoutACorner() {
		StepExpansion out = StepPointExpander.expand(new double[] { 0, 10 }, new double[] { 5, 5 }, noNulls(2),
				StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10 }, new double[] { 5, 5 }, new int[] { 0, 1 },
				new boolean[] { true, true });
	}

	@Test
	void aDuplicateBaseDropsTheDegenerateCorner() {
		StepExpansion out = StepPointExpander.expand(new double[] { 0, 0 }, new double[] { 5, 9 }, noNulls(2),
				StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 0 }, new double[] { 5, 9 }, new int[] { 0, 1 },
				new boolean[] { true, true });
	}

	@Test
	void aDuplicateBaseAlsoDropsTheCornerThatWouldRepeatTheRightPoint() {
		StepExpansion before = StepPointExpander.expand(new double[] { 0, 0 }, new double[] { 5, 9 }, noNulls(2),
				StepMode.BEFORE_LITERAL, false);
		StepExpansion center = StepPointExpander.expand(new double[] { 0, 0 }, new double[] { 5, 9 }, noNulls(2),
				StepMode.CENTER_LITERAL, false);

		assertExpansion(before, new double[] { 0, 0 }, new double[] { 5, 9 }, new int[] { 0, 1 },
				new boolean[] { true, true });
		assertExpansion(center, new double[] { 0, 0 }, new double[] { 5, 9 }, new int[] { 0, 1 },
				new boolean[] { true, true });
	}

	@Test
	void aSinglePointIsCopiedThrough() {
		StepExpansion out = StepPointExpander.expand(new double[] { 0 }, new double[] { 5 }, noNulls(1),
				StepMode.CENTER_LITERAL, false);

		assertExpansion(out, new double[] { 0 }, new double[] { 5 }, new int[] { 0 }, new boolean[] { true });
	}

	@Test
	void emptyInputYieldsEmptyArrays() {
		StepExpansion out = StepPointExpander.expand(new double[0], new double[0], new boolean[0],
				StepMode.AFTER_LITERAL, true);

		assertExpansion(out, new double[0], new double[0], new int[0], new boolean[0]);
	}

	@Test
	void twoPointsGetASingleCornerInAfterMode() {
		StepExpansion out = StepPointExpander.expand(new double[] { 0, 10 }, new double[] { 5, 9 }, noNulls(2),
				StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10, 10 }, new double[] { 5, 5, 9 }, new int[] { 0, 0, 1 },
				new boolean[] { true, false, true });
	}

	@Test
	void aNullInTheMiddleBreaksTheRunWhenMissingValuesAreNotConnected() {
		StepExpansion out = StepPointExpander.expand(BASE, new double[] { 5, Double.NaN, 3 },
				new boolean[] { false, true, false }, StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10, 20 }, new double[] { 5, Double.NaN, 3 }, new int[] { 0, 1, 2 },
				new boolean[] { true, true, true });
	}

	@Test
	void aNullInTheMiddleGetsItsCornerBeforeTheNullWhenMissingValuesAreConnected() {
		StepExpansion out = StepPointExpander.expand(BASE, new double[] { 5, Double.NaN, 3 },
				new boolean[] { false, true, false }, StepMode.AFTER_LITERAL, true);

		assertExpansion(out, new double[] { 0, 20, 10, 20 }, new double[] { 5, 5, Double.NaN, 3 },
				new int[] { 0, 0, 1, 2 }, new boolean[] { true, false, true, true });
	}

	@Test
	void anIsolatedPointBetweenNullsIsLeftAlone() {
		StepExpansion out = StepPointExpander.expand(BASE, new double[] { Double.NaN, 9, Double.NaN },
				new boolean[] { true, false, true }, StepMode.AFTER_LITERAL, false);

		assertExpansion(out, new double[] { 0, 10, 20 }, new double[] { Double.NaN, 9, Double.NaN },
				new int[] { 0, 1, 2 }, new boolean[] { true, true, true });
	}

	@Test
	void leadingAndTrailingNullsKeepTheirPlaceAroundTheOnlyCorner() {
		StepExpansion out = StepPointExpander.expand(new double[] { 0, 10, 20, 30 },
				new double[] { Double.NaN, 5, 9, Double.NaN }, new boolean[] { true, false, false, true },
				StepMode.AFTER_LITERAL, true);

		assertExpansion(out, new double[] { 0, 10, 20, 20, 30 }, new double[] { Double.NaN, 5, 5, 9, Double.NaN },
				new int[] { 0, 1, 1, 2, 3 }, new boolean[] { true, true, false, true, true });
	}

	@Test
	void allNullPointsAreCopiedThroughUnchanged() {
		StepExpansion out = StepPointExpander.expand(BASE, new double[] { Double.NaN, Double.NaN, Double.NaN },
				new boolean[] { true, true, true }, StepMode.CENTER_LITERAL, true);

		assertExpansion(out, new double[] { 0, 10, 20 }, new double[] { Double.NaN, Double.NaN, Double.NaN },
				new int[] { 0, 1, 2 }, new boolean[] { true, true, true });
	}

	@Test
	void missingValuesAreNullValues() {
		assertTrue(StepPointExpander.isNullValue(null), "a null reference is a missing value");
		assertTrue(StepPointExpander.isNullValue(Double.NaN), "NaN is a missing value");
		assertTrue(StepPointExpander.isNullValue(Double.valueOf(Double.NaN)), "a NaN Double is a missing value");
	}

	@Test
	void presentValuesAreNotNullValues() {
		assertFalse(StepPointExpander.isNullValue(Double.valueOf(1)), "1.0 is a present value");
		assertFalse(StepPointExpander.isNullValue(Integer.valueOf(0)), "0 is a present value");
		assertFalse(StepPointExpander.isNullValue("x"), "a non-number is not a missing value");
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
			// Device base coordinates are strictly increasing, as they are along an axis.
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
					StepExpansion out = StepPointExpander.expand(base, value, isNull, mode, connectMissingValue);
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

		// The corpus must be worth checking: plenty of nulls and plenty of corners.
		assertTrue(nullPoints > 1000, "only " + nullPoints + " null points were generated");
		assertTrue(corners > 5000, "only " + corners + " corners were generated");
	}

	/**
	 * Every synthetic corner carries the value of a real, non-null data point.
	 * Without connectMissingValue a null also breaks the run, so no corner may sit
	 * next to one there and BIRT's isolated-point detection keeps working. With
	 * connectMissingValue the seeker skips nulls altogether, which is what lets the
	 * corner of a bridged pair sit right before the null run.
	 */
	private static void assertCornersBelongToRealPoints(StepExpansion out, boolean[] isNull,
			boolean connectMissingValue, String context) {
		for (int k = 0; k < out.base.length; k++) {
			assertTrue(out.owner[k] >= 0 && out.owner[k] < isNull.length,
					"vertex " + k + " has an out-of-range owner; " + context);
			if (out.real[k]) {
				continue;
			}
			assertFalse(isNull[out.owner[k]], "corner " + k + " is owned by a null point; " + context);
			if (connectMissingValue) {
				continue;
			}
			assertFalse(k > 0 && isNullVertex(out, k - 1, isNull),
					"corner " + k + " follows a null point; " + context);
			assertFalse(k + 1 < out.base.length && isNullVertex(out, k + 1, isNull),
					"corner " + k + " precedes a null point; " + context);
		}
	}

	/**
	 * Every pair of vertices the renderer's seeker would join is axis parallel: the
	 * two vertices differ in exactly one coordinate.
	 */
	private static void assertConnectedSegmentsAreAxisParallel(StepExpansion out, boolean[] isNull,
			boolean connectMissingValue, String context) {
		int previous = -1;
		boolean runBroken = false;
		for (int k = 0; k < out.base.length; k++) {
			if (isNullVertex(out, k, isNull)) {
				// A null is a gap; without connectMissingValue it also starts a new run.
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

	/** The real vertices are the input points, unchanged and in order. */
	private static void assertRealVerticesAreTheInput(StepExpansion out, double[] base, double[] value,
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

	private static boolean isNullVertex(StepExpansion out, int k, boolean[] isNull) {
		return out.real[k] && isNull[out.owner[k]];
	}
}
