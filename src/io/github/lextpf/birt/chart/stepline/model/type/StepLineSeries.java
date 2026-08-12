/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline.model.type;

import org.eclipse.birt.chart.model.type.LineSeries;

/**
 * A representation of the model object '<em><b>Step Line Series</b></em>': a
 * line series whose points are joined by horizontal and vertical segments
 * instead of a direct line, producing a piecewise-constant (staircase) plot.
 *
 * @see io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage#getStepLineSeries()
 */
public interface StepLineSeries extends LineSeries {

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
	StepLineSeries copyInstance();
}
