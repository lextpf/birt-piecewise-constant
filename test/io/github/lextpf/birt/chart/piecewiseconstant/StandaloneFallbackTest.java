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

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl;
import io.github.lextpf.birt.chart.piecewiseconstant.render.PiecewiseConstantLine;
import io.github.lextpf.birt.chart.piecewiseconstant.test.CapturingPngRenderer;
import io.github.lextpf.birt.chart.piecewiseconstant.test.CapturingPngRenderer.Seg;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartFixtures;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartFixtures.Options;

/**
 * Proves that {@link PiecewiseConstantSetup#registerStandalone()} makes the
 * piecewise constant series work in a standalone chart engine, which has no
 * extension registry.
 * <p>
 * Constraints: this class must not use {@code ChartPlatformExtension}. It
 * creates the {@link PluginSettings} singleton itself with the
 * {@code STANDALONE} flag, and it must do so before any other class touches
 * that singleton. The flag makes {@link PluginSettings#inEclipseEnv()} return
 * {@code false} for the life of the JVM, so every lookup reads the hard-coded
 * arrays and not {@code plugin.xml}. Surefire forks one JVM per test class, so
 * no other test class creates the singleton first, and the flag reaches no
 * other test class.
 * <p>
 * Non-obvious behaviour: the test methods run in a fixed order, because they
 * tell one story in one JVM. A test must observe the standalone chart engine
 * before the registration, and the registration happens in a later test.
 */
@TestMethodOrder(OrderAnnotation.class)
class StandaloneFallbackTest {

	// The line makes two steps and has four segments.
	private static final Double[] VALUES = { 5.0, 9.0, 3.0 };

	private static PluginSettings settings;

	@BeforeAll
	static void createStandalonePluginSettings() {
		PlatformConfig config = new PlatformConfig();
		config.setProperty(PluginSettings.PROP_STANDALONE, "true");
		settings = PluginSettings.instance(config);
	}

	@Test
	@Order(1)
	void aStandaloneEngineKnowsNothingAboutThePiecewiseConstantSeries() throws Exception {
		assertFalse(settings.inEclipseEnv(), "the STANDALONE flag must switch the extension registry off");

		assertNull(settings.getRenderer(PiecewiseConstantSeriesImpl.class),
				"a series that nothing registered must not resolve to a renderer in the standalone chart engine");
		assertNull(EPackage.Registry.INSTANCE.getEPackage(PiecewiseConstantPackage.eNS_URI),
				"nothing must have registered the EMF package of this plug-in yet");
	}

	@Test
	@Order(2)
	void registerStandaloneIsIdempotent() {
		PiecewiseConstantSetup.registerStandalone();
		PiecewiseConstantSetup.registerStandalone();

		assertEquals(1, registeredSeriesEntries(PiecewiseConstantSeriesImpl.class.getName()),
				"registerStandalone() must append to PluginSettings' series array exactly once");
	}

	@Test
	@Order(3)
	void afterRegistrationTheStandaloneEngineResolvesRendererProcessorAndPackage() throws Exception {
		BaseRenderer renderer = settings.getRenderer(PiecewiseConstantSeriesImpl.class);
		assertNotNull(renderer, "the piecewise constant renderer must resolve in the standalone chart engine");
		assertSame(PiecewiseConstantLine.class, renderer.getClass());

		IDataSetProcessor processor = settings.getDataSetProcessor(PiecewiseConstantSeriesImpl.class);
		assertNotNull(processor,
				"the piecewise constant data set processor must resolve in the standalone chart engine");
		assertEquals("org.eclipse.birt.chart.extension.datafeed.DataSetProcessorImpl", processor.getClass().getName());

		assertSame(PiecewiseConstantPackage.eINSTANCE,
				EPackage.Registry.INSTANCE.getEPackage(PiecewiseConstantPackage.eNS_URI),
				"registerStandalone() must put the EMF package of this plug-in into the global registry");
	}

	@Test
	@Order(4)
	void aStandaloneEngineRendersTheStaircase() throws Exception {
		Options options = new Options().markersVisible(false);
		ChartWithAxes chart = ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES, options);

		CapturingPngRenderer device = new CapturingPngRenderer();
		File png = new File(CapturingPngRenderer.outputDirectory(), "piecewise-constant-standalone.png");
		List<Seg> segments = CapturingPngRenderer.render(chart, png, device);

		assertEquals(4, segments.size(), "three data points in AFTER mode make two treads and two steps");
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
		assertTrue(png.isFile(), "the standalone chart engine must have written a PNG file");
	}

	/**
	 * Counts the entries for one series class in the private standalone lookup
	 * array of {@code PluginSettings}.
	 * <p>
	 * Non-obvious behaviour: {@code registerSeriesRenderer} appends without a
	 * check, and every lookup stops at the first match. A second registration is
	 * therefore invisible through the public API. The array is the only place
	 * where a test can observe the guard in {@link PiecewiseConstantSetup}.
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
