/*******************************************************************************
 * Copyright (c) 2026 lextpf.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.github.lextpf.birt.chart.piecewiseconstant.model.type.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantFactory;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;

/**
 * The implementation of the model <b>Factory</b>.
 * <p>
 * Intent: the class builds the objects of the piecewise constant model, and it
 * converts the step mode between its literal string and its literal object.
 * <p>
 * Constraints: EMF calls the methods of this class while it reads or writes the
 * chart XML. The methods throw an {@link IllegalArgumentException} for a
 * classifier that this package does not hold.
 * <p>
 * Side effects: none.
 */
public class PiecewiseConstantFactoryImpl extends EFactoryImpl implements PiecewiseConstantFactory {

	/**
	 * Returns the factory of the piecewise constant package.
	 * <p>
	 * Intent: the field {@link PiecewiseConstantFactory#eINSTANCE} calls this
	 * method. Callers must read that field and must not call this method directly.
	 * <p>
	 * Non-obvious behaviour: the method returns the registered factory when the
	 * package is already in {@code EPackage.Registry.INSTANCE}. If the lookup throws
	 * an exception, then the method writes the exception to the log of
	 * {@code EcorePlugin} and builds a new factory.
	 *
	 * @return the registered factory of the piecewise constant namespace, or a new
	 *         factory
	 */
	public static PiecewiseConstantFactory init() {
		try {
			PiecewiseConstantFactory thePiecewiseConstantFactory = (PiecewiseConstantFactory) EPackage.Registry.INSTANCE
					.getEFactory(PiecewiseConstantPackage.eNS_URI);
			if (thePiecewiseConstantFactory != null) {
				return thePiecewiseConstantFactory;
			}
		} catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new PiecewiseConstantFactoryImpl();
	}

	/**
	 * Builds a factory. EMF needs a public constructor without arguments here.
	 */
	public PiecewiseConstantFactoryImpl() {
		super();
	}

	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
		case PiecewiseConstantPackage.PIECEWISE_CONSTANT_SERIES:
			return (EObject) createPiecewiseConstantSeries();
		default:
			throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public Object createFromString(EDataType eDataType, String initialValue) {
		switch (eDataType.getClassifierID()) {
		case PiecewiseConstantPackage.STEP_MODE:
			return createStepModeFromString(eDataType, initialValue);
		default:
			throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public String convertToString(EDataType eDataType, Object instanceValue) {
		switch (eDataType.getClassifierID()) {
		case PiecewiseConstantPackage.STEP_MODE:
			return convertStepModeToString(eDataType, instanceValue);
		default:
			throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Reads a '<em>Step Mode</em>' literal from its literal string.
	 * <p>
	 * Constraints: the string must be the literal string of a step mode. The
	 * comparison is case-sensitive.
	 *
	 * @param eDataType    the data type of the enumeration
	 * @param initialValue the literal string, as written in the chart XML
	 * @return the matching literal object
	 * @throws IllegalArgumentException if no literal has this string
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
	 * Writes a '<em>Step Mode</em>' literal as its literal string.
	 * <p>
	 * Constraints: if the value is <code>null</code>, then the method returns
	 * <code>null</code>.
	 *
	 * @param eDataType     the data type of the enumeration
	 * @param instanceValue the literal object
	 * @return the literal string, as written in the chart XML
	 */
	public String convertStepModeToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	@Override
	public PiecewiseConstantSeries createPiecewiseConstantSeries() {
		return new PiecewiseConstantSeriesImpl();
	}

	@Override
	public PiecewiseConstantPackage getPiecewiseConstantPackage() {
		return (PiecewiseConstantPackage) getEPackage();
	}
}
