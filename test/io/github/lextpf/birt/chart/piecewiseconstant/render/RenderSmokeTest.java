/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import org.eclipse.birt.chart.api.ChartEngine;
import org.eclipse.birt.chart.device.IDeviceRenderer;
import org.eclipse.birt.chart.factory.GeneratedChartState;
import org.eclipse.birt.chart.factory.Generator;
import org.eclipse.birt.chart.factory.RunTimeContext;
import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.impl.BoundsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.ibm.icu.util.ULocale;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;
import io.github.lextpf.birt.chart.piecewiseconstant.test.CapturingPngRenderer;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartFixtures;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartFixtures.Options;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartPlatformExtension;

/**
 * Renders every variant of the piecewise constant series through the devices of
 * the extension registry, and writes the files to
 * <code>build/test-output</code>.
 * <p>
 * Intent: {@link PiecewiseConstantGeometryTest} constructs its device class
 * directly. This class takes the devices from the extension registry under
 * their <code>dv.PNG</code> and <code>dv.SVG</code> ids, which is the way an
 * application gets them.
 * <p>
 * Non-obvious behaviour: the assertions are shallow on purpose.
 * The images are the result that a
 * reader must look at.
 */
@ExtendWith(ChartPlatformExtension.class)
class RenderSmokeTest {

	private static final Double[] VALUES = { 5.0, 9.0, 3.0 };

	private static final Double[] SECOND_VALUES = { 3.0, 4.0, 2.0 };

	/** The chart bounds that the tests give to the generator, in points. */
	private static final int CHART_WIDTH_POINTS = 600;

	private static final int CHART_HEIGHT_POINTS = 400;

	/**
	 * The factor that {@link CapturingPngRenderer#EXPORT_DPI} applies to those
	 * bounds.
	 *
	 * @see CapturingPngRenderer#EXPORT_DPI
	 */
	private static final int EXPORT_SCALE = CapturingPngRenderer.EXPORT_DPI / 72;

	@Test
	void everyStepModeRendersToPng() throws Exception {
		renderPng(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES,
				titled("Piecewise constant · After")), "piecewise-constant-after.png");
		renderPng(ChartFixtures.piecewiseConstantChart(StepMode.BEFORE_LITERAL, VALUES,
				titled("Piecewise constant · Before")), "piecewise-constant-before.png");
		renderPng(ChartFixtures.piecewiseConstantChart(StepMode.CENTER_LITERAL, VALUES,
				titled("Piecewise constant · Center")), "piecewise-constant-center.png");
	}

	@Test
	void everyStepModeRendersTransposedToPng() throws Exception {
		renderPng(
				ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES,
						titled("Piecewise constant · After (transposed)").transposed(true)),
				"piecewise-constant-after-transposed.png");
		renderPng(
				ChartFixtures.piecewiseConstantChart(StepMode.BEFORE_LITERAL, VALUES,
						titled("Piecewise constant · Before (transposed)").transposed(true)),
				"piecewise-constant-before-transposed.png");
		renderPng(
				ChartFixtures.piecewiseConstantChart(StepMode.CENTER_LITERAL, VALUES,
						titled("Piecewise constant · Center (transposed)").transposed(true)),
				"piecewise-constant-center-transposed.png");
	}

	@Test
	void missingValuesRenderToPng() throws Exception {
		Double[] values = { 5.0, null, 3.0 };
		renderPng(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, values,
				titled("Piecewise constant · null gap").connectMissingValue(false)), "piecewise-constant-null-gap.png");
		renderPng(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, values,
				titled("Piecewise constant · null bridged").connectMissingValue(true)),
				"piecewise-constant-null-connected.png");
	}

	@Test
	void stackedSeriesRenderToPng() throws Exception {
		renderPng(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES,
				titled("Piecewise constant · stacked").stacked(true).secondValues(SECOND_VALUES)),
				"piecewise-constant-stacked.png");
	}

	@Test
	void theRenderedPngKeepsItsTransparentBackground() throws Exception {
		File png = renderPng(ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES,
				titled("Piecewise constant · After")), "piecewise-constant-after.png");

		BufferedImage image = ImageIO.read(png);

		assertNotNull(image, png + " is not a readable image");
		assertTrue(image.getColorModel().hasAlpha(),
				"the PNG device writes ARGB, so the image must carry an alpha channel");
		assertEquals(0, image.getRGB(2, 2) >>> 24,
				"the chart background must be fully transparent, so that the image also works on a dark page");
		assertTrue(drawnPixels(image) > 0, "the plot area is empty: the chart drew nothing on the transparent ground");
		assertEquals(CHART_WIDTH_POINTS * EXPORT_SCALE, image.getWidth(),
				"the device must apply IDeviceRenderer.DPI_RESOLUTION and render at the export resolution");
		assertEquals(CHART_HEIGHT_POINTS * EXPORT_SCALE, image.getHeight(),
				"the device must apply IDeviceRenderer.DPI_RESOLUTION and render at the export resolution");
	}

	@Test
	void aPiecewiseConstantLineRendersToSvg() throws Exception {
		File svg = render(
				ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES,
						titled("Piecewise constant · After")),
				"dv.SVG", "piecewise-constant-after.svg");

		String document = Files.readString(svg.toPath(), StandardCharsets.UTF_8);
		assertTrue(document.contains("<path"), "the SVG device draws a line as a <path> element");
	}

	private static File renderPng(ChartWithAxes chart, String fileName) throws Exception {
		return render(chart, "dv.PNG", fileName);
	}

	private static Options titled(String caption) {
		return new Options().title(caption);
	}

	/**
	 * Counts the pixels that the chart drew inside the plot area.
	 */
	private static int drawnPixels(BufferedImage image) {
		int drawn = 0;
		for (int y = image.getHeight() / 4; y < image.getHeight() * 3 / 4; y++) {
			for (int x = image.getWidth() / 4; x < image.getWidth() * 3 / 4; x++) {
				if ((image.getRGB(x, y) >>> 24) != 0) {
					drawn++;
				}
			}
		}
		return drawn;
	}

	/**
	 * Renders one chart with a device of the extension registry.
	 */
	private static File render(Chart chart, String deviceId, String fileName) throws Exception {
		File out = new File(CapturingPngRenderer.outputDirectory(), fileName);

		IDeviceRenderer device = ChartEngine.instance().getRenderer(deviceId);
		CapturingPngRenderer.configureExport(device, out);

		RunTimeContext rtc = new RunTimeContext();
		rtc.setULocale(ULocale.ENGLISH);

		Generator generator = Generator.instance();
		GeneratedChartState state = generator.build(device.getDisplayServer(), chart,
				BoundsImpl.create(0, 0, CHART_WIDTH_POINTS, CHART_HEIGHT_POINTS), null, rtc, null);
		generator.render(device, state);

		assertTrue(out.isFile(), "the device did not write " + fileName);
		assertTrue(out.length() > 0, fileName + " is empty");
		return out;
	}
}
