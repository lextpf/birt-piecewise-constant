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

/**
 * Holds the vertices of a piecewise constant line.
 * <p>
 * Intent: {@link PiecewiseConstantExpander#expand(double[], double[], boolean[],
 * io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode, boolean)}
 * returns this object. The renderer reads it to build the longer location array.
 * <p>
 * Constraints: the four arrays have the same length {@code m}. This length is
 * not less than the number of data points. Each array holds one entry per
 * vertex, in drawing order. The constructor stores the arrays and does not copy
 * them, so the caller must not change them afterwards.
 * <p>
 * Side effects: none.
 * <p>
 * Non-obvious behaviour: the class does not know the coordinate system. It holds
 * the pair of coordinates that the caller passed in, which is usually device
 * pixels.
 */
public final class PiecewiseConstantExpansion {

	/** The category axis coordinate of every vertex. */
	public final double[] base;

	/** The value axis coordinate of every vertex. */
	public final double[] value;

	/**
	 * For every vertex, the index of the data point whose value the vertex carries.
	 * <p>
	 * Non-obvious behaviour: a corner vertex always names a data point that has a
	 * value. The renderer can therefore take the data point hints of
	 * {@code owner[k]} for vertex {@code k}. The tooltip of a tread then belongs to
	 * a real data point. The vertex of a data point names that data point itself,
	 * also for a missing value.
	 */
	public final int[] owner;

	/**
	 * For every vertex, {@code true} for a data point and {@code false} for a
	 * corner vertex. The data points appear unchanged and in their original order.
	 */
	public final boolean[] real;

	/**
	 * Creates an expansion from its four parallel arrays.
	 * <p>
	 * Constraints: the four arrays must have the same length. The constructor
	 * stores the arrays and does not copy them.
	 *
	 * @param base  the category axis coordinates
	 * @param value the value axis coordinates
	 * @param owner for every vertex, the index of the data point whose value it
	 *              carries
	 * @param real  {@code true} for a data point, {@code false} for a corner vertex
	 */
	public PiecewiseConstantExpansion(double[] base, double[] value, int[] owner, boolean[] real) {
		this.base = base;
		this.value = value;
		this.owner = owner;
		this.real = real;
	}
}
