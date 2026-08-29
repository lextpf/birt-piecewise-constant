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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.birt.chart.api.ChartEngine;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.core.framework.PlatformConfig;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Starts the POJO runtime once per JVM, before a test class that needs the
 * extension registry runs.
 * <p>
 * Intent: the chart engine finds this plug-in only through the extension
 * registry. A test class gets that registry with {@code @ExtendWith}.
 * <p>
 * Constraints: the {@link PlatformConfig} must not carry the
 * {@code STANDALONE} property. Without that property {@code PluginSettings}
 * starts the POJO {@code ServiceLauncher}, and
 * {@link Platform#getExtensionRegistry()} returns a registry.
 * <p>
 * It never calls {@code Platform.shutdown()}.
 * The extension therefore loads {@code SerializerImpl} after the runtime is up.
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.PiecewiseConstantModelLoader
 */
public class ChartPlatformExtension implements BeforeAllCallback {

	private static boolean started;

	@Override
	public void beforeAll(ExtensionContext context) throws ClassNotFoundException {
		startPlatform();
	}

	// Starts the POJO runtime, and returns immediately on every call after the first one.
	private static synchronized void startPlatform() throws ClassNotFoundException {
		if (started) {
			return;
		}

		PlatformConfig config = new PlatformConfig();
		config.setTempDir(System.getProperty("java.io.tmpdir"));
		ChartEngine.instance(config);

		assertNotNull(Platform.getExtensionRegistry(),
				"the POJO runtime did not start: Platform.getExtensionRegistry() is null");

		Class.forName("org.eclipse.birt.chart.model.impl.SerializerImpl");

		started = true;
	}
}
