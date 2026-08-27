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
 * The model object '<em><b>Piecewise Constant Series</b></em>'.
 * <p>
 * Intent: this series holds the same data as a {@link LineSeries}. The renderer
 * joins two consecutive points with a tread and a step instead of a direct line.
 * The plotted function therefore stays constant between the points. The step
 * mode states where the renderer puts the step.
 * <p>
 * Constraints: this interface is the model contract only. The chart engine
 * resolves the renderer from the exact class name of the series object. It
 * therefore looks up the implementation class and never this interface.
 * <p>
 * Side effects: none.
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage#getPiecewiseConstantSeries()
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl
 */
public interface PiecewiseConstantSeries extends LineSeries {

	/**
	 * Returns the step mode of this series.
	 * <p>
	 * Constraints: the default value is <code>"After"</code>. If the caller does not
	 * set the attribute, then this method returns the default value.
	 *
	 * @return the value of the '<em>Step Mode</em>' attribute
	 * @see StepMode
	 * @see #isSetStepMode()
	 * @see #unsetStepMode()
	 * @see #setStepMode(StepMode)
	 */
	StepMode getStepMode();

	/**
	 * Sets the step mode of this series.
	 * <p>
	 * Constraints: a <code>null</code> value puts the attribute back to the default
	 * value <code>"After"</code>.
	 * <p>
	 * Side effects: the implementation marks the attribute as set. The serializer
	 * then writes a <code>StepMode</code> element for this series.
	 *
	 * @param value the new value of the '<em>Step Mode</em>' attribute
	 * @see StepMode
	 * @see #isSetStepMode()
	 * @see #unsetStepMode()
	 * @see #getStepMode()
	 */
	void setStepMode(StepMode value);

	/**
	 * Puts the step mode back to the default value and marks the attribute as not
	 * set.
	 * <p>
	 * Side effects: the serializer writes no <code>StepMode</code> element for a
	 * series whose attribute is not set.
	 *
	 * @see #isSetStepMode()
	 * @see #getStepMode()
	 * @see #setStepMode(StepMode)
	 */
	void unsetStepMode();

	/**
	 * Tells whether a caller has set the step mode.
	 * <p>
	 * Non-obvious behaviour: the serializer writes the <code>StepMode</code> element
	 * only when this method returns <code>true</code>.
	 *
	 * @return <code>true</code> when the '<em>Step Mode</em>' attribute is set
	 * @see #unsetStepMode()
	 * @see #getStepMode()
	 * @see #setStepMode(StepMode)
	 */
	boolean isSetStepMode();

	/**
	 * Returns a copy of this series.
	 * <p>
	 * Intent: this declaration narrows the return type of
	 * {@link LineSeries#copyInstance()}. The chart engine copies the design series
	 * into a runtime series for every render.
	 * <p>
	 * Constraints: the implementation must return a piecewise constant series. A
	 * missing override returns a plain {@link LineSeries}, and then the chart engine
	 * draws a straight line without a report of an error.
	 *
	 * @return a copy of this series, with the step mode and its set flag
	 */
	@Override
	PiecewiseConstantSeries copyInstance();
}
