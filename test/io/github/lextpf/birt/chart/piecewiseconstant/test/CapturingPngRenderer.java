/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.chart.device.IDeviceRenderer;
import org.eclipse.birt.chart.device.image.PngRendererImpl;
import org.eclipse.birt.chart.event.LineRenderEvent;
import org.eclipse.birt.chart.event.OvalRenderEvent;
import org.eclipse.birt.chart.event.StructureSource;
import org.eclipse.birt.chart.event.StructureType;
import org.eclipse.birt.chart.exception.ChartException;
import org.eclipse.birt.chart.factory.GeneratedChartState;
import org.eclipse.birt.chart.factory.Generator;
import org.eclipse.birt.chart.factory.RunTimeContext;
import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.attribute.LineAttributes;
import org.eclipse.birt.chart.model.attribute.impl.BoundsImpl;

import com.ibm.icu.util.ULocale;

/**
 * A real PNG device that additionally records every series line segment the
 * chart engine draws, and counts the ovals it draws for isolated data points -
 * the geometric oracle the piecewise-constant tests assert against.
 * <p>
 * Every line of a chart, including the ones of the series, ends up in
 * {@code drawLine}, and BIRT tags each render event with a
 * {@link StructureSource} saying which part of the chart it belongs to. Series
 * line segments carry {@link StructureType#SERIES} with the series itself as
 * the source object, axes and grid lines carry {@link StructureType#AXIS}, and
 * a data point that has no neighbour to connect to is drawn as an oval tagged
 * {@link StructureType#SERIES_DATA_POINT}. Filtering on those types isolates
 * exactly the series geometry.
 * <p>
 * {@code DeferredCache.flushLines} replays the cached line events in insertion
 * order, so {@link #segments} is in the order the renderer's data point seeker
 * produced them.
 */
public class CapturingPngRenderer extends PngRendererImpl {

	/** A captured line segment in device coordinates. */
	public record Seg(double x1, double y1, double x2, double y2) {

		/** @return the start point as <code>{x, y}</code>. */
		public double[] start() {
			return new double[] { x1, y1 };
		}

		/** @return the end point as <code>{x, y}</code>. */
		public double[] end() {
			return new double[] { x2, y2 };
		}
	}

	/** Every series line segment, in the order it was drawn. */
	public final List<Seg> segments = new ArrayList<>();

	/**
	 * The series each entry of {@link #segments} belongs to; index aligned with
	 * {@link #segments}.
	 */
	private final List<Object> segmentSeries = new ArrayList<>();

	/**
	 * The number of ovals <em>stroked</em> for a series data point - which is the
	 * number of isolated data points, the ones that have no neighbour to connect
	 * to and are therefore drawn as a dot instead of a line.
	 * <p>
	 * The two are the same thing because of how BIRT emits the two kinds of oval a
	 * series can produce. An isolated point comes out of
	 * {@code Line.LineDataPointsRenderer2D.drawSinglePoint} as a bare
	 * {@code drawOval} whose outline is the series' own line attributes - it is a
	 * stroke and nothing else. A data point <em>marker</em> comes out of
	 * {@code MarkerRenderer} as a {@code fillOval} plus a {@code drawOval} whose
	 * outline is the marker's, and the fixtures switch that outline off, so the
	 * marker is filled but never stroked. {@code G2dRendererBase.drawOval} agrees:
	 * it returns without touching the canvas when the outline is invisible.
	 * Counting only the ovals that really get a stroke therefore counts isolated
	 * points and no markers, whatever shape the markers happen to be - a
	 * distinction that did not matter while they were boxes, which are drawn as
	 * polygons, and does now that they are circles.
	 */
	public int seriesPointOvals;

	/**
	 * The resolution every test render is exported at, in dots per inch - four
	 * times the 72 dpi a chart model is measured in, three times the 96 dpi a
	 * device falls back to.
	 * <p>
	 * BIRT lays a chart out in points and only scales on the way to the device:
	 * {@code Generator.render} hands the device an {@code EXPECTED_BOUNDS} scaled
	 * by {@code dpi / 72}, so the 600x400 pt chart becomes a 2400x1600 px image.
	 * Text and markers follow that factor -
	 * {@code G2dDisplayServerBase.createFont} multiplies the point size by it and
	 * {@code MarkerRenderer} multiplies the marker size by it - so they are
	 * re-rendered at the higher resolution rather than upsampled. Anti-aliasing is
	 * on regardless: {@code G2dRendererBase.prepareGraphicsContext} sets both the
	 * shape and the text hint.
	 * <p>
	 * Line thickness does <em>not</em> follow. {@code Generator.render} does walk
	 * the model multiplying every {@code LineAttributes} by the integer scale, but
	 * it does so on a copy it makes for itself, while the series renderers were
	 * handed the original model back in {@code Generator.build} and read their
	 * thickness from that - so every stroke stays as many device pixels wide as
	 * the model says, however high the resolution goes.
	 */
	public static final int EXPORT_DPI = 288;

