/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.stepline.model.type.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import io.github.lextpf.birt.chart.stepline.model.type.StepLineFactory;
import io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage;
import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;

/**
 * An implementation of the model <b>Factory</b>.
 */
public class StepLineFactoryImpl extends EFactoryImpl implements StepLineFactory {

	/**
	 * Creates the default factory implementation.
	 *
	 * @return the registered factory for the step line namespace, or a new one
	 */
	public static StepLineFactory init() {
		try {
			StepLineFactory theStepLineFactory = (StepLineFactory) EPackage.Registry.INSTANCE
					.getEFactory(StepLinePackage.eNS_URI);
			if (theStepLineFactory != null) {
				return theStepLineFactory;
			}
		} catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new StepLineFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 */
	public StepLineFactoryImpl() {
		super();
	}

	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
		case StepLinePackage.STEP_LINE_SERIES:
			return (EObject) createStepLineSeries();
		default:
			throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public Object createFromString(EDataType eDataType, String initialValue) {
		switch (eDataType.getClassifierID()) {
		case StepLinePackage.STEP_MODE:
			return createStepModeFromString(eDataType, initialValue);
		default:
			throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public String convertToString(EDataType eDataType, Object instanceValue) {
		switch (eDataType.getClassifierID()) {
		case StepLinePackage.STEP_MODE:
			return convertStepModeToString(eDataType, instanceValue);
		default:
			throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Parses a '<em>Step Mode</em>' literal.
	 *
	 * @param eDataType    the enum's data type
	 * @param initialValue the literal as written in the chart XML
	 * @return the matching literal object
	 */
	public StepMode createStepModeFromString(EDataType eDataType, String initialValue) {
		StepMode result = StepMode.get(initialValue);
		if (result == null) {
			throw new IllegalArgumentException(
					"The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return result;
	}

	/**
	 * Renders a '<em>Step Mode</em>' literal.
	 *
	 * @param eDataType     the enum's data type
	 * @param instanceValue the literal object
	 * @return the literal as written in the chart XML
	 */
	public String convertStepModeToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	@Override
	public StepLineSeries createStepLineSeries() {
		return new StepLineSeriesImpl();
	}

	@Override
	public StepLinePackage getStepLinePackage() {
		return (StepLinePackage) getEPackage();
	}
}
