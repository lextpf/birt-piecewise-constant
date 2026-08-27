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

import org.eclipse.emf.ecore.EFactory;

/**
 * The <b>Factory</b> for the step line model. It provides a create method for
 * each non-abstract class of the model.
 *
 * @see io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage
 */
public interface StepLineFactory extends EFactory {

	/**
	 * The singleton instance of the factory.
	 */
	StepLineFactory eINSTANCE = io.github.lextpf.birt.chart.stepline.model.type.impl.StepLineFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Step Line Series</em>'.
	 *
	 * @return a new object of class '<em>Step Line Series</em>'
	 */
	StepLineSeries createStepLineSeries();

	/**
	 * Returns the package supported by this factory.
	 *
	 * @return the package supported by this factory
	 */
	StepLinePackage getStepLinePackage();
}