	/**
	 * Points the device at an output file and puts it into export resolution.
	 * <p>
	 * The order matters. {@code SwingDisplayServer.getDpiResolution} caches the
	 * resolution the first time it is asked for and only consults the value passed
	 * here while that cache is still empty, so the property has to be set before
	 * anything - {@code Generator.build} above all - reads the display server.
	 *
	 * @param device the device to configure
	 * @param out    the file the device writes to
	 */
	public static void configureExport(IDeviceRenderer device, File out) {
		device.setProperty(IDeviceRenderer.FILE_IDENTIFIER, out.getAbsolutePath());
		device.setProperty(IDeviceRenderer.DPI_RESOLUTION, Integer.valueOf(EXPORT_DPI));
	}

	@Override
	public void drawLine(LineRenderEvent lre) throws ChartException {
		if (lre.getSource() instanceof StructureSource ss && ss.getType() == StructureType.SERIES) {
			segments.add(new Seg(lre.getStart().getX(), lre.getStart().getY(), lre.getEnd().getX(),
					lre.getEnd().getY()));
			segmentSeries.add(ss.getSource());
		}
		super.drawLine(lre);
	}

	@Override
	public void drawOval(OvalRenderEvent ore) throws ChartException {
		if (ore.getSource() instanceof StructureSource ss && ss.getType() == StructureType.SERIES_DATA_POINT
				&& isStroked(ore)) {
			seriesPointOvals++;
		}
		super.drawOval(ore);
	}

	/**
	 * @param ore an oval the chart engine is about to draw
	 * @return whether the device will actually put a stroke on the canvas for it -
	 *         the same precondition {@code G2dRendererBase.drawOval} checks before
	 *         it does any drawing at all
	 */
	private static boolean isStroked(OvalRenderEvent ore) {
		LineAttributes outline = ore.getOutline();
		return outline != null && outline.isVisible();
	}

	/**
	 * Splits {@link #segments} per series, without needing to know the series
	 * objects: segments are grouped by the identity of their source object, and the
	 * groups come in the order their series was first drawn.
	 *
	 * @return one segment list per series that drew at least one segment
	 */
	public List<List<Seg>> segmentGroups() {
		List<Object> order = new ArrayList<>();
		List<List<Seg>> groups = new ArrayList<>();
		for (int i = 0; i < segments.size(); i++) {
			int group = -1;
			for (int g = 0; g < order.size(); g++) {
				if (order.get(g) == segmentSeries.get(i)) {
					group = g;
					break;
				}
			}
			if (group < 0) {
				order.add(segmentSeries.get(i));
				groups.add(new ArrayList<>());
				group = groups.size() - 1;
			}
			groups.get(group).add(segments.get(i));
		}
		return groups;
	}

	/**
	 * Builds and renders a chart into a PNG file and returns the series segments
	 * that were drawn on the way.
	 *
	 * @param chart  the chart model to render
	 * @param pngOut the PNG file the device writes to
	 * @param idr    the capturing device to render with
	 * @return {@link #segments} of <code>idr</code>
	 * @throws ChartException if the chart engine fails
	 */
	public static List<Seg> render(Chart chart, File pngOut, CapturingPngRenderer idr) throws ChartException {
		configureExport(idr, pngOut);

		RunTimeContext rtc = new RunTimeContext();
		rtc.setULocale(ULocale.ENGLISH);

		Generator generator = Generator.instance();
		GeneratedChartState gcs = generator.build(idr.getDisplayServer(), chart, BoundsImpl.create(0, 0, 600, 400), null,
				rtc, null);
		generator.render(idr, gcs);

		return idr.segments;
	}

	/**
	 * @return the directory named by the <code>piecewiseconstant.test.out</code> system
	 *         property, created if it does not exist yet
	 */
	public static File outputDirectory() {
		String configured = System.getProperty("piecewiseconstant.test.out"); //$NON-NLS-1$
		File dir = configured != null ? new File(configured) : new File("test-output"); //$NON-NLS-1$
		if (!dir.isDirectory() && !dir.mkdirs()) {
			throw new IllegalStateException("could not create the test output directory " + dir);
		}
		return dir;
	}
}
