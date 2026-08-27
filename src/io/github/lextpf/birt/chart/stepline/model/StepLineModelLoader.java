/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline.model;

import org.eclipse.birt.chart.model.IExtChartModelLoader;
import org.eclipse.emf.ecore.EPackage;

import io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage;

/**
 * Hands the step line EMF package to the chart engine. Instantiated by BIRT
 * through the <code>org.eclipse.birt.chart.engine.charttypes</code> extension
 * point, so it needs a public no-argument constructor.
 */
public class StepLineModelLoader implements IExtChartModelLoader {

	/**
	 * Creates the loader.
	 */
	public StepLineModelLoader() {
		super();
	}

	@Override
	public EPackage getChartTypePackage() {
		return StepLinePackage.eINSTANCE;
	}
}
