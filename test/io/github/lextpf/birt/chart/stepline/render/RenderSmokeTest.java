/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline.render;

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

import io.github.lextpf.birt.chart.stepline.model.type.StepMode;
import io.github.lextpf.birt.chart.stepline.test.CapturingPngRenderer;
import io.github.lextpf.birt.chart.stepline.test.ChartFixtures;
import io.github.lextpf.birt.chart.stepline.test.ChartFixtures.Options;
import io.github.lextpf.birt.chart.stepline.test.ChartPlatformExtension;

/**
 * Renders every step-line variant through the real, registry-provided devices
 * and writes the results to <code>build/test-output</code>.
 * <p>
 * Unlike {@link StepLineGeometryTest} this does not construct the device class
 * directly: the devices come from the extension registry under their
 * <code>dv.PNG</code> / <code>dv.SVG</code> ids, which is how an embedding
 * application obtains them - so this also proves that the step series survives
 * a device it knows nothing about. The assertions are deliberately shallow (a
 * non-empty file, a drawn path in the SVG, and a transparent background in the
 * PNG); the images themselves are the artefact worth looking at.
 */
@ExtendWith(ChartPlatformExtension.class)
class RenderSmokeTest {

	private static final Double[] VALUES = { 5.0, 9.0, 3.0 };

	private static final Double[] SECOND_VALUES = { 3.0, 4.0, 2.0 };

	@Test
	void everyStepModeRendersToPng() throws Exception {
		renderPng(ChartFixtures.stepLineChart(StepMode.AFTER_LITERAL, VALUES, new Options()), "step-after.png");
		renderPng(ChartFixtures.stepLineChart(StepMode.BEFORE_LITERAL, VALUES, new Options()), "step-before.png");
		renderPng(ChartFixtures.stepLineChart(StepMode.CENTER_LITERAL, VALUES, new Options()), "step-center.png");
	}

	@Test
	void everyStepModeRendersTransposedToPng() throws Exception {
		Options transposed = new Options().transposed(true);
		renderPng(ChartFixtures.stepLineChart(StepMode.AFTER_LITERAL, VALUES, transposed),
				"step-after-transposed.png");
		renderPng(ChartFixtures.stepLineChart(StepMode.BEFORE_LITERAL, VALUES, transposed),
				"step-before-transposed.png");
		renderPng(ChartFixtures.stepLineChart(StepMode.CENTER_LITERAL, VALUES, transposed),
				"step-center-transposed.png");
	}

	@Test
	void missingValuesRenderToPng() throws Exception {
		Double[] values = { 5.0, null, 3.0 };
		renderPng(ChartFixtures.stepLineChart(StepMode.AFTER_LITERAL, values,
				new Options().connectMissingValue(false)), "step-null-gap.png");
		renderPng(ChartFixtures.stepLineChart(StepMode.AFTER_LITERAL, values,
				new Options().connectMissingValue(true)), "step-null-connected.png");
	}

	@Test
	void stackedSeriesRenderToPng() throws Exception {
		renderPng(ChartFixtures.stepLineChart(StepMode.AFTER_LITERAL, VALUES,
				new Options().stacked(true).secondValues(SECOND_VALUES)), "step-stacked.png");
	}

	@Test
	void theRenderedPngKeepsItsTransparentBackground() throws Exception {
		File png = renderPng(ChartFixtures.stepLineChart(StepMode.AFTER_LITERAL, VALUES, new Options()),
				"step-after.png");

		BufferedImage image = ImageIO.read(png);

		assertNotNull(image, png + " is not a readable image");
		assertTrue(image.getColorModel().hasAlpha(),
				"BIRT's PNG device writes ARGB, so the image must carry an alpha channel");
		assertEquals(0, image.getRGB(2, 2) >>> 24,
				"the chart background must be fully transparent so the image works on a dark page too");
		assertTrue(drawnPixels(image) > 0, "the plot area is empty - nothing was drawn on the transparent ground");
	}

	@Test
	void aStepLineRendersToSvg() throws Exception {
		File svg = render(ChartFixtures.stepLineChart(StepMode.AFTER_LITERAL, VALUES, new Options()), "dv.SVG",
				"step-after.svg");

		String document = Files.readString(svg.toPath(), StandardCharsets.UTF_8);
		assertTrue(document.contains("<path"), "BIRT's SVG device draws lines as <path> elements");
	}

	private static File renderPng(ChartWithAxes chart, String fileName) throws Exception {
		return render(chart, "dv.PNG", fileName);
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
		device.setProperty(IDeviceRenderer.FILE_IDENTIFIER, out.getAbsolutePath());

		RunTimeContext rtc = new RunTimeContext();
		rtc.setULocale(ULocale.ENGLISH);

		Generator generator = Generator.instance();
		GeneratedChartState state = generator.build(device.getDisplayServer(), chart,
				BoundsImpl.create(0, 0, 600, 400), null, rtc, null);
		generator.render(device, state);

		assertTrue(out.isFile(), fileName + " was not written");
		assertTrue(out.length() > 0, fileName + " is empty");
		return out;
	}
}
