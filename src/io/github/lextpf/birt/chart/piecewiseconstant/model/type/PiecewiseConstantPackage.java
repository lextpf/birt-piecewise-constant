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
 * The EMF <b>Package</b> of the piecewise constant model.
 * <p>
 * Intent: the package holds the meta object of every class, of every feature and
 * of the enumeration. EMF needs these meta objects to read and write the series
 * in the chart XML.
 * <p>
 * Constraints: {@link #eNS_URI} must stay equal to the <code>namespaceURI</code>
 * of the <code>charttypes</code> extension in <code>plugin.xml</code>. A saved
 * chart holds the same URI, so a change to it makes old charts unreadable.
 * <p>
 * Side effects: the first read of {@link #eINSTANCE} runs
 * {@code PiecewiseConstantPackageImpl.init()}. That method puts the package into
 * the global {@code EPackage.Registry.INSTANCE}.
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantFactory
 */
public interface PiecewiseConstantPackage extends EPackage {

	/**
	 * The name of the package.
	 */
	String eNAME = "piecewiseconstant"; //$NON-NLS-1$

	/**
	 * The namespace URI of the package. The chart XML holds this URI, and the
	 * <code>charttypes</code> extension in <code>plugin.xml</code> names it.
	 */
	String eNS_URI = "http://lextpf.github.io/birt/chart/PiecewiseConstantModelType"; //$NON-NLS-1$

	/**
	 * The namespace prefix of the package. The serializer writes the series as
	 * <code>xsi:type="piecewise:PiecewiseConstantSeries"</code>.
	 */
	String eNS_PREFIX = "piecewise"; //$NON-NLS-1$

	/**
	 * The single instance of the package. The first read of this field builds and
	 * registers the package.
	 */
	PiecewiseConstantPackage eINSTANCE =
			io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantPackageImpl.init();

	/**
	 * The classifier id of the
	 * '{@link io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl
	 * <em>Piecewise Constant Series</em>}' class. The factory reads this id to
	 * decide which object it creates.
	 */
	int PIECEWISE_CONSTANT_SERIES = 0;

	/**
	 * The classifier id of the '{@link StepMode <em>Step Mode</em>}' enumeration.
	 * The factory reads this id to decide which data type it parses.
	 */
	int STEP_MODE = 1;

	/**
	 * The feature id of the '<em><b>Step Mode</b></em>' attribute. The value is the
	 * feature count of BIRT's <code>LineSeries</code>, which is 19 in BIRT 4.24.
	 * <p>
	 * Constraints: javac inlines this constant into the compiled class files. If a
	 * later chart engine adds a feature to <code>LineSeries</code>, then this id
	 * collides with the new BIRT feature. The plug-in must be rebuilt against that
	 * chart engine. {@code PiecewiseConstantPackageImpl.init()} compares this
	 * constant against the feature count of the run time and stops the plug-in on a
	 * mismatch.
	 */
	int PIECEWISE_CONSTANT_SERIES__STEP_MODE = TypePackage.LINE_SERIES_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Piecewise Constant Series</em>'
	 * class. The series adds one feature to the features of
	 * <code>LineSeries</code>.
	 */
	int PIECEWISE_CONSTANT_SERIES_FEATURE_COUNT = TypePackage.LINE_SERIES_FEATURE_COUNT + 1;

	/**
	 * Returns the meta object of the class '{@link PiecewiseConstantSeries
	 * <em>Piecewise Constant Series</em>}'.
	 *
	 * @return the meta object of the class '<em>Piecewise Constant Series</em>'
	 * @see PiecewiseConstantSeries
	 */
	EClass getPiecewiseConstantSeries();

	/**
	 * Returns the meta object of the attribute
	 * '{@link PiecewiseConstantSeries#getStepMode <em>Step Mode</em>}'.
	 *
	 * @return the meta object of the attribute '<em>Step Mode</em>'
	 * @see PiecewiseConstantSeries#getStepMode()
	 * @see #getPiecewiseConstantSeries()
	 */
	EAttribute getPiecewiseConstantSeries_StepMode();

	/**
	 * Returns the meta object of the enumeration '{@link StepMode <em>Step
	 * Mode</em>}'.
	 *
	 * @return the meta object of the enumeration '<em>Step Mode</em>'
	 * @see StepMode
	 */
	EEnum getStepMode();

	/**
	 * Returns the factory that builds the objects of this model.
	 *
	 * @return the factory of this model
	 */
	PiecewiseConstantFactory getPiecewiseConstantFactory();

	/**
	 * The meta object of every class, of every feature and of the enumeration.
	 * <p>
	 * Constraints: each field reads {@link #eINSTANCE}. A read of any field of this
	 * interface therefore builds and registers the package.
	 */
	interface Literals {

		/**
		 * The meta object of the
		 * '{@link io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl.PiecewiseConstantSeriesImpl
		 * <em>Piecewise Constant Series</em>}' class.
		 */
		EClass PIECEWISE_CONSTANT_SERIES = eINSTANCE.getPiecewiseConstantSeries();

		/**
		 * The meta object of the '<em><b>Step Mode</b></em>' attribute.
		 */
		EAttribute PIECEWISE_CONSTANT_SERIES__STEP_MODE = eINSTANCE.getPiecewiseConstantSeries_StepMode();

		/**
		 * The meta object of the '<em>Step Mode</em>' enumeration.
		 */
		EEnum STEP_MODE = eINSTANCE.getStepMode();
	}
}
