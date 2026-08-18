/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant.model.type;

import org.eclipse.birt.chart.model.type.LineSeries;

/**
 * A representation of the model object '<em><b>Piecewise Constant
 * Series</b></em>': a line series whose points are joined by horizontal and
 * vertical segments instead of a direct line, so that the plotted function is
 * constant between the points. Such a series is also known as a step line, a
 * staircase or a zero-order-hold series.
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage#getPiecewiseConstantSeries()
 */
public interface PiecewiseConstantSeries extends LineSeries {

	/**
	 * Returns the value of the '<em><b>Step Mode</b></em>' attribute. The default
	 * value is <code>"After"</code>.
	 *
	 * @return the value of the '<em>Step Mode</em>' attribute
	 * @see StepMode
	 * @see #isSetStepMode()
	 * @see #unsetStepMode()
	 * @see #setStepMode(StepMode)
	 */
	StepMode getStepMode();

	/**
	 * Sets the value of the '{@link #getStepMode() <em>Step Mode</em>}' attribute.
	 *
	 * @param value the new value of the '<em>Step Mode</em>' attribute
	 * @see StepMode
	 * @see #isSetStepMode()
	 * @see #unsetStepMode()
	 * @see #getStepMode()
	 */
	void setStepMode(StepMode value);

	/**
	 * Unsets the value of the '{@link #getStepMode() <em>Step Mode</em>}'
	 * attribute.
	 *
	 * @see #isSetStepMode()
	 * @see #getStepMode()
	 * @see #setStepMode(StepMode)
	 */
	void unsetStepMode();

	/**
	 * Returns whether the value of the '{@link #getStepMode() <em>Step Mode</em>}'
	 * attribute is set.
	 *
	 * @return whether the value of the '<em>Step Mode</em>' attribute is set
	 * @see #unsetStepMode()
	 * @see #getStepMode()
	 * @see #setStepMode(StepMode)
	 */
	boolean isSetStepMode();

	@Override
	PiecewiseConstantSeries copyInstance();
}
