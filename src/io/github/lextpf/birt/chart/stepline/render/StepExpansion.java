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

/**
 * The staircase vertices produced by
 * {@link StepPointExpander#expand(double[], double[], boolean[], io.github.lextpf.birt.chart.stepline.model.type.StepMode, boolean)}.
 * <p>
 * The four arrays have the same length <code>m &gt;= n</code>, one entry per
 * output vertex, in drawing order:
 * <ul>
 * <li><code>base[k]</code> - the coordinate along the category/base axis;</li>
 * <li><code>value[k]</code> - the coordinate along the value axis;</li>
 * <li><code>owner[k]</code> - the index into the original arrays whose value
 * this vertex carries. A vertex is always attributed to a real, non-null data
 * point, so a caller may safely take the tooltip, marker or label data of
 * <code>owner[k]</code> for vertex <code>k</code>;</li>
 * <li><code>real[k]</code> - <code>true</code> for the original data points
 * (which appear unchanged and in their original order), <code>false</code> for
 * the synthetic corners inserted between them.</li>
 * </ul>
 * <p>
 * The class is coordinate-system agnostic: it holds whatever pair of coordinates
 * the caller passed in, typically device-space pixels.
 */
public final class StepExpansion {

	/** The base (category) axis coordinate of every vertex. */
	public final double[] base;

	/** The value axis coordinate of every vertex. */
	public final double[] value;

	/** For every vertex, the index of the original point whose value it carries. */
	public final int[] owner;

	/** For every vertex, <code>true</code> for an original point. */
	public final boolean[] real;

	/**
	 * Creates an expansion from its four parallel arrays.
	 *
	 * @param base  the base axis coordinates
	 * @param value the value axis coordinates
	 * @param owner the index of the owning original point per vertex
	 * @param real  <code>true</code> per original point, <code>false</code> per
	 *              synthetic corner
	 */
	public StepExpansion(double[] base, double[] value, int[] owner, boolean[] real) {
		this.base = base;
		this.value = value;
		this.owner = owner;
		this.real = real;
	}
}
