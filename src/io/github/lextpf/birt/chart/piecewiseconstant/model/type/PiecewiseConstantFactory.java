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
 * The <b>Factory</b> for the piecewise constant model. It provides a create method for
 * each non-abstract class of the model.
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage
 */
public interface PiecewiseConstantFactory extends EFactory {

	/**
	 * The singleton instance of the factory.
	 */
	PiecewiseConstantFactory eINSTANCE =
			io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Piecewise Constant Series</em>'.
	 *
	 * @return a new object of class '<em>Piecewise Constant Series</em>'
	 */
	PiecewiseConstantSeries createPiecewiseConstantSeries();

	/**
	 * Returns the package supported by this factory.
	 *
	 * @return the package supported by this factory
	 */
	PiecewiseConstantPackage getPiecewiseConstantPackage();
}
