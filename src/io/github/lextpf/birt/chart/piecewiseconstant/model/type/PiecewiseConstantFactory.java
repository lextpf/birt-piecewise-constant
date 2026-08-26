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

import org.eclipse.emf.ecore.EFactory;

/**
 * The EMF <b>Factory</b> of the piecewise constant model.
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage
 */
public interface PiecewiseConstantFactory extends EFactory {

	/**
	 * The single instance of the factory. The first read of this field returns the
	 * registered factory of the package, or builds a new factory.
	 */
	PiecewiseConstantFactory eINSTANCE =
			io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantFactoryImpl.init();

	/**
	 * Returns a new '<em>Piecewise Constant Series</em>'.
	 * <p>
	 * Non-obvious behaviour: the new series carries no values and no set flags. A
	 * caller that needs the values of a stock series calls
	 * {@code PiecewiseConstantSeriesImpl.create()} instead.
	 */
	PiecewiseConstantSeries createPiecewiseConstantSeries();

	PiecewiseConstantPackage getPiecewiseConstantPackage();
}
