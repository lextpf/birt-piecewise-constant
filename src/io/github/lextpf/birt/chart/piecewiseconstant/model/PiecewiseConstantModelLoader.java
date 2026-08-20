/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant.model;

import org.eclipse.birt.chart.model.IExtChartModelLoader;
import org.eclipse.emf.ecore.EPackage;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage;

/**
 * Hands the EMF package of the piecewise constant series to the chart engine.
 * <p>
 * Intent: the chart engine needs this package to read and write the series in
 * the chart XML. BIRT collects the package through the
 * <code>org.eclipse.birt.chart.engine.charttypes</code> extension point.
 * <p>
 * Constraints: BIRT builds this class through the extension point, so the class
 * must stay public and must keep a public constructor without arguments. The
 * class name and the namespace URI must stay in the <code>charttypes</code>
 * extension of <code>plugin.xml</code>.
 * <p>
 * Non-obvious behaviour: BIRT's {@code SerializerImpl} reads the
 * <code>charttypes</code> extensions in its static initializer, and it caches
 * the list of packages once for the whole JVM. If {@code SerializerImpl} loads
 * before {@code Platform.startup}, then the cache stays empty for the life of
 * the JVM. The chart XML then loses the piecewise constant series. The caller
 * must therefore start the platform before the first use of the serializer.
 */
public class PiecewiseConstantModelLoader implements IExtChartModelLoader {

	/**
	 * Builds the loader. BIRT needs a public constructor without arguments here.
	 */
	public PiecewiseConstantModelLoader() {
		super();
	}

	/**
	 * Returns the EMF package of the piecewise constant model.
	 * <p>
	 * Side effects: the first call builds the package and puts it into the global
	 * {@code EPackage.Registry.INSTANCE}.
	 *
	 * @return the piecewise constant package
	 */
	@Override
	public EPackage getChartTypePackage() {
		return PiecewiseConstantPackage.eINSTANCE;
	}
}
