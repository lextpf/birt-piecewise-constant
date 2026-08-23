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

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;
import io.github.lextpf.birt.chart.piecewiseconstant.test.ChartPlatformExtension;

/**
 * Guards the sample report {@code piecewise-constant-sample.rptdesign} that
 * {@link RuntimeSmokeIT} gives to a POJO runtime.
 * <p>
 * Intent: {@link RuntimeSmokeIT} runs only if somebody points
 * {@code -Dbirt.runtime.dir} at an unpacked distribution. Without this class no
 * normal build would check the report resource.
 * <p>
 * Non-obvious behaviour: the parse in this class proves that the report is well
 * formed XML. The parse also proves that the report is wired the way the report
 * engine expects. The class then reads the embedded chart XML back through
 * {@code SerializerImpl}. That read proves that the report carries a piecewise
 * constant series bound to the columns of the report. Without that check, a
 * typing error in the CDATA section would appear only on a machine that has the
 * runtime.
 */
@ExtendWith(ChartPlatformExtension.class)
class SampleReportTest {

	private static final String SAMPLE_REPORT = "piecewise-constant-sample.rptdesign";

	private static Document report;

	/**
	 * Parses the sample report from the test classpath into {@link #report}.
	 * <p>
	 * Constraints: the parser resolves no external DTD and no external schema.
	 *
	 * @throws IOException                  if the resource cannot be read
	 * @throws SAXException                 if the resource is not well formed XML
	 * @throws ParserConfigurationException if the parser cannot be created
	 */
	@BeforeAll
	static void parseTheSampleReport() throws IOException, SAXException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		DocumentBuilder builder = factory.newDocumentBuilder();

		try (InputStream in = SampleReportTest.class.getClassLoader().getResourceAsStream(SAMPLE_REPORT)) {
			assertNotNull(in, SAMPLE_REPORT + " is missing from the test classpath");
			// If the resource is not well formed, then this call throws a SAXException.
			// That exception is the assertion on well formed XML.
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
		assertEquals("Piecewise Constant Data Set", dataSet.getAttribute("name"));

		assertEquals(List.of("category", "value"), columnNames(dataSet),
				"the data set must declare exactly the two columns that the chart binds to");

		assertEquals(List.of("resultSetHints", "columnHints"), listPropertyNames(dataSet),
				"the ROM defines no \"resultSet\" on script-data-set; only oda-data-set and joint-data-set have one."
						+ " ListPropertyState skips the undefined-property error for that name, but it leaves the"
						+ " property definition null, and DesignReader then fails with a NullPointerException in"
						+ " StructureState");

		String fetch = methodBody(dataSet, "fetch");
		assertTrue(fetch.contains("i < 5"), "the data set must produce five rows, but fetch reads:\n" + fetch);
		assertTrue(fetch.contains("row[\"category\"]") && fetch.contains("row[\"value\"]"),
				"fetch must fill both columns:\n" + fetch);
	}

	@Test
	void bindsTheChartItemToThatDataSet() {
		Element chartItem = onlyElement("extended-item");
		assertEquals("Chart", chartItem.getAttribute("extensionName"));
		assertEquals("Piecewise Constant Data Set", property(chartItem, "dataSet"));
		assertEquals("SVG", property(chartItem, "outputFormat"),
				"RuntimeSmokeIT looks for SVG in the rendered HTML");

		List<String> bound = new ArrayList<>();
		for (Element structure : elements(chartItem.getElementsByTagName("structure"))) {
			bound.add(property(structure, "name"));
		}
		assertEquals(List.of("category", "value"), bound, "the chart must bind both data set columns");
	}

	@Test
	void carriesAPiecewiseConstantSeriesInItsChartXml() {
		String chartXml = chartXml();

		assertTrue(chartXml.contains("xmlns:piecewise=\"http://lextpf.github.io/birt/chart/PiecewiseConstantModelType\""),
				"the chart XML does not declare the namespace of this plug-in:\n" + chartXml);
		assertTrue(chartXml.contains("xsi:type=\"piecewise:PiecewiseConstantSeries\""),
				"the chart XML contains no piecewise constant series:\n" + chartXml);
		assertTrue(chartXml.contains("<StepMode>After</StepMode>"),
				"the chart XML contains no step mode:\n" + chartXml);
	}

	@Test
	void thatChartXmlLoadsBackAsAnAfterModeSeriesBoundToTheReportColumns() throws IOException {
		Chart chart = SerializerImpl.instance()
				.read(new ByteArrayInputStream(chartXml().getBytes(StandardCharsets.UTF_8)));

		Axis xAxis = ((ChartWithAxes) chart).getPrimaryBaseAxes()[0];
		Series categorySeries = xAxis.getSeriesDefinitions().get(0).getSeries().get(0);
		Series valueSeries = ((ChartWithAxes) chart).getPrimaryOrthogonalAxis(xAxis).getSeriesDefinitions().get(0)
				.getSeries().get(0);

		PiecewiseConstantSeries piecewiseConstant = assertInstanceOf(PiecewiseConstantSeries.class, valueSeries,
				"the value series of the sample report is not a piecewise constant series");
		assertEquals(StepMode.AFTER_LITERAL, piecewiseConstant.getStepMode());

		assertEquals("row[\"category\"]", categorySeries.getDataDefinition().get(0).getDefinition());
		assertEquals("row[\"value\"]", valueSeries.getDataDefinition().get(0).getDefinition());
		assertTrue(categorySeries.getDataSet() == null && valueSeries.getDataSet() == null,
				"a report chart takes its data from the report and not from an inline data set");

		// If the outermost block carries an explicit size of 0 x 0, then
		// ChartReportItemPresentationBase.onRowSets returns null. The HTML then contains
		// an empty <div> and no error appears anywhere. The sample must therefore
		// carry the size of the report item here.
		Bounds bounds = chart.getBlock().getBounds();
		assertEquals(400.0, bounds.getWidth(), 0.0, "the chart block must carry the width of the report item");
		assertEquals(250.0, bounds.getHeight(), 0.0, "the chart block must carry the height of the report item");
	}

	/**
	 * @return the chart XML out of the CDATA section of the
	 *         {@code xmlRepresentation} property
	 * @throws AssertionError if the report has no {@code xmlRepresentation}
	 *                        property
	 */
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

	/**
	 * @param parent the element to read
	 * @param name   the value of the {@code name} attribute
	 * @return the text of the {@code <property name="..."/>} child with that name
	 * @throws AssertionError if the element has no such child
	 */
	private static String property(Element parent, String name) {
		for (Element property : elements(parent.getChildNodes())) {
			if ("property".equals(property.getNodeName()) && name.equals(property.getAttribute("name"))) {
				return text(property);
			}
		}
		throw new AssertionError("no property named " + name + " on <" + parent.getNodeName() + ">");
	}

	/**
	 * @param parent the element to read
	 * @return the names of the {@code <list-property>} children, in document order
	 */
	private static List<String> listPropertyNames(Element parent) {
		List<String> names = new ArrayList<>();
		for (Element list : elements(parent.getChildNodes())) {
			if ("list-property".equals(list.getNodeName())) {
				names.add(list.getAttribute("name"));
			}
		}
		return names;
	}

	/**
	 * @param dataSet the {@code <script-data-set>} element
	 * @return the column names that the data set declares in its result set hints
	 */
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

	/**
	 * @param parent the element to read
	 * @param name   the value of the {@code name} attribute
	 * @return the script text of the {@code <method>} child with that name
	 * @throws AssertionError if the element has no such child
	 */
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
