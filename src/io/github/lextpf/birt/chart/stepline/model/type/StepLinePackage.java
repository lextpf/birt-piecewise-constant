/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline.model.type;

import org.eclipse.birt.chart.model.type.TypePackage;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;

/**
 * The <b>Package</b> for the step line model. It contains accessors for the
 * meta objects to represent each class, each feature of each class and each
 * enum.
 *
 * @see io.github.lextpf.birt.chart.stepline.model.type.StepLineFactory
 */
public interface StepLinePackage extends EPackage {

	/**
	 * The package name.
	 */
	String eNAME = "stepline"; //$NON-NLS-1$

	/**
	 * The package namespace URI.
	 */
	String eNS_URI = "http://lextpf.github.io/birt/chart/StepLineModelType"; //$NON-NLS-1$

	/**
	 * The package namespace name.
	 */
	String eNS_PREFIX = "stepline"; //$NON-NLS-1$

	/**
	 * The singleton instance of the package.
	 */
	StepLinePackage eINSTANCE = io.github.lextpf.birt.chart.stepline.model.type.impl.StepLinePackageImpl.init();

	/**
	 * The meta object id for the
	 * '{@link io.github.lextpf.birt.chart.stepline.model.type.impl.StepLineSeriesImpl
	 * <em>Step Line Series</em>}' class.
	 */
	int STEP_LINE_SERIES = 0;

	/**
	 * The meta object id for the '{@link StepMode <em>Step Mode</em>}' enum.
	 */
	int STEP_MODE = 1;

	/**
	 * The feature id for the '<em><b>Step Mode</b></em>' attribute. It follows
	 * BIRT's inherited line series features, so it shifts if the chart engine ever
	 * adds a feature to <code>LineSeries</code> - see
	 * {@code StepLinePackageImpl.init()}.
	 */
	int STEP_LINE_SERIES__STEP_MODE = TypePackage.LINE_SERIES_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Step Line Series</em>' class.
	 */
	int STEP_LINE_SERIES_FEATURE_COUNT = TypePackage.LINE_SERIES_FEATURE_COUNT + 1;

	/**
	 * Returns the meta object for class '{@link StepLineSeries <em>Step Line
	 * Series</em>}'.
	 *
	 * @return the meta object for class '<em>Step Line Series</em>'
	 * @see StepLineSeries
	 */
	EClass getStepLineSeries();

	/**
	 * Returns the meta object for the attribute
	 * '{@link StepLineSeries#getStepMode <em>Step Mode</em>}'.
	 *
	 * @return the meta object for the attribute '<em>Step Mode</em>'
	 * @see StepLineSeries#getStepMode()
	 * @see #getStepLineSeries()
	 */
	EAttribute getStepLineSeries_StepMode();

	/**
	 * Returns the meta object for enum '{@link StepMode <em>Step Mode</em>}'.
	 *
	 * @return the meta object for enum '<em>Step Mode</em>'
	 * @see StepMode
	 */
	EEnum getStepMode();

	/**
	 * Returns the factory that creates the instances of the model.
	 *
	 * @return the factory that creates the instances of the model
	 */
	StepLineFactory getStepLineFactory();

	/**
	 * Defines literals for the meta objects that represent each class, each feature
	 * of each class and each enum.
	 */
	interface Literals {

		/**
		 * The meta object literal for the
		 * '{@link io.github.lextpf.birt.chart.stepline.model.type.impl.StepLineSeriesImpl
		 * <em>Step Line Series</em>}' class.
		 */
		EClass STEP_LINE_SERIES = eINSTANCE.getStepLineSeries();

		/**
		 * The meta object literal for the '<em><b>Step Mode</b></em>' attribute
		 * feature.
		 */
		EAttribute STEP_LINE_SERIES__STEP_MODE = eINSTANCE.getStepLineSeries_StepMode();

		/**
		 * The meta object literal for the '<em>Step Mode</em>' enum.
		 */
		EEnum STEP_MODE = eINSTANCE.getStepMode();
	}
}
