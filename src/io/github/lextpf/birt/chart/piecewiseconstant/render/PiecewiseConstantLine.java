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

import org.eclipse.birt.chart.computation.DataPointHints;
import org.eclipse.birt.chart.device.IPrimitiveRenderer;
import org.eclipse.birt.chart.exception.ChartException;
import org.eclipse.birt.chart.extension.render.Line;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.Fill;
import org.eclipse.birt.chart.model.attribute.LineAttributes;
import org.eclipse.birt.chart.model.attribute.Location;
import org.eclipse.birt.chart.model.attribute.Location3D;
import org.eclipse.birt.chart.model.layout.Plot;
import org.eclipse.birt.chart.model.type.LineSeries;
import org.eclipse.birt.chart.render.ISeriesRenderingHints;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;

/**
 * Draws a {@link PiecewiseConstantSeries} as a piecewise constant line.
 * <p>
 * Intent: the stock {@link Line} renderer draws a straight segment between two
 * consecutive entries of its {@code Location[]} array. This renderer inserts one
 * corner vertex per step into that array. The segments then form the treads and
 * the steps of the piecewise constant line. This renderer draws nothing itself.
 * It intercepts the four hooks that receive the laid out device locations,
 * expands the arrays with {@link PiecewiseConstantExpander} and calls
 * {@code super}.
 * <p>
 * Constraints: the chart engine creates this renderer through the
 * {@code org.eclipse.birt.chart.engine.modelrenderers} extension point. The
 * class must therefore keep a public constructor without arguments.
 * <p>
 * Non-obvious behaviour: the stock renderer indexes the {@code Location[]} array
 * with the index of the {@code DataPointHints[]} array. Both arrays must
 * therefore keep the same length and the same index meaning. Every corner vertex
 * reuses the {@link DataPointHints} of the data point whose value it carries.
 * The tooltip and the interactivity of a tread therefore belong to that data
 * point.
 * <p>
 * Non-obvious behaviour: {@code Line.renderSeries} draws the markers and the
 * data point labels from the original arrays, so they stay on the real data
 * points. The shadow, the legend graphic and the 3D plane sorting also keep
 * their stock behaviour, because the expansion keeps the two arrays aligned.
 */
public class PiecewiseConstantLine extends Line {

	public PiecewiseConstantLine() {
		super();
	}

	// This override replaces the laid out vertices with the vertices of the piecewise constant line.
	@Override
	protected void renderDataPoints(IPrimitiveRenderer ipr, Plot p, ISeriesRenderingHints srh, DataPointHints[] dpha,
			LineAttributes lia, Location[] loa, boolean bShowAsTape, double dTapeWidth, Fill paletteEntry,
			boolean usePaletteLineColor) throws ChartException {
		Expanded e = expand(dpha, loa);
		super.renderDataPoints(ipr, p, srh, e.dpha, lia, e.loa, bShowAsTape, dTapeWidth, paletteEntry,
				usePaletteLineColor);
	}

	// The shadow follows the line, so this override expands the arrays in the same way as renderDataPoints.
	@Override
	protected void renderShadow(IPrimitiveRenderer ipr, Plot p, LineAttributes lia, Location[] loa, boolean bShowAsTape,
			DataPointHints[] dpha) throws ChartException {
		Expanded e = expand(dpha, loa);
		super.renderShadow(ipr, p, lia, e.loa, bShowAsTape, e.dpha);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Intent: a piecewise constant series is constant between its data points. A
	 * series with {@code isCurve()} set must therefore still show treads and
	 * steps, and not a spline through its data points.
	 * <p>
	 * Non-obvious behaviour: {@code Line.renderSeries} calls this hook instead of
	 * {@link #renderDataPoints} when {@code isCurve()} is true. This override
	 * sends the call back to {@link #renderDataPoints}. {@code Line} passes the
	 * {@link Plot} argument on to nobody, so {@code null} is safe here.
	 */
	@Override
	protected void renderAsCurve(IPrimitiveRenderer ipr, LineAttributes lia, ISeriesRenderingHints srh, Location[] loa,
			boolean bShowAsTape, double tapeWidth, Fill paletteEntry, boolean usePaletteLineColor)
			throws ChartException {
		renderDataPoints(ipr, null, srh, srh.getDataPoints(), lia, loa, bShowAsTape, tapeWidth, paletteEntry,
				usePaletteLineColor);
	}

	// The shadow follows the line, so this hook takes the same path as renderAsCurve.
	@Override
	protected void renderShadowAsCurve(IPrimitiveRenderer ipr, LineAttributes lia, ISeriesRenderingHints srh,
			Location[] loa, boolean bShowAsTape, double tapeWidth) throws ChartException {
		renderShadow(ipr, null, lia, loa, bShowAsTape, srh.getDataPoints());
	}

	/**
	 * Builds the vertices of the piecewise constant line from a laid out point
	 * list.
	 */
	private Expanded expand(DataPointHints[] dpha, Location[] loa) {
		boolean is3D = isDimension3D();
		// The chart engine never transposes a 3D chart. A transposed 2D chart swaps
		// the device coordinates of the category axis and the value axis.
		// Line.Transposition hides that swap behind getX, getY and set.
		boolean transposed = !is3D && ((ChartWithAxes) getModel()).isTransposed();
		Transposition t = transposed ? Transposition.TRANSPOSED : Transposition.NOT_TRANSPOSED;

		int n = loa.length;
		double[] base = new double[n];
		double[] value = new double[n];
		double[] z = is3D ? new double[n] : null;
		boolean[] isNull = new boolean[n];
		for (int i = 0; i < n; i++) {
			base[i] = t.getX(loa[i]);
			value[i] = t.getY(loa[i]);
			if (is3D) {
				z[i] = ((Location3D) loa[i]).getZ();
			}
			isNull[i] = isNaN(dpha[i].getOrthogonalValue());
		}

		LineSeries series = (LineSeries) getSeries();
		StepMode mode = series instanceof PiecewiseConstantSeries step ? step.getStepMode() : StepMode.AFTER_LITERAL;
		// The expander inserts a corner vertex only between two data points that the
		// renderer connects. It must therefore receive the same connectMissingValue
		// flag that the chart engine gives to the data point seeker of the stock
		// renderer.
		PiecewiseConstantExpansion x = PiecewiseConstantExpander.expand(base, value, isNull, mode,
				series.isConnectMissingValue());

		int m = x.owner.length;
		// Line.LineDataPointsRenderer3D casts the whole array to Location3D[]. In 3D
		// the type of the array itself, and not only the type of its elements, must
		// therefore be Location3D[].
		Location[] out = is3D ? new Location3D[m] : new Location[m];
		DataPointHints[] hints = new DataPointHints[m];
		for (int k = 0; k < m; k++) {
			int owner = x.owner[k];
			hints[k] = dpha[owner];
			if (x.real[k]) {
				out[k] = loa[owner];
			} else if (is3D) {
				out[k] = goFactory.createLocation3D(x.base[k], x.value[k], z[owner]);
			} else {
				Location corner = goFactory.createLocation(0, 0);
				t.set(corner, x.base[k], x.value[k]);
				out[k] = corner;
			}
		}

		return new Expanded(hints, out);
	}

	/**
	 * The two arrays that {@link PiecewiseConstantLine#expand} returns.
	 */
	private record Expanded(DataPointHints[] dpha, Location[] loa) {
	}
}
