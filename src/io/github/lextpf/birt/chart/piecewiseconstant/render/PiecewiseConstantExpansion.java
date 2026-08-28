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
 * Constraints: the four arrays have the same length {@code m}. This length is
 * not less than the number of data points. Each array holds one entry per
 * vertex, in drawing order. The constructor stores the arrays and does not copy
 * them, so the caller must not change them afterwards.
 */
public final class PiecewiseConstantExpansion {

	/** The category axis coordinate of every vertex. */
	public final double[] base;

	/** The value axis coordinate of every vertex. */
	public final double[] value;

	/**
	 * For every vertex, the index of the data point whose value the vertex carries.
	 * <p>
	 * See {@link PiecewiseConstantExpander}.
	 */
	public final int[] owner;

	/**
	 * For every vertex, {@code true} for a data point and {@code false} for a
	 * corner vertex. The data points appear unchanged and in their original order.
	 */
	public final boolean[] real;

	public PiecewiseConstantExpansion(double[] base, double[] value, int[] owner, boolean[] real) {
		this.base = base;
		this.value = value;
		this.owner = owner;
		this.real = real;
	}
}
