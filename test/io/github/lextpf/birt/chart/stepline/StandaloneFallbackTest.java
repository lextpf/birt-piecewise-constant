/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import org.eclipse.birt.chart.datafeed.IDataSetProcessor;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.render.BaseRenderer;
import org.eclipse.birt.chart.util.PluginSettings;
import org.eclipse.birt.core.framework.PlatformConfig;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;
import io.github.lextpf.birt.chart.stepline.model.type.impl.StepLineSeriesImpl;
import io.github.lextpf.birt.chart.stepline.render.StepLine;
import io.github.lextpf.birt.chart.stepline.test.CapturingPngRenderer;
import io.github.lextpf.birt.chart.stepline.test.CapturingPngRenderer.Seg;
import io.github.lextpf.birt.chart.stepline.test.ChartFixtures;
import io.github.lextpf.birt.chart.stepline.test.ChartFixtures.Options;

/**
 * Proves that {@link StepLineSetup#registerStandalone()} makes the step line
 * work in a chart engine that has no extension registry at all.
 * <p>
 * The test deliberately does <em>not</em> use
 * {@code ChartPlatformExtension}: it creates the {@link PluginSettings}
 * singleton itself with the {@code STANDALONE} flag set, which makes
 * {@link PluginSettings#inEclipseEnv()} return {@code false} for the rest of
 * the JVM's life, so every lookup goes through the hard-coded arrays instead of
 * {@code plugin.xml}. Surefire forks one JVM per test class, so that decision
 * cannot leak into another test class - and no other test class can have
 * created the singleton first.
 * <p>
 * The methods are ordered because they tell one story in one JVM: what the
 * standalone engine looks like <em>before</em> registration has to be observed
 * before registration happens.
 */
@TestMethodOrder(OrderAnnotation.class)
class StandaloneFallbackTest {

	/** Three points on categories A, B, C - two steps, four segments. */
	private static final Double[] VALUES = { 5.0, 9.0, 3.0 };

	private static PluginSettings settings;

	/**
	 * Creates the {@link PluginSettings} singleton in standalone mode, before
	 * anything else in this JVM can create it with a platform behind it.
	 */
	@BeforeAll
	static void createStandalonePluginSettings() {
		PlatformConfig config = new PlatformConfig();
		config.setProperty(PluginSettings.PROP_STANDALONE, "true");
		settings = PluginSettings.instance(config);
	}

	@Test
	@Order(1)
	void aStandaloneEngineKnowsNothingAboutTheStepLineSeries() throws Exception {
		assertFalse(settings.inEclipseEnv(), "the STANDALONE flag must switch the extension registry off");

		assertNull(settings.getRenderer(StepLineSeriesImpl.class),
				"an unregistered series must not resolve to a renderer in standalone mode");
		assertNull(EPackage.Registry.INSTANCE.getEPackage(StepLinePackage.eNS_URI),
				"nothing may have registered our EMF package yet");
	}

	@Test
	@Order(2)
	void registerStandaloneIsIdempotent() {
		StepLineSetup.registerStandalone();
		StepLineSetup.registerStandalone();

		assertEquals(1, registeredSeriesEntries(StepLineSeriesImpl.class.getName()),
				"registerStandalone() must append to PluginSettings' series array exactly once");
	}

	@Test
	@Order(3)
	void afterRegistrationTheStandaloneEngineResolvesRendererProcessorAndPackage() throws Exception {
		BaseRenderer renderer = settings.getRenderer(StepLineSeriesImpl.class);
		assertNotNull(renderer, "the step line renderer must resolve in standalone mode");
		assertSame(StepLine.class, renderer.getClass());

		IDataSetProcessor processor = settings.getDataSetProcessor(StepLineSeriesImpl.class);
		assertNotNull(processor, "the step line data set processor must resolve in standalone mode");
		assertEquals("org.eclipse.birt.chart.extension.datafeed.DataSetProcessorImpl", processor.getClass().getName());

		assertSame(StepLinePackage.eINSTANCE, EPackage.Registry.INSTANCE.getEPackage(StepLinePackage.eNS_URI),
				"registerStandalone() must have put our EMF package into the global registry");
	}

	@Test
	@Order(4)
	void aStandaloneEngineRendersTheStaircase() throws Exception {
		Options options = new Options().markersVisible(false);
		ChartWithAxes chart = ChartFixtures.stepLineChart(StepMode.AFTER_LITERAL, VALUES, options);

		CapturingPngRenderer device = new CapturingPngRenderer();
		File png = new File(CapturingPngRenderer.outputDirectory(), "standalone-after.png");
		List<Seg> segments = CapturingPngRenderer.render(chart, png, device);

		assertEquals(4, segments.size(), "three points in AFTER mode make two treads and two risers");
		for (int i = 0; i < segments.size(); i++) {
			Seg segment = segments.get(i);
			assertTrue((segment.x1() == segment.x2()) ^ (segment.y1() == segment.y2()),
					"segment " + i + " is not axis parallel: " + segment);
			if (i > 0) {
				Seg previous = segments.get(i - 1);
				assertEquals(previous.x2(), segment.x1(), "segment " + i + " does not start where segment "
						+ (i - 1) + " ended");
				assertEquals(previous.y2(), segment.y1(), "segment " + i + " does not start where segment "
						+ (i - 1) + " ended");
			}
		}
		assertTrue(png.isFile(), "the standalone render must have produced a PNG");
	}

	/**
	 * Counts how often a series class appears in {@code PluginSettings}' private
	 * standalone lookup array.
	 * <p>
	 * {@code registerSeriesRenderer} appends unconditionally and every lookup
	 * stops at the first match, so a double registration is invisible through the
	 * public API - the array itself is the only place where the guard in
	 * {@link StepLineSetup} can be observed.
	 *
	 * @param seriesClassName the fully qualified series implementation class name
	 * @return the number of entries naming that class
	 */
	private static int registeredSeriesEntries(String seriesClassName) {
		try {
			Field field = PluginSettings.class.getDeclaredField("saSeries");
			field.setAccessible(true);
			String[] series = assertInstanceOf(String[].class, field.get(null));
			int count = 0;
			for (String entry : series) {
				if (seriesClassName.equals(entry)) {
					count++;
				}
			}
			return count;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("PluginSettings.saSeries is no longer readable", e);
		}
	}
}
