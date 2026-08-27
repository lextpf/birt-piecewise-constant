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

import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;

/**
 * Renders a
 * {@link io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries} as a
 * piecewise-constant (staircase) line.
 * <p>
 * Registered through the
 * <code>org.eclipse.birt.chart.engine.modelrenderers</code> extension point, so
 * it needs a public no-argument constructor.
 * <p>
 * The renderer draws nothing itself. All the stock {@link Line} renderer needs
 * to draw a staircase is a longer point list, so this class only intercepts the
 * four hooks that receive the already laid out device locations, inserts the
 * staircase corners with {@link StepPointExpander} and hands the longer arrays
 * back to <code>super</code>. Everything else - markers, labels, shadows,
 * tooltips, the legend graphic, 3D plane sorting - keeps working unchanged,
 * because the expansion keeps the two arrays in lockstep: the expanded
 * <code>Location[]</code> and <code>DataPointHints[]</code> have the same
 * length and the same index meaning, and every synthetic corner reuses the
 * {@link DataPointHints} of the real data point it belongs to. The stock inner
 * renderers only ever index <code>loa[]</code> by a <code>dpha[]</code> index
 * and never look at the rendering hints object, so longer index aligned arrays
 * are all they need.
 */
public class StepLine extends Line {

	/**
	 * Creates the renderer.
	 */
	public StepLine() {
		super();
	}

	@Override
	protected void renderDataPoints(IPrimitiveRenderer ipr, Plot p, ISeriesRenderingHints srh, DataPointHints[] dpha,
			LineAttributes lia, Location[] loa, boolean bShowAsTape, double dTapeWidth, Fill paletteEntry,
			boolean usePaletteLineColor) throws ChartException {
		Expanded e = expand(dpha, loa);
		super.renderDataPoints(ipr, p, srh, e.dpha, lia, e.loa, bShowAsTape, dTapeWidth, paletteEntry,
				usePaletteLineColor);
	}

	@Override
	protected void renderShadow(IPrimitiveRenderer ipr, Plot p, LineAttributes lia, Location[] loa, boolean bShowAsTape,
			DataPointHints[] dpha) throws ChartException {
		Expanded e = expand(dpha, loa);
		super.renderShadow(ipr, p, lia, e.loa, bShowAsTape, e.dpha);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A step line is piecewise constant by definition, so a series with
	 * <code>curve</code> set must still be drawn as a staircase rather than as a
	 * spline through its points. The curve path is therefore bypassed and routed
	 * back into the ordinary data point rendering. <code>Line</code> passes the
	 * {@link Plot} on to nobody, so <code>null</code> is safe here.
	 */
	@Override
	protected void renderAsCurve(IPrimitiveRenderer ipr, LineAttributes lia, ISeriesRenderingHints srh, Location[] loa,
			boolean bShowAsTape, double tapeWidth, Fill paletteEntry, boolean usePaletteLineColor)
			throws ChartException {
		renderDataPoints(ipr, null, srh, srh.getDataPoints(), lia, loa, bShowAsTape, tapeWidth, paletteEntry,
				usePaletteLineColor);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The shadow follows the line, so it takes the same bypass as
	 * {@link #renderAsCurve}.
	 */
	@Override
	protected void renderShadowAsCurve(IPrimitiveRenderer ipr, LineAttributes lia, ISeriesRenderingHints srh,
			Location[] loa, boolean bShowAsTape, double tapeWidth) throws ChartException {
		renderShadow(ipr, null, lia, loa, bShowAsTape, srh.getDataPoints());
	}

	/**
	 * Inserts the staircase corners into a laid out point list.
	 *
	 * @param dpha the data point hints, one per original point
	 * @param loa  the device locations, index aligned with <code>dpha</code>
	 * @return the expanded, still index aligned pair of arrays
	 */
	private Expanded expand(DataPointHints[] dpha, Location[] loa) {
		boolean is3D = isDimension3D();
		// A 3D chart is never transposed; a transposed 2D chart swaps the roles of
		// the two device coordinates, which is exactly what Line.Transposition
		// abstracts over.
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
		StepMode mode = series instanceof StepLineSeries step ? step.getStepMode() : StepMode.AFTER_LITERAL;
		// The expander only emits a corner between points that will actually be
		// connected, so it has to be told the very flag the stock renderer's data
		// point seeker is created with.
		StepExpansion x = StepPointExpander.expand(base, value, isNull, mode, series.isConnectMissingValue());

		int m = x.owner.length;
		// LineDataPointsRenderer3D hard casts the array to Location3D[], so in 3D the
		// array itself - not only its elements - has to have that type.
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
	 * The two index aligned arrays {@link StepLine#expand} produces.
	 *
	 * @param dpha the data point hints, one per staircase vertex
	 * @param loa  the device locations of the staircase vertices
	 */
	private record Expanded(DataPointHints[] dpha, Location[] loa) {
	}
}
