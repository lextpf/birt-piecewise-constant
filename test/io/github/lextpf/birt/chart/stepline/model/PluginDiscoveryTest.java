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

import io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage;
import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;
import io.github.lextpf.birt.chart.stepline.model.type.impl.StepLineSeriesImpl;
import io.github.lextpf.birt.chart.stepline.render.StepLine;
import io.github.lextpf.birt.chart.stepline.test.ChartPlatformExtension;

/**
 * Verifies that the running BIRT platform finds everything this bundle's
 * {@code plugin.xml} registers.
 */
@ExtendWith(ChartPlatformExtension.class)
class PluginDiscoveryTest {

	@Test
	void rendererExtensionResolvesToStepLine() throws Exception {
		BaseRenderer renderer = PluginSettings.instance().getRenderer(StepLineSeriesImpl.class);

		assertNotNull(renderer, "no modelRenderer registered for StepLineSeriesImpl");
		assertSame(StepLine.class, renderer.getClass());
	}

	@Test
	void dataSetProcessorExtensionResolvesToTheStockProcessor() throws Exception {
		IDataSetProcessor processor = PluginSettings.instance().getDataSetProcessor(StepLineSeriesImpl.class);

		assertNotNull(processor, "no datasetProcessor registered for StepLineSeriesImpl");
		assertSame(DataSetProcessorImpl.class, processor.getClass());
	}

	@Test
	void modelLoaderRegistersThePackageUnderItsNamespace() throws Exception {
		Map<String, Object> packages = PluginSettings.instance().getExtChartModelPackages();

		assertTrue(packages.containsKey(StepLinePackage.eNS_URI),
				"charttypes extension did not contribute " + StepLinePackage.eNS_URI + ", got " + packages.keySet());
		assertSame(StepLinePackage.eINSTANCE, packages.get(StepLinePackage.eNS_URI));
		assertSame(StepLinePackage.eINSTANCE, EPackage.Registry.INSTANCE.get(StepLinePackage.eNS_URI));
	}

	@Test
	void createIsAStaticFactoryThatMarksStepModeAsSet() throws Exception {
		Method create = StepLineSeriesImpl.class.getMethod("create");
		assertTrue(Modifier.isStatic(create.getModifiers()), "StepLineSeriesImpl.create() must be static");

		Object created = create.invoke(null);

		StepLineSeries series = assertInstanceOf(StepLineSeries.class, created);
		assertTrue(series.isSetStepMode(), "create() must set the stepMode feature");
		assertEquals(StepMode.AFTER_LITERAL, series.getStepMode());
	}

	@Test
	void createdSeriesIsRecognisedAsAnExtendedModelElement() {
		Series series = StepLineSeriesImpl.create();

		assertSame(StepLinePackage.Literals.STEP_LINE_SERIES, series.eClass());
		assertTrue(ChartDynamicExtension.isExtended(series),
				"ChartDynamicExtension did not pick up the step line EClass");
	}

	@Test
	void theSeriesDescribesItselfByModeAndDisplayName() {
		StepLineSeries series = (StepLineSeries) StepLineSeriesImpl.create();

		assertEquals("Step Line Series", series.getDisplayName());
		assertTrue(series.toString().endsWith("(stepMode: After)"), series.toString());

		series.unsetStepMode();

		assertFalse(series.isSetStepMode());
		assertEquals(StepMode.AFTER_LITERAL, series.getStepMode());
		assertTrue(series.toString().endsWith("(stepMode: <unset>)"), series.toString());
	}
}
