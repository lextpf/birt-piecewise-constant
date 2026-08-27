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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.github.lextpf.birt.chart.piecewiseconstant.test.CapturingPngRenderer;

/**
 * Runs {@code piecewise-constant-sample.rptdesign} through an unpacked POJO
 * runtime, and checks that the piecewise constant chart comes out as SVG.
 * <p>
 * Intent: this test is the end-to-end proof that the jar is a well formed BIRT
 * plug-in.
 * <p>
 * Constraints: the test runs only if the system property
 * {@code birt.runtime.dir} names an unpacked distribution. The distribution is
 * a download of about 100 MB and is not part of the build. Without
 * {@code -Dbirt.runtime.dir} JUnit reports the test as skipped. The first
 * command builds the jar. The second command runs this test against the
 * distribution:
 *
 * <pre>
 * .\build.ps1
 * .\test.ps1 -Test RuntimeSmokeIT -Runtime C:\birt-runtime-4.24.0
 * </pre>
 * <p>
 * Non-obvious behaviour: the test loads the report engine from the jars of the
 * distribution in a separate {@link URLClassLoader}. The parent of that class
 * loader is the platform class loader. No class of the test classpath of this
 * module can therefore reach the report engine. The only other entry on that class
 * loader is the jar of this plug-in. The POJO {@code ServiceLauncher} finds the
 * jar: it scans the class loader for {@code META-INF/MANIFEST.MF} entries and
 * reads the {@code plugin.xml} next to each one. A user gets the same result
 * when the user copies the jar into {@code ReportEngine/lib}. The test also
 * clears {@code BIRT_HOME}. If that property is set, then the report engine
 * tries to start the OSGi runtime instead of the POJO runtime.
 */
@EnabledIfSystemProperty(named = "birt.runtime.dir", matches = ".+")
class RuntimeSmokeIT {

	/**
	 * The name of the jar, in the {@code build} directory and in the POJO runtime.
	 */
	private static final String BUNDLE_JAR = "io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar";

	/** The name of the sample report on the test classpath. */
	private static final String SAMPLE_REPORT = "piecewise-constant-sample.rptdesign";

	@Test
	void theReportRuntimeRendersThePiecewiseConstantChartAsSvg() throws Exception {
		Path lib = Path.of(System.getProperty("birt.runtime.dir"), "ReportEngine", "lib");
		assertTrue(Files.isDirectory(lib),
				lib + " does not exist: point -Dbirt.runtime.dir at an unpacked birt-runtime-4.24.0 distribution");

		File design = extractSampleReport();
		File html = new File(CapturingPngRenderer.outputDirectory(), "runtime-smoke.html");
		Files.deleteIfExists(html.toPath());

		int exitCode = runReport(lib, design, html);

		assertEquals(0, exitCode, "ReportRunner did not finish successfully");
		assertTrue(html.isFile(), "ReportRunner wrote no output at " + html);

		String output = Files.readString(html.toPath(), StandardCharsets.UTF_8);
		assertTrue(output.contains("image/svg+xml") || output.contains("<svg"),
				"the rendered report contains no SVG chart; the POJO runtime probably did not resolve the piecewise"
						+ " constant series:\n" + output);
	}

	/**
	 * Runs {@code org.eclipse.birt.report.engine.api.ReportRunner} out of the
	 * distribution.
	 * <p>
	 * Constraints: the class is not on the classpath of this module, so the method
	 * calls it by reflection. An import at compile time would bind this test to
	 * another copy of the report engine.
	 * <p>
	 * Side effects: the method clears the {@code BIRT_HOME} system property and
	 * replaces the context class loader of the thread. It restores both before it
	 * returns.
	 *
	 * @param lib    the {@code ReportEngine/lib} directory of the distribution
	 * @param design the report design to run
	 * @param html   the HTML file to write
	 * @return the exit code of the runner, 0 for success
	 * @throws Exception if the runner cannot be loaded or called
	 */
	private static int runReport(Path lib, File design, File html) throws Exception {
		String[] arguments = { "-f", "HTML", "-o", html.getAbsolutePath(), "-m", "RunAndRender",
				design.getAbsolutePath() };

		String birtHome = System.getProperty("BIRT_HOME");
		System.clearProperty("BIRT_HOME");
		ClassLoader callerLoader = Thread.currentThread().getContextClassLoader();
		try (URLClassLoader runtime = new URLClassLoader(runtimeClasspath(lib), ClassLoader.getPlatformClassLoader())) {
			Thread.currentThread().setContextClassLoader(runtime);

			Class<?> runnerClass = runtime.loadClass("org.eclipse.birt.report.engine.api.ReportRunner");
			Object runner = runnerClass.getConstructor(String[].class).newInstance((Object) arguments);
			return (Integer) runnerClass.getMethod("execute").invoke(runner);
		} finally {
			Thread.currentThread().setContextClassLoader(callerLoader);
			if (birtHome != null) {
				System.setProperty("BIRT_HOME", birtHome);
			}
		}
	}

	/**
	 * @param lib the {@code ReportEngine/lib} directory of the distribution
	 * @return every jar of the distribution, and the jar of this plug-in last
	 * @throws IOException if the method cannot list the directory
	 */
	private static URL[] runtimeClasspath(Path lib) throws IOException {
		List<URL> urls = new ArrayList<>();
		try (Stream<Path> entries = Files.list(lib)) {
			for (Path jar : entries.filter(path -> path.getFileName().toString().endsWith(".jar")).sorted().toList()) {
				urls.add(jar.toUri().toURL());
			}
		}
		assertTrue(!urls.isEmpty(), "the directory " + lib + " contains no jar");
		urls.add(bundleRoot().toURI().toURL());
		return urls.toArray(new URL[0]);
	}

	/**
	 * @return the packaged jar of this plug-in. If the build has not packaged the
	 *         project yet, then the method returns {@code build/classes}, which
	 *         carries the same {@code META-INF/MANIFEST.MF} and the same
	 *         {@code plugin.xml} at its root.
	 * @throws AssertionError if neither the jar nor the classes directory exists
	 */
	private static File bundleRoot() {
		// Surefire runs with the working directory set to the build directory.
		File jar = new File(BUNDLE_JAR);
		if (jar.isFile()) {
			return jar;
		}
		File classes = new File("classes");
		assertTrue(new File(classes, "plugin.xml").isFile(),
				"neither " + jar.getAbsolutePath() + " nor " + classes.getAbsolutePath()
						+ "/plugin.xml exists: run .\\build.ps1 clean install first");
		return classes;
	}

	/**
	 * Copies the sample report out of the test classpath into the test output
	 * directory, so that the report engine can open it as a file.
	 *
	 * @return the copied report design file
	 * @throws IOException if the method cannot copy the resource
	 */
	private static File extractSampleReport() throws IOException {
		File design = new File(CapturingPngRenderer.outputDirectory(), SAMPLE_REPORT);
		try (InputStream in = RuntimeSmokeIT.class.getClassLoader().getResourceAsStream(SAMPLE_REPORT)) {
			if (in == null) {
				throw new IOException(SAMPLE_REPORT + " is missing from the test classpath");
			}
			Files.copy(in, design.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return design;
	}
}
