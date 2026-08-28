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
 */
public interface PiecewiseConstantSeries extends LineSeries {

	/**
	 * Returns the step mode of this series.
	 */
	StepMode getStepMode();

	/**
	 * Sets the step mode of this series.
	 */
	void setStepMode(StepMode value);

	/**
	 * Puts the step mode back to the default value and marks the attribute as not
	 * set.
	 */
	void unsetStepMode();

	/**
	 * Tells whether a caller has set the step mode.
	 */
	boolean isSetStepMode();

	/**
	 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl#copyInstance()
	 */
	@Override
	PiecewiseConstantSeries copyInstance();
}
