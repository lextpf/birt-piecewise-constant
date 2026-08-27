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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.Bounds;
import org.eclipse.birt.chart.model.component.Axis;
import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.impl.SerializerImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;
import io.github.lextpf.birt.chart.stepline.test.ChartPlatformExtension;

/**
 * Guards the sample report {@code stepline-sample.rptdesign} that
 * {@link RuntimeSmokeIT} feeds to a real BIRT runtime.
 * <p>
 * The runtime test only runs when somebody points {@code -Dbirt.runtime.dir} at
 * an unpacked distribution, so without this class the resource would be
 * unverified in every normal build. Parsing it here proves it is well formed
 * XML and wired up the way BIRT expects, and reading its embedded chart XML
 * back through {@code SerializerImpl} proves the payload really is a step line
 * series bound to the report's columns - a typo in the CDATA would otherwise
 * only show up on a machine that has the runtime.
 */
@ExtendWith(ChartPlatformExtension.class)
class SampleReportTest {

	private static final String SAMPLE_REPORT = "stepline-sample.rptdesign";

	private static Document report;

	@BeforeAll
	static void parseTheSampleReport() throws IOException, SAXException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		DocumentBuilder builder = factory.newDocumentBuilder();

		try (InputStream in = SampleReportTest.class.getClassLoader().getResourceAsStream(SAMPLE_REPORT)) {
			assertNotNull(in, SAMPLE_REPORT + " is missing from the test classpath");
			// A malformed resource throws SAXException here - that is the
			// well-formedness assertion.
			report = builder.parse(in);
		}
	}

	@Test
	void isABirtReportDesignWithAScriptedFiveRowDataSet() {
		Element root = report.getDocumentElement();
		assertEquals("report", root.getNodeName());
		assertEquals("http://www.eclipse.org/birt/2005/design", root.getAttribute("xmlns"));
		assertEquals("3.2.26", root.getAttribute("version"));

		Element dataSet = onlyElement("script-data-set");
		assertEquals("Step Line Data Set", dataSet.getAttribute("name"));

		assertEquals(List.of("category", "value"), columnNames(dataSet),
				"the data set must expose exactly the two columns the chart binds to");

		assertEquals(List.of("resultSetHints", "columnHints"), listPropertyNames(dataSet),
				"the ROM defines no \"resultSet\" on script-data-set - only oda-data-set and joint-data-set have one."
						+ " ListPropertyState skips the undefined-property error for that name but leaves the property"
						+ " definition null, so DesignReader dies with a NullPointerException in StructureState");

		String fetch = methodBody(dataSet, "fetch");
		assertTrue(fetch.contains("i < 5"), "the data set must produce five rows, but fetch reads:\n" + fetch);
		assertTrue(fetch.contains("row[\"category\"]") && fetch.contains("row[\"value\"]"),
				"fetch must fill both columns:\n" + fetch);
	}

	@Test
	void bindsTheChartItemToThatDataSet() {
		Element chartItem = onlyElement("extended-item");
		assertEquals("Chart", chartItem.getAttribute("extensionName"));
		assertEquals("Step Line Data Set", property(chartItem, "dataSet"));
		assertEquals("SVG", property(chartItem, "outputFormat"),
				"the runtime smoke test looks for SVG in the rendered HTML");

		List<String> bound = new ArrayList<>();
		for (Element structure : elements(chartItem.getElementsByTagName("structure"))) {
			bound.add(property(structure, "name"));
		}
		assertEquals(List.of("category", "value"), bound, "the chart must bind both data set columns");
	}

	@Test
	void carriesAStepLineSeriesInItsChartXml() {
		String chartXml = chartXml();

		assertTrue(chartXml.contains("xmlns:stepline=\"http://lextpf.github.io/birt/chart/StepLineModelType\""),
				"the chart XML does not declare our namespace:\n" + chartXml);
		assertTrue(chartXml.contains("xsi:type=\"stepline:StepLineSeries\""),
				"the chart XML holds no step line series:\n" + chartXml);
		assertTrue(chartXml.contains("<StepMode>After</StepMode>"),
				"the chart XML holds no step mode:\n" + chartXml);
	}

	@Test
	void thatChartXmlLoadsBackAsAnAfterModeSeriesBoundToTheReportColumns() throws IOException {
		Chart chart = SerializerImpl.instance()
				.read(new ByteArrayInputStream(chartXml().getBytes(StandardCharsets.UTF_8)));

		Axis xAxis = ((ChartWithAxes) chart).getPrimaryBaseAxes()[0];
		Series categorySeries = xAxis.getSeriesDefinitions().get(0).getSeries().get(0);
		Series valueSeries = ((ChartWithAxes) chart).getPrimaryOrthogonalAxis(xAxis).getSeriesDefinitions().get(0)
				.getSeries().get(0);

		StepLineSeries stepLine = assertInstanceOf(StepLineSeries.class, valueSeries,
				"the value series of the sample report is not a step line");
		assertEquals(StepMode.AFTER_LITERAL, stepLine.getStepMode());

		assertEquals("row[\"category\"]", categorySeries.getDataDefinition().get(0).getDefinition());
		assertEquals("row[\"value\"]", valueSeries.getDataDefinition().get(0).getDefinition());
		assertTrue(categorySeries.getDataSet() == null && valueSeries.getDataSet() == null,
				"a report chart takes its data from the report, not from an inline data set");

		// ChartReportItemPresentationBase.onRowSets returns null - an empty <div> in
		// the HTML, no error anywhere - when the outermost block is explicitly sized
		// 0 x 0, so the sample has to carry the item's size here.
		Bounds bounds = chart.getBlock().getBounds();
		assertEquals(400.0, bounds.getWidth(), 0.0, "the chart block must carry the report item's width");
		assertEquals(250.0, bounds.getHeight(), 0.0, "the chart block must carry the report item's height");
	}

	/** @return the raw chart XML out of the {@code xmlRepresentation} CDATA */
	private static String chartXml() {
		for (Element property : elements(report.getElementsByTagName("xml-property"))) {
			if ("xmlRepresentation".equals(property.getAttribute("name"))) {
				return property.getTextContent();
			}
		}
		throw new AssertionError("the sample report has no xmlRepresentation property");
	}

	private static Element onlyElement(String tagName) {
		List<Element> found = elements(report.getElementsByTagName(tagName));
		assertEquals(1, found.size(), "expected exactly one <" + tagName + "> in " + SAMPLE_REPORT);
		return found.get(0);
	}

	private static List<Element> elements(NodeList nodes) {
		List<Element> found = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i) instanceof Element element) {
				found.add(element);
			}
		}
		return found;
	}

	/** @return the value of a {@code <property name="..."/>} child */
	private static String property(Element parent, String name) {
		for (Element property : elements(parent.getChildNodes())) {
			if ("property".equals(property.getNodeName()) && name.equals(property.getAttribute("name"))) {
				return text(property);
			}
		}
		throw new AssertionError("no property named " + name + " on <" + parent.getNodeName() + ">");
	}

	/** @return the names of the {@code <list-property>} children, in document order */
	private static List<String> listPropertyNames(Element parent) {
		List<String> names = new ArrayList<>();
		for (Element list : elements(parent.getChildNodes())) {
			if ("list-property".equals(list.getNodeName())) {
				names.add(list.getAttribute("name"));
			}
		}
		return names;
	}

	/** @return the column names the data set declares in its result set hints */
	private static List<String> columnNames(Element dataSet) {
		List<String> names = new ArrayList<>();
		for (Element list : elements(dataSet.getChildNodes())) {
			if ("list-property".equals(list.getNodeName()) && "resultSetHints".equals(list.getAttribute("name"))) {
				for (Element structure : elements(list.getElementsByTagName("structure"))) {
					names.add(property(structure, "name"));
				}
			}
		}
		return names;
	}

	private static String methodBody(Element parent, String name) {
		for (Element method : elements(parent.getChildNodes())) {
			if ("method".equals(method.getNodeName()) && name.equals(method.getAttribute("name"))) {
				return method.getTextContent();
			}
		}
		throw new AssertionError("no method named " + name + " on <" + parent.getNodeName() + ">");
	}

	private static String text(Node node) {
		return node.getTextContent().trim();
	}
}
