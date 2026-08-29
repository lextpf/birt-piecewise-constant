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
 * A PNG device that also records every line segment of a value series, and
 * counts the ovals the chart engine draws for isolated data points.
 * <p>
 * Non-obvious behaviour: every line of a chart reaches {@code drawLine}. The
 * chart engine tags each render event with a {@link StructureSource}. That
 * source names the part of the chart the event belongs to. A line segment of a
 * value series carries {@link StructureType#SERIES} with the series as the
 * source object. An axis line and a grid line carry {@link StructureType#AXIS}.
 * A data point that has no neighbour to connect to is an isolated data point.
 * The chart engine draws that data point as an oval that carries
 * {@link StructureType#SERIES_DATA_POINT}. A filter on those types therefore
 * selects the geometry of the value series and nothing else.
 * <p>
 * {@code DeferredCache.flushLines} replays the cached line events in insertion
 * order. {@link #segments} is therefore in the order the data point seeker of
 * the renderer produced them.
 */
public class CapturingPngRenderer extends PngRendererImpl {

	/** One recorded line segment, in device coordinates. */
	public record Seg(double x1, double y1, double x2, double y2) {

		public double[] start() {
			return new double[] { x1, y1 };
		}

		public double[] end() {
			return new double[] { x2, y2 };
		}
	}

	/** Every line segment of a value series, in the order the device drew it. */
	public final List<Seg> segments = new ArrayList<>();

	// The series that each entry of segments belongs to.
	private final List<Object> segmentSeries = new ArrayList<>();

	/**
	 * The number of ovals the device strokes for a data point of a value series.
	 * That number is the number of isolated data points, which are the data points
	 * that have no neighbour to connect to.
	 * <p>
	 * Non-obvious behaviour: a value series produces two kinds of oval, and only
	 * one kind gets a stroke. An isolated data point comes from
	 * {@code Line.LineDataPointsRenderer2D.drawSinglePoint} as a plain
	 * {@code drawOval}. The outline of that oval is the line attributes of the
	 * series, so the chart engine strokes it. A marker comes from
	 * {@code MarkerRenderer} as a {@code fillOval} plus a {@code drawOval} whose
	 * outline is the outline of the marker. The fixtures make that outline
	 * invisible, so the chart engine fills the marker but never strokes it.
	 * {@code G2dRendererBase.drawOval} returns without any drawing if the outline
	 * is invisible. A count of the stroked ovals therefore counts isolated data
	 * points and no marker, for every marker shape.
	 */
	public int seriesPointOvals;

	/**
	 * The export resolution of every test render, in dots per inch. The value is
	 * four times the 72 dots per inch of a chart model. It is three times the 96
	 * dots per inch that a device falls back to.
	 * <p>
	 * Non-obvious behaviour: the chart engine lays a chart out in points. It scales
	 * the layout only when it passes the bounds to the device.
	 * {@code Generator.render} gives the device an {@code EXPECTED_BOUNDS} that it
	 * scales by {@code dpi / 72}. The
	 * 600x400 point chart therefore becomes a 2400x1600 pixel image. Text and
	 * markers follow that factor, because
	 * {@code G2dDisplayServerBase.createFont} multiplies the point size by it and
	 * {@code MarkerRenderer} multiplies the marker size by it.
	 * <p>
	 * Line thickness does not follow that factor. {@code Generator.render} walks
	 * the model and multiplies every {@code LineAttributes} thickness by the
	 * integer scale, but it does that on a copy of the model. The series renderers
	 * received the original model in {@code Generator.build} and read the
	 * thickness from it. Every stroke therefore stays as many device pixels wide
	 * as the model states, at every resolution.
	 */
	public static final int EXPORT_DPI = 288;

	/**
	 * Names the output file of the device and sets the export resolution.
	 * <p>
	 * Constraints: the caller must call this method before the chart engine reads
	 * the display server, and {@code Generator.build} reads it.
	 * {@code SwingDisplayServer.getDpiResolution} caches the resolution on the
	 * first call, and it reads the value set here only while that cache is empty.
	 */
	public static void configureExport(IDeviceRenderer device, File out) {
		device.setProperty(IDeviceRenderer.FILE_IDENTIFIER, out.getAbsolutePath());
		device.setProperty(IDeviceRenderer.DPI_RESOLUTION, Integer.valueOf(EXPORT_DPI));
	}

	// Records the line if it belongs to a value series, and then draws it.
	@Override
	public void drawLine(LineRenderEvent lre) throws ChartException {
		if (lre.getSource() instanceof StructureSource ss && ss.getType() == StructureType.SERIES) {
			segments.add(new Seg(lre.getStart().getX(), lre.getStart().getY(), lre.getEnd().getX(),
					lre.getEnd().getY()));
			segmentSeries.add(ss.getSource());
		}
		super.drawLine(lre);
	}

	// Counts the oval if it belongs to a data point of a value series and the device strokes it, and then draws it.
	@Override
	public void drawOval(OvalRenderEvent ore) throws ChartException {
		if (ore.getSource() instanceof StructureSource ss && ss.getType() == StructureType.SERIES_DATA_POINT
				&& isStroked(ore)) {
			seriesPointOvals++;
		}
		super.drawOval(ore);
	}

	// Reports whether the device puts a stroke on the canvas for one oval.
	private static boolean isStroked(OvalRenderEvent ore) {
		LineAttributes outline = ore.getOutline();
		return outline != null && outline.isVisible();
	}

	/**
	 * Splits {@link #segments} into one list per value series.
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
	 * Builds a chart, renders it into a PNG file and returns the recorded
	 * segments.
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
	 * Returns the directory that every test writes its output files to.
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
