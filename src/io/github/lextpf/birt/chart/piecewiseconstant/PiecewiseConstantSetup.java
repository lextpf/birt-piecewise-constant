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
 * Intent: the plug-in normally needs no setup code. Both regular BIRT
 * environments find the plug-in through the {@code plugin.xml} at the root of
 * the jar:
 * <ul>
 * <li><b>The OSGi runtime</b> (the report designer, the OSGi report engine):
 * Equinox installs the plug-in and reads its {@code modelrenderers},
 * {@code datasetprocessors} and {@code charttypes} extensions from the
 * registry.</li>
 * <li><b>The POJO runtime</b> ({@code Platform.startup} or
 * {@code ChartEngine.instance(new PlatformConfig())}, the {@code ReportEngine}
 * of the {@code birt-runtime} distribution): BIRT's {@code ServiceLauncher}
 * scans the classpath for {@code plugin.xml} files. The user puts the jar into
 * {@code ReportEngine/lib}, or onto the classpath.</li>
 * </ul>
 * <p>
 * Only a third environment needs this class: the standalone chart engine. The
 * caller starts it with the {@code STANDALONE} flag
 * ({@code config.setProperty(PluginSettings.PROP_STANDALONE, "true")}), and the
 * chart engine then skips {@code Platform.startup}. There
 * {@link PluginSettings#inEclipseEnv()} returns {@code false}, and every lookup
 * goes against hard-coded arrays of the series types of BIRT. The call
 * {@code getRenderer(PiecewiseConstantSeriesImpl.class)} therefore returns
 * {@code null}, and the chart engine cannot draw the chart.
 * <p>
 * Constraints: the caller calls {@link #registerStandalone()} once. The call
 * must come after the chart engine builds the {@link PluginSettings} singleton,
 * and before the chart engine builds the first chart.
 * <p>
 * Side effects: {@link #registerStandalone()} appends the piecewise constant
 * series to the arrays of {@link PluginSettings}. It also puts the EMF package
 * into {@link org.eclipse.emf.ecore.EPackage.Registry#INSTANCE}. The serializer
 * needs that package to read and write
 * {@code xsi:type="piecewise:PiecewiseConstantSeries"}.
 * <p>
 * Non-obvious behaviour: the call is safe in every environment. The OSGi runtime
 * and the POJO runtime resolve through {@code plugin.xml}, so they never read
 * the extra array entries.
 */
public final class PiecewiseConstantSetup {

	/**
	 * The stock data set processor of BIRT. The piecewise constant series stores
	 * exactly what a {@code LineSeries} stores, so it uses the processor of the
	 * built-in line series.
	 * <p>
	 * Constraints: this name must stay equal to the {@code processor} of the
	 * {@code datasetprocessors} extension in {@code plugin.xml}.
	 */
	private static final String DATA_SET_PROCESSOR = "org.eclipse.birt.chart.extension.datafeed.DataSetProcessorImpl"; //$NON-NLS-1$

	/**
	 * The guard against a second registration.
	 * {@code PluginSettings.registerSeriesRenderer} appends to its arrays on every
	 * call, so a second call makes the arrays grow without a limit.
	 */
	private static boolean registered;

	/**
	 * Holds static members only. No caller builds this class.
	 */
	private PiecewiseConstantSetup() {
	}

	/**
	 * Makes the piecewise constant series known to a standalone chart engine.
	 * <p>
	 * Intent: the method registers the EMF package, the renderer and the data set
	 * processor of the series.
	 * <p>
	 * Constraints: the caller must call this method after the {@link PluginSettings}
	 * singleton exists. That is, after {@code ChartEngine.instance(config)} or
	 * {@code PluginSettings.instance(config)} with the {@code STANDALONE} property.
	 * The caller must also call it before the chart engine builds the first chart.
	 * <p>
	 * Side effects: the method appends the series to the global arrays of
	 * {@link PluginSettings}, and it puts the EMF package into
	 * {@code EPackage.Registry.INSTANCE}.
	 * <p>
	 * Non-obvious behaviour: the method is synchronized, and a second call does
	 * nothing.
	 */
	public static synchronized void registerStandalone() {
		if (registered) {
			return;
		}

		// A read of eINSTANCE runs PiecewiseConstantPackageImpl.init(). That method
		// registers the package under its namespace URI in EPackage.Registry.INSTANCE.
		PiecewiseConstantPackage.eINSTANCE.eClass();

		PluginSettings.instance().registerSeriesRenderer(PiecewiseConstantSeriesImpl.class.getName(),
				DATA_SET_PROCESSOR, PiecewiseConstantLine.class.getName());

		registered = true;
	}
}
