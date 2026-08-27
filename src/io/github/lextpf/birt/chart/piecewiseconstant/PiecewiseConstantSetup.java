/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant;

import org.eclipse.birt.chart.util.PluginSettings;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl;
import io.github.lextpf.birt.chart.piecewiseconstant.render.PiecewiseConstantLine;

/**
 * Registers the piecewise constant series with a chart engine that has no
 * extension registry.
 * <p>
 * The bundle normally needs no setup code at all. Both of BIRT's regular
 * environments discover it through the {@code plugin.xml} at the root of the
 * jar:
 * <ul>
 * <li><b>OSGi</b> (the Designer, the OSGi report engine): the bundle is
 * installed and its {@code modelrenderers}, {@code datasetprocessors} and
 * {@code charttypes} extensions are read from the Equinox registry.</li>
 * <li><b>The POJO runtime</b> ({@code Platform.startup} /
 * {@code ChartEngine.instance(new PlatformConfig())}, the {@code ReportEngine}
 * of the {@code birt-runtime} distribution): BIRT's {@code ServiceLauncher}
 * scans the classpath for {@code plugin.xml} files, so dropping the jar into
 * {@code ReportEngine/lib} - or just onto the classpath - is enough.</li>
 * </ul>
 * <p>
 * It is only the third environment that needs this class: a chart engine
 * created with the {@code STANDALONE} flag
 * ({@code config.setProperty(PluginSettings.PROP_STANDALONE, "true")}), which
 * skips {@code Platform.startup} entirely. There
 * {@link PluginSettings#inEclipseEnv()} is {@code false} and every lookup goes
 * against hard-coded arrays of the series types BIRT ships with, so
 * {@code getRenderer(PiecewiseConstantSeriesImpl.class)} returns {@code null}
 * and the chart cannot be drawn. Calling {@link #registerStandalone()} once -
 * after the {@link PluginSettings} singleton has been created and before the
 * first chart is built - appends the piecewise constant series to those arrays
 * and puts the EMF package into
 * {@link org.eclipse.emf.ecore.EPackage.Registry#INSTANCE}, which is what the
 * serializer needs to read and write
 * {@code xsi:type="piecewise:PiecewiseConstantSeries"}.
 * <p>
 * The call is idempotent and safe to make in any environment: under OSGi and in
 * the POJO runtime the extra array entries are simply never consulted, because
 * those environments resolve through {@code plugin.xml}.
 */
public final class PiecewiseConstantSetup {

	/**
	 * BIRT's stock data set processor; the piecewise constant series stores
	 * exactly what a {@code LineSeries} stores, so it feeds on the same one the
	 * built-in line series uses.
	 */
	private static final String DATA_SET_PROCESSOR = "org.eclipse.birt.chart.extension.datafeed.DataSetProcessorImpl"; //$NON-NLS-1$

	/**
	 * Guards against a second registration:
	 * {@code PluginSettings.registerSeriesRenderer} appends to its arrays
	 * unconditionally, so calling it twice would grow them without end.
	 */
	private static boolean registered;

	private PiecewiseConstantSetup() {
	}

	/**
	 * Makes the piecewise constant series known to a standalone chart engine:
	 * registers the EMF package and the renderer/data set processor pair.
	 * <p>
	 * Call this after the {@link PluginSettings} singleton exists - that is, after
	 * {@code ChartEngine.instance(config)} or {@code PluginSettings.instance(config)}
	 * with the {@code STANDALONE} property - and before the first chart is built.
	 * Repeated calls do nothing.
	 */
	public static synchronized void registerStandalone() {
		if (registered) {
			return;
		}

		// Touching eINSTANCE runs PiecewiseConstantPackageImpl.init(), which
		// registers the package under its nsURI in EPackage.Registry.INSTANCE.
		PiecewiseConstantPackage.eINSTANCE.eClass();

		PluginSettings.instance().registerSeriesRenderer(PiecewiseConstantSeriesImpl.class.getName(),
				DATA_SET_PROCESSOR, PiecewiseConstantLine.class.getName());

		registered = true;
	}
}
