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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.data.NumberDataSet;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.impl.SerializerImpl;
import org.eclipse.birt.chart.model.type.LineSeries;
import org.eclipse.birt.chart.model.util.ChartDefaultValueUtil;
import org.eclipse.birt.chart.render.BaseRenderer;
import org.eclipse.birt.chart.util.PluginSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage;
import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;
import io.github.lextpf.birt.chart.stepline.model.type.impl.StepLineSeriesImpl;
import io.github.lextpf.birt.chart.stepline.render.StepLine;
import io.github.lextpf.birt.chart.stepline.test.ChartFixtures;
import io.github.lextpf.birt.chart.stepline.test.ChartPlatformExtension;

/**
 * Verifies that a chart carrying a {@link StepLineSeries} survives BIRT's
 * serializer and EMF's copy machinery unchanged.
 */
@ExtendWith(ChartPlatformExtension.class)
class SerializerRoundTripTest {

	private static ChartWithAxes beforeModeChart() {
		return ChartFixtures.stepLineChart(StepMode.BEFORE_LITERAL, false, ChartFixtures.defaultValues());
	}

	private static String write(Chart chart) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		SerializerImpl.instance().write(chart, bos);
		return bos.toString(StandardCharsets.UTF_8);
	}

	private static Chart read(String xml) throws IOException {
		return SerializerImpl.instance().read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static SeriesDefinition valueSeriesDefinition(Chart chart) {
		return ((ChartWithAxes) chart).getPrimaryBaseAxes()[0].getAssociatedAxes().get(0).getSeriesDefinitions().get(0);
	}

	private static Series valueSeries(Chart chart) {
		return valueSeriesDefinition(chart).getSeries().get(0);
	}

	@Test
	void writesTheExtensionNamespaceAndStepModeAsAnElement() throws IOException {
		String xml = write(beforeModeChart());

		assertTrue(xml.contains(StepLinePackage.eNS_URI), "extension namespace missing from:\n" + xml);
		assertTrue(xml.contains("xsi:type=\"stepline:StepLineSeries\""), "xsi:type missing from:\n" + xml);
		assertTrue(xml.contains("<StepMode>Before</StepMode>"), "StepMode element missing from:\n" + xml);
		assertFalse(xml.contains("StepMode=\""), "StepMode must be an element, not an attribute:\n" + xml);
		assertFalse(xml.contains("stepMode=\""), "StepMode must be an element, not an attribute:\n" + xml);
	}

	@Test
	void readsTheStepLineSeriesBackWithItsMode() throws IOException {
		Chart back = read(write(beforeModeChart()));

		StepLineSeries series = assertInstanceOf(StepLineSeries.class, valueSeries(back));
		assertEquals(StepMode.BEFORE_LITERAL, series.getStepMode());
		assertTrue(series.isSetStepMode());
	}

	@Test
	void chartCopyInstanceKeepsTheSeriesTypeAndMode() {
		ChartWithAxes copy = beforeModeChart().copyInstance();

		StepLineSeries series = assertInstanceOf(StepLineSeries.class, valueSeries(copy));
		assertSame(StepLineSeriesImpl.class, series.getClass());
		assertEquals(StepMode.BEFORE_LITERAL, series.getStepMode());
	}

	@Test
	void seriesCopyInstanceKeepsTheModeAndItsSetFlag() {
		StepLineSeries series = (StepLineSeries) valueSeries(beforeModeChart());

		StepLineSeries copy = series.copyInstance();

		assertSame(StepLineSeriesImpl.class, copy.getClass());
		assertEquals(StepMode.BEFORE_LITERAL, copy.getStepMode());
		assertTrue(copy.isSetStepMode());
	}

	@Test
	void aSeriesFromCreateDefaultSerialisesWithoutAStepModeElement() throws IOException {
		ChartWithAxes chart = beforeModeChart();
		SeriesDefinition definition = valueSeriesDefinition(chart);
		NumberDataSet values = NumberDataSetImpl.create(ChartFixtures.defaultValues());
		Series unsetSeries = StepLineSeriesImpl.createDefault();
		unsetSeries.setDataSet(values);
		unsetSeries.setSeriesIdentifier("step");
		definition.getSeries().clear();
		definition.getSeries().add(unsetSeries);

		String xml = write(chart);

		assertTrue(xml.contains("xsi:type=\"stepline:StepLineSeries\""), "xsi:type missing from:\n" + xml);
		assertFalse(xml.contains("StepMode"), "createDefault() must leave stepMode unset:\n" + xml);
		assertFalse(((StepLineSeries) unsetSeries).isSetStepMode());
		assertEquals(StepMode.AFTER_LITERAL, ((StepLineSeries) unsetSeries).getStepMode());
	}

	@Test
	void theDeserialisedSeriesClassStillResolvesToTheStepLineRenderer() throws Exception {
		Chart back = read(write(beforeModeChart()));

		BaseRenderer renderer = PluginSettings.instance().getRenderer(valueSeries(back).getClass());

		assertSame(StepLine.class, renderer.getClass());
	}

	@Test
	void chartDefaultValueUtilDoesNotYieldStepLineDefaults() {
		Series series = valueSeries(beforeModeChart());

		Series defaults = ChartDefaultValueUtil.getDefaultSeries(series);

		// Documents the pitfall: StepLineSeries is a LineSeries, so BIRT hands back
		// stock line-series defaults and never a StepLineSeries.
		assertInstanceOf(LineSeries.class, defaults);
		assertFalse(defaults instanceof StepLineSeries,
				"getDefaultSeries() must not be used to obtain step line defaults");
	}
}
