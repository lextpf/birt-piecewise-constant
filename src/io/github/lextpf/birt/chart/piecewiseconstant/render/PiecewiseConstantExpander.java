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

import java.util.Arrays;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;

/**
 * Turns the points of a series into the vertices of a piecewise-constant
 * (staircase) line by inserting a synthetic corner between every pair of
 * connected points.
 * <p>
 * The expansion is pure and coordinate-system agnostic: <code>base</code> is the
 * coordinate along the category/base axis and <code>value</code> the coordinate
 * along the value axis, in whatever space the caller works in (typically device
 * pixels). It knows nothing about the chart engine, so it can be unit tested
 * without the platform.
 * <p>
 * The rules are chosen so that the stock line renderer keeps working on the
 * longer arrays:
 * <ul>
 * <li><b>R1</b> - a null point is copied through verbatim and never carries a
 * corner, so the renderer's isolated-point detection still sees the original
 * neighbourhood of every null;</li>
 * <li><b>R2</b> - with <code>connectMissingValue == false</code> a null breaks
 * the run; with <code>true</code> the run continues across the nulls and the
 * corner of the bridged pair is emitted right after its left point, that is
 * before the null run;</li>
 * <li><b>R3</b> - two equal values form a flat step and need no corner;</li>
 * <li><b>R4</b> - the corner geometry follows the {@link StepMode};</li>
 * <li><b>R5</b> - a corner that coincides with the vertex before it or with the
 * point it leads to is dropped (this happens when two points share a base
 * coordinate);</li>
 * <li><b>R6</b> - every original point is emitted, unchanged and in order, and
 * every vertex is attributed to a real, non-null point through
 * {@link PiecewiseConstantExpansion#owner}.</li>
 * </ul>
 */
public final class PiecewiseConstantExpander {

	/**
	 * Only static members.
	 */
	private PiecewiseConstantExpander() {
	}

	/**
	 * Tells whether a data value counts as missing, exactly as the chart engine's
	 * <code>BaseRenderer.isNaN(Object)</code> does (which is not visible from
	 * here).
	 *
	 * @param v the value taken from a data set
	 * @return <code>true</code> for <code>null</code> and for a {@link Number} that
	 *         is <code>NaN</code>
	 */
	public static boolean isNullValue(Object v) {
		return v == null || (v instanceof Number n && Double.isNaN(n.doubleValue()));
	}

	/**
	 * Expands the points of a series into staircase vertices.
	 *
	 * @param base                the base axis coordinate of every point
	 * @param value               the value axis coordinate of every point; the
	 *                            entries of null points are copied through
	 *                            untouched
	 * @param isNull              <code>true</code> for every point the renderer
	 *                            treats as missing
	 * @param mode                where the line jumps from one value to the next
	 * @param connectMissingValue whether the renderer connects the points around a
	 *                            null instead of breaking the run
	 * @return the expanded vertices; four arrays of the same length
	 *         <code>m &gt;= base.length</code>
	 */
	public static PiecewiseConstantExpansion expand(double[] base, double[] value, boolean[] isNull, StepMode mode,
			boolean connectMissingValue) {
		int n = base.length;
		// A point contributes itself plus at most two corners (CENTER).
		Vertices out = new Vertices(3 * n);

		for (int i = 0; i < n; i++) {
			// R6: the point itself, unchanged and in order.
			out.point(base[i], value[i], i);

			// R1: nulls are pass-through only, they never carry a corner.
			if (isNull[i]) {
				continue;
			}

			// R2: the partner the renderer's seeker would connect this point to.
			int right = nextConnected(isNull, i, connectMissingValue);
			if (right < 0) {
				continue;
			}

			double bL = base[i];
			double vL = value[i];
			double bR = base[right];
			double vR = value[right];

			// R3: a flat step needs no corner.
			if (vL == vR) {
				continue;
			}

			// R4: the corner geometry, owned by the point whose value it carries.
			switch (mode) {
			case AFTER_LITERAL -> out.corner(bR, vL, i, bR, vR);
			case BEFORE_LITERAL -> out.corner(bL, vR, right, bR, vR);
			case CENTER_LITERAL -> {
				double bM = (bL + bR) / 2;
				out.corner(bM, vL, i, bR, vR);
				out.corner(bM, vR, right, bR, vR);
			}
			}
		}

		return out.trim();
	}

	/**
	 * Returns the index of the point that follows <code>i</code> in the same run,
	 * or <code>-1</code> when the run ends at <code>i</code> (R2).
	 */
	private static int nextConnected(boolean[] isNull, int i, boolean connectMissingValue) {
		for (int right = i + 1; right < isNull.length; right++) {
			if (!isNull[right]) {
				return right;
			}
			if (!connectMissingValue) {
				return -1;
			}
		}
		return -1;
	}

	/**
	 * Collects the output vertices and applies the degenerate-corner rule (R5).
	 */
	private static final class Vertices {

		private final double[] base;

		private final double[] value;

		private final int[] owner;

		private final boolean[] real;

		private int size;

		Vertices(int capacity) {
			base = new double[capacity];
			value = new double[capacity];
			owner = new int[capacity];
			real = new boolean[capacity];
		}

		/**
		 * Appends an original data point.
		 */
		void point(double b, double v, int index) {
			append(b, v, index, true);
		}

		/**
		 * Appends a synthetic corner unless it coincides with the vertex before it or
		 * with the point <code>(bRight, vRight)</code> it leads to (R5).
		 */
		void corner(double b, double v, int index, double bRight, double vRight) {
			if (size > 0 && base[size - 1] == b && value[size - 1] == v) {
				return;
			}
			if (b == bRight && v == vRight) {
				return;
			}
			append(b, v, index, false);
		}

		private void append(double b, double v, int index, boolean isReal) {
			base[size] = b;
			value[size] = v;
			owner[size] = index;
			real[size] = isReal;
			size++;
		}

		/**
		 * Returns the vertices, trimmed to their actual length.
		 */
		PiecewiseConstantExpansion trim() {
			return new PiecewiseConstantExpansion(Arrays.copyOf(base, size), Arrays.copyOf(value, size),
					Arrays.copyOf(owner, size), Arrays.copyOf(real, size));
		}
	}
}
