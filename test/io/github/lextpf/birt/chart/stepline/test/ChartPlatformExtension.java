/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.birt.chart.api.ChartEngine;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.core.framework.PlatformConfig;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Boots BIRT's POJO platform once per JVM before any test class that needs the
 * extension registry.
 * <p>
 * The {@link PlatformConfig} deliberately carries <em>no</em>
 * {@code STANDALONE} property, so {@code PluginSettings} starts BIRT's POJO
 * {@code ServiceLauncher} and {@link Platform#getExtensionRegistry()} becomes
 * non-null. Only then is it safe to touch
 * {@code org.eclipse.birt.chart.model.impl.SerializerImpl} and
 * {@code org.eclipse.birt.chart.model.util.ChartDynamicExtension}: both cache
 * the list of extension model packages in a static initializer, and that cache
 * would be empty forever if their class initializer ran before start-up. The
 * class is therefore loaded here, after the platform is up.
 * <p>
 * {@code Platform.shutdown()} is never called - the platform outlives the test
 * class, and surefire forks a fresh JVM per test class anyway.
 */
public class ChartPlatformExtension implements BeforeAllCallback {

	private static boolean started;

	@Override
	public void beforeAll(ExtensionContext context) throws ClassNotFoundException {
		startPlatform();
	}

	private static synchronized void startPlatform() throws ClassNotFoundException {
		if (started) {
			return;
		}

		PlatformConfig config = new PlatformConfig();
		config.setTempDir(System.getProperty("java.io.tmpdir"));
		ChartEngine.instance(config);

		assertNotNull(Platform.getExtensionRegistry(),
				"BIRT's POJO platform did not start: Platform.getExtensionRegistry() is null");

		Class.forName("org.eclipse.birt.chart.model.impl.SerializerImpl");

		started = true;
	}
}
