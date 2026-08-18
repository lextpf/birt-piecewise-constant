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
 * Renders every piecewise constant variant through the real,
 * registry-provided devices and writes the results to
 * <code>build/test-output</code>.
 * <p>
 * Unlike {@link PiecewiseConstantGeometryTest} this does not construct the
 * device class directly: the devices come from the extension registry under
 * their <code>dv.PNG</code> / <code>dv.SVG</code> ids, which is how an embedding
 * application obtains them - so this also proves that the piecewise constant
 * series survives a device it knows nothing about. The assertions are deliberately shallow (a
 * non-empty file, a drawn path in the SVG, and a transparent background in the
 * PNG); the images themselves are the artefact worth looking at.
 */
@ExtendWith(ChartPlatformExtension.class)
class RenderSmokeTest {

	private static final Double[] VALUES = { 5.0, 9.0, 3.0 };

	private static final Double[] SECOND_VALUES = { 3.0, 4.0, 2.0 };

	/** The chart bounds handed to the generator, in points. */
	private static final int CHART_WIDTH_POINTS = 600;

	private static final int CHART_HEIGHT_POINTS = 400;

	/**
	 * What {@link CapturingPngRenderer#EXPORT_DPI} makes of those bounds: BIRT
	 * measures a chart in points (1/72 inch) and scales the device bounds by
	 * <code>dpi / 72</code> on the way to the device.
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
				"BIRT's PNG device writes ARGB, so the image must carry an alpha channel");
		assertEquals(0, image.getRGB(2, 2) >>> 24,
				"the chart background must be fully transparent so the image works on a dark page too");
		assertTrue(drawnPixels(image) > 0, "the plot area is empty - nothing was drawn on the transparent ground");
		assertEquals(CHART_WIDTH_POINTS * EXPORT_SCALE, image.getWidth(),
				"the device must have honoured IDeviceRenderer.DPI_RESOLUTION and rendered at export resolution");
		assertEquals(CHART_HEIGHT_POINTS * EXPORT_SCALE, image.getHeight(),
				"the device must have honoured IDeviceRenderer.DPI_RESOLUTION and rendered at export resolution");
	}

	@Test
	void aPiecewiseConstantLineRendersToSvg() throws Exception {
		File svg = render(
				ChartFixtures.piecewiseConstantChart(StepMode.AFTER_LITERAL, VALUES,
						titled("Piecewise constant · After")),
				"dv.SVG", "piecewise-constant-after.svg");

		String document = Files.readString(svg.toPath(), StandardCharsets.UTF_8);
		assertTrue(document.contains("<path"), "BIRT's SVG device draws lines as <path> elements");
	}

	private static File renderPng(ChartWithAxes chart, String fileName) throws Exception {
		return render(chart, "dv.PNG", fileName);
	}

	/**
	 * @param caption the title the rendered chart carries
	 * @return fresh options with that title
	 */
	private static Options titled(String caption) {
		return new Options().title(caption);
	}

	/**
	 * @param image a rendered chart
	 * @return the number of pixels in the middle half of the image - which is
	 *         inside the plot area - that are not fully transparent
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

		assertTrue(out.isFile(), fileName + " was not written");
		assertTrue(out.length() > 0, fileName + " is empty");
		return out;
	}
}
