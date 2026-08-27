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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.eclipse.birt.chart.datafeed.IDataSetProcessor;
import org.eclipse.birt.chart.extension.datafeed.DataSetProcessorImpl;
import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.util.ChartDynamicExtension;
import org.eclipse.birt.chart.render.BaseRenderer;
import org.eclipse.birt.chart.util.PluginSettings;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl;
import io.github.lextpf.birt.chart.piecewiseconstant.render.PiecewiseConstantLine;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartPlatformExtension;

/**
 * Verifies that the running BIRT platform finds everything this bundle's
 * {@code plugin.xml} registers.
 */
@ExtendWith(ChartPlatformExtension.class)
class PluginDiscoveryTest {

	@Test
	void rendererExtensionResolvesToPiecewiseConstantLine() throws Exception {
		BaseRenderer renderer = PluginSettings.instance().getRenderer(PiecewiseConstantSeriesImpl.class);

		assertNotNull(renderer, "no modelRenderer registered for PiecewiseConstantSeriesImpl");
		assertSame(PiecewiseConstantLine.class, renderer.getClass());
	}

	@Test
	void dataSetProcessorExtensionResolvesToTheStockProcessor() throws Exception {
		IDataSetProcessor processor = PluginSettings.instance().getDataSetProcessor(PiecewiseConstantSeriesImpl.class);

		assertNotNull(processor, "no datasetProcessor registered for PiecewiseConstantSeriesImpl");
		assertSame(DataSetProcessorImpl.class, processor.getClass());
	}

	@Test
	void modelLoaderRegistersThePackageUnderItsNamespace() throws Exception {
		Map<String, Object> packages = PluginSettings.instance().getExtChartModelPackages();

		assertTrue(packages.containsKey(PiecewiseConstantPackage.eNS_URI),
				"charttypes extension did not contribute " + PiecewiseConstantPackage.eNS_URI + ", got " + packages.keySet());
		assertSame(PiecewiseConstantPackage.eINSTANCE, packages.get(PiecewiseConstantPackage.eNS_URI));
		assertSame(PiecewiseConstantPackage.eINSTANCE, EPackage.Registry.INSTANCE.get(PiecewiseConstantPackage.eNS_URI));
	}

	@Test
	void createIsAStaticFactoryThatMarksStepModeAsSet() throws Exception {
		Method create = PiecewiseConstantSeriesImpl.class.getMethod("create");
		assertTrue(Modifier.isStatic(create.getModifiers()), "PiecewiseConstantSeriesImpl.create() must be static");

		Object created = create.invoke(null);

		PiecewiseConstantSeries series = assertInstanceOf(PiecewiseConstantSeries.class, created);
		assertTrue(series.isSetStepMode(), "create() must set the stepMode feature");
		assertEquals(StepMode.AFTER_LITERAL, series.getStepMode());
	}

	@Test
	void createdSeriesIsRecognisedAsAnExtendedModelElement() {
		Series series = PiecewiseConstantSeriesImpl.create();

		assertSame(PiecewiseConstantPackage.Literals.PIECEWISE_CONSTANT_SERIES, series.eClass());
		assertTrue(ChartDynamicExtension.isExtended(series),
				"ChartDynamicExtension did not pick up the piecewise constant EClass");
	}

	@Test
	void theSeriesDescribesItselfByModeAndDisplayName() {
		PiecewiseConstantSeries series = (PiecewiseConstantSeries) PiecewiseConstantSeriesImpl.create();

		assertEquals("Piecewise Constant Series", series.getDisplayName());
		assertTrue(series.toString().endsWith("(stepMode: After)"), series.toString());

		series.unsetStepMode();

		assertFalse(series.isSetStepMode());
		assertEquals(StepMode.AFTER_LITERAL, series.getStepMode());
		assertTrue(series.toString().endsWith("(stepMode: <unset>)"), series.toString());
	}
}
