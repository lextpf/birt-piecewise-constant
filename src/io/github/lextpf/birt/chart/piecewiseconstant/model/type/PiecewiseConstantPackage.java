/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant.model.type;

import org.eclipse.birt.chart.model.type.TypePackage;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;

/**
 * The <b>Package</b> for the piecewise constant model. It contains accessors for the
 * meta objects to represent each class, each feature of each class and each
 * enum.
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantFactory
 */
public interface PiecewiseConstantPackage extends EPackage {

	/**
	 * The package name.
	 */
	String eNAME = "piecewiseconstant"; //$NON-NLS-1$

	/**
	 * The package namespace URI.
	 */
	String eNS_URI = "http://lextpf.github.io/birt/chart/PiecewiseConstantModelType"; //$NON-NLS-1$

	/**
	 * The package namespace name.
	 */
	String eNS_PREFIX = "piecewise"; //$NON-NLS-1$

	/**
	 * The singleton instance of the package.
	 */
	PiecewiseConstantPackage eINSTANCE =
			io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantPackageImpl.init();

	/**
	 * The meta object id for the
	 * '{@link io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl
	 * <em>Piecewise Constant Series</em>}' class.
	 */
	int PIECEWISE_CONSTANT_SERIES = 0;

	/**
	 * The meta object id for the '{@link StepMode <em>Step Mode</em>}' enum.
	 */
	int STEP_MODE = 1;

	/**
	 * The feature id for the '<em><b>Step Mode</b></em>' attribute. It follows
	 * BIRT's inherited line series features, so it shifts if the chart engine ever
	 * adds a feature to <code>LineSeries</code> - see
	 * {@code PiecewiseConstantPackageImpl.init()}.
	 */
	int PIECEWISE_CONSTANT_SERIES__STEP_MODE = TypePackage.LINE_SERIES_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Piecewise Constant Series</em>' class.
	 */
	int PIECEWISE_CONSTANT_SERIES_FEATURE_COUNT = TypePackage.LINE_SERIES_FEATURE_COUNT + 1;

	/**
	 * Returns the meta object for class '{@link PiecewiseConstantSeries <em>Piecewise Constant
	 * Series</em>}'.
	 *
	 * @return the meta object for class '<em>Piecewise Constant Series</em>'
	 * @see PiecewiseConstantSeries
	 */
	EClass getPiecewiseConstantSeries();

	/**
	 * Returns the meta object for the attribute
	 * '{@link PiecewiseConstantSeries#getStepMode <em>Step Mode</em>}'.
	 *
	 * @return the meta object for the attribute '<em>Step Mode</em>'
	 * @see PiecewiseConstantSeries#getStepMode()
	 * @see #getPiecewiseConstantSeries()
	 */
	EAttribute getPiecewiseConstantSeries_StepMode();

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
	PiecewiseConstantFactory getPiecewiseConstantFactory();

	/**
	 * Defines literals for the meta objects that represent each class, each feature
	 * of each class and each enum.
	 */
	interface Literals {

		/**
		 * The meta object literal for the
		 * '{@link io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl
		 * <em>Piecewise Constant Series</em>}' class.
		 */
		EClass PIECEWISE_CONSTANT_SERIES = eINSTANCE.getPiecewiseConstantSeries();

		/**
		 * The meta object literal for the '<em><b>Step Mode</b></em>' attribute
		 * feature.
		 */
		EAttribute PIECEWISE_CONSTANT_SERIES__STEP_MODE = eINSTANCE.getPiecewiseConstantSeries_StepMode();

		/**
		 * The meta object literal for the '<em>Step Mode</em>' enum.
		 */
		EEnum STEP_MODE = eINSTANCE.getStepMode();
	}
}
