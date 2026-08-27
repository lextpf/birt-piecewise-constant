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
 * Expands the data points of one series into the vertices of a piecewise
 * constant line.
 * <p>
 * Intent: the stock line renderer draws straight segments between consecutive
 * vertices. This class inserts one corner vertex per step, so those segments
 * form treads and steps.
 * <p>
 * Constraints: the class does not know the coordinate system. {@code base} holds
 * the coordinate along the category axis (BIRT: base axis). {@code value} holds
 * the coordinate along the value axis. Both hold coordinates of the space of the
 * caller, which is usually device pixels. The class does not use the chart
 * engine, so a test can call it without a started chart engine.
 * <p>
 * Non-obvious behaviour: the following rules keep the stock line renderer
 * working on the longer arrays.
 * <ul>
 * <li><b>R1</b> - the expander copies a missing value through without change and
 * gives it no corner vertex. The isolated-point detection of the chart engine
 * therefore still sees the original neighbours of every missing value;</li>
 * <li><b>R2</b> - if {@code connectMissingValue} is false, then a missing value
 * ends the run. If it is true, then the run continues across the missing values.
 * The expander then writes the corner vertex of the bridged pair directly after
 * its left data point, that is before the missing values;</li>
 * <li><b>R3</b> - two equal consecutive values stay on one tread and need no
 * corner vertex;</li>
 * <li><b>R4</b> - the geometry of the corner vertex follows the
 * {@link StepMode};</li>
 * <li><b>R5</b> - the expander drops a corner vertex that is equal to the vertex
 * before it, or equal to the data point it leads to. This happens if two data
 * points share one base coordinate;</li>
 * <li><b>R6</b> - the expander writes every data point, unchanged and in order.
 * Every corner vertex names a data point that has a value, through
 * {@link PiecewiseConstantExpansion#owner}.</li>
 * </ul>
 */
public final class PiecewiseConstantExpander {

	private PiecewiseConstantExpander() {
	}

	/**
	 * Tells whether a data value counts as a missing value.
	 * <p>
	 * Intent: the method gives the same answer as {@code BaseRenderer.isNaN(Object)}
	 * of the chart engine. That method is protected and static, so this class
	 * cannot call it.
	 *
	 * @param v the value taken from a data set
	 * @return {@code true} for {@code null} and for a {@link Number} that is
	 *         {@code NaN}; {@code false} for every other value
	 */
	public static boolean isNullValue(Object v) {
		return v == null || (v instanceof Number n && Double.isNaN(n.doubleValue()));
	}

	/**
 * Expands the data points of one series into the vertices of a piecewise
 * constant line.
	 * <p>
	 * Constraints: {@code base}, {@code value} and {@code isNull} must have the
	 * same length. The caller must pass the same {@code connectMissingValue} flag
	 * that the chart engine uses for this series.
	 *
	 * @param connectMissingValue {@code true} if the renderer connects the data
	 *                            points around a missing value instead of ending
	 *                            the run
	 * @return the vertices; four arrays of the same length
	 *         {@code m >= base.length}
	 */
	public static PiecewiseConstantExpansion expand(double[] base, double[] value, boolean[] isNull, StepMode mode,
			boolean connectMissingValue) {
		int n = base.length;
		// One data point gives one vertex plus at most two corner vertices (Center).
		Vertices out = new Vertices(3 * n);

		for (int i = 0; i < n; i++) {
			// R6: the data point itself, unchanged and in order.
			out.point(base[i], value[i], i);

			// R1: a missing value passes through and never carries a corner vertex.
			if (isNull[i]) {
				continue;
			}

			// R2: the data point that the stock renderer connects to this one.
			int right = nextConnected(isNull, i, connectMissingValue);
			if (right < 0) {
				continue;
			}

			double bL = base[i];
			double vL = value[i];
			double bR = base[right];
			double vR = value[right];

			// R3: two equal values stay on one tread and need no corner vertex.
			if (vL == vR) {
				continue;
			}

			// R4: the geometry of the corner vertex. The corner vertex belongs to the
			// data point whose value it carries.
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
	 * Returns the index of the data point that follows {@code i} in the same run
	 * (R2).
	 * <p>
	 * Non-obvious behaviour: the search copies the data point seeker of the stock
	 * renderer. If {@code connectMissingValue} is false, then the first missing
	 * value after {@code i} ends the run.
	 *
	 * @return the index of the next connected data point, or {@code -1} if the run
	 *         ends at {@code i}
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
	 * Collects the output vertices and applies the rule for the degenerate corner
	 * vertex (R5).
	 * <p>
	 * Constraints: the caller must append the vertices in drawing order, and must
	 * not append more vertices than the capacity allows. {@link #corner} compares
	 * only against the vertex that it appended last.
	 */
	private static final class Vertices {

		private final double[] base;

		private final double[] value;

		/** For every vertex, the index of the data point whose value it carries. */
		private final int[] owner;

		private final boolean[] real;

		private int size;

		Vertices(int capacity) {
			base = new double[capacity];
			value = new double[capacity];
			owner = new int[capacity];
			real = new boolean[capacity];
		}

		void point(double b, double v, int index) {
			append(b, v, index, true);
		}

		/**
		 * Appends one corner vertex (R5).
		 * <p>
		 * Non-obvious behaviour: the method drops the corner vertex if it is equal to
		 * the vertex before it. The method also drops it if it is equal to the data
		 * point {@code (bRight, vRight)} that it leads to.
		 *
		 * @param b      the category axis coordinate of the corner vertex
		 * @param v      the value axis coordinate of the corner vertex
		 * @param index  the index of the data point whose value the corner vertex
		 *               carries
		 * @param bRight the category axis coordinate of the data point it leads to
		 * @param vRight the value axis coordinate of the data point it leads to
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

		PiecewiseConstantExpansion trim() {
			return new PiecewiseConstantExpansion(Arrays.copyOf(base, size), Arrays.copyOf(value, size),
					Arrays.copyOf(owner, size), Arrays.copyOf(real, size));
		}
	}
}
