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
 * Runs {@code piecewise-constant-sample.rptdesign} through a real, unpacked BIRT report
 * runtime and checks that the piecewise constant chart comes out as SVG.
 * <p>
 * This is the end-to-end proof that the bundle is a well formed BIRT plug-in:
 * the report engine is loaded from the distribution's own jars in a separate
 * {@link URLClassLoader} whose parent is the platform class loader, so
 * <em>nothing</em> of this module's own test classpath can leak into it. The
 * only extra entry on that class loader is our bundle jar; BIRT's POJO
 * {@code ServiceLauncher} finds it by scanning the class loader for
 * {@code META-INF/MANIFEST.MF} entries and reads the {@code plugin.xml} next to
 * it, which is exactly what happens when a user drops the jar into
 * {@code ReportEngine/lib}. {@code BIRT_HOME} is cleared so the engine does not
 * try to start Equinox instead.
 * <p>
 * The distribution is a ~100 MB download and is not part of the build, so the
 * test only runs when it is pointed at one:
 *
 * <pre>
 * .\build.ps1 clean install
 * .\build.ps1 test -Dtest=RuntimeSmokeIT -Dbirt.runtime.dir=C:\birt-runtime-4.24.0
 * </pre>
 *
 * Without {@code -Dbirt.runtime.dir} the test is reported as skipped.
 */
@EnabledIfSystemProperty(named = "birt.runtime.dir", matches = ".+")
class RuntimeSmokeIT {

	/** The bundle jar, as it is named in {@code build} and in the runtime. */
	private static final String BUNDLE_JAR = "io.github.lextpf.birt.chart.piecewiseconstant_1.0.0.jar";

	/** The sample report, on the test classpath. */
	private static final String SAMPLE_REPORT = "piecewise-constant-sample.rptdesign";

	@Test
	void theReportRuntimeRendersThePiecewiseConstantChartAsSvg() throws Exception {
		Path lib = Path.of(System.getProperty("birt.runtime.dir"), "ReportEngine", "lib");
		assertTrue(Files.isDirectory(lib),
				lib + " does not exist - point -Dbirt.runtime.dir at an unpacked birt-runtime-4.24.0 distribution");

		File design = extractSampleReport();
		File html = new File(CapturingPngRenderer.outputDirectory(), "runtime-smoke.html");
		Files.deleteIfExists(html.toPath());

		int exitCode = runReport(lib, design, html);

		assertEquals(0, exitCode, "ReportRunner did not finish successfully");
		assertTrue(html.isFile(), "ReportRunner produced no output at " + html);

		String output = Files.readString(html.toPath(), StandardCharsets.UTF_8);
		assertTrue(output.contains("image/svg+xml") || output.contains("<svg"),
				"the rendered report contains no SVG chart - the piecewise constant series was probably not resolved:\n"
						+ output);
	}

	/**
	 * Runs {@code org.eclipse.birt.report.engine.api.ReportRunner} out of the
	 * distribution. The class is not on this module's classpath, so everything goes
	 * through reflection - a compile time import would bind us to a different copy
	 * of the engine.
	 *
	 * @param lib    the distribution's {@code ReportEngine/lib} directory
	 * @param design the report design to run
	 * @param html   the HTML file to write
	 * @return the runner's exit code, 0 on success
	 * @throws Exception if the runner cannot be loaded or invoked
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
	 * @param lib the distribution's {@code ReportEngine/lib} directory
	 * @return every jar of the distribution plus this bundle
	 * @throws IOException if the directory cannot be listed
	 */
	private static URL[] runtimeClasspath(Path lib) throws IOException {
		List<URL> urls = new ArrayList<>();
		try (Stream<Path> entries = Files.list(lib)) {
			for (Path jar : entries.filter(path -> path.getFileName().toString().endsWith(".jar")).sorted().toList()) {
				urls.add(jar.toUri().toURL());
			}
		}
		assertTrue(!urls.isEmpty(), "no jars found in " + lib);
		urls.add(bundleRoot().toURI().toURL());
		return urls.toArray(new URL[0]);
	}

	/**
	 * @return the packaged bundle jar, or - if the project has not been packaged
	 *         yet - {@code build/classes}, which carries the very same
	 *         {@code META-INF/MANIFEST.MF} and {@code plugin.xml} at its root
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
						+ "/plugin.xml exists - run .\\build.ps1 clean install first");
		return classes;
	}

	/**
	 * @return the sample report, copied out of the test classpath into the test
	 *         output directory so that the engine can open it as a file
	 * @throws IOException if the resource cannot be copied
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
