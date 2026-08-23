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

import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.type.impl.LineSeriesImpl;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantFactory;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;

/**
 * The implementation of the model object '<em><b>Piecewise Constant
 * Series</b></em>'.
 * <p>
 * Intent: the class adds the step mode attribute to BIRT's
 * {@code LineSeriesImpl}. It inherits every other feature of a line series.
 * <p>
 * Constraints: the chart engine resolves the renderer from the exact class name
 * of the series object ({@code PluginSettings.getRenderer}). This class name
 * must therefore stay in the <code>modelrenderers</code> and
 * <code>datasetprocessors</code> extensions of <code>plugin.xml</code>, and in
 * {@code PiecewiseConstantSetup}. If a developer renames or moves this class,
 * then the chart engine finds no renderer.
 * <p>
 * Side effects: none beyond the EMF notifications of the setters.
 * <p>
 * Non-obvious behaviour: the class implements one feature,
 * {@link PiecewiseConstantSeriesImpl#getStepMode <em>Step Mode</em>}.
 */
public class PiecewiseConstantSeriesImpl extends LineSeriesImpl implements PiecewiseConstantSeries {

	/**
	 * The default value of the '{@link #getStepMode() <em>Step Mode</em>}'
	 * attribute. The value must stay equal to the default value of the attribute in
	 * the meta model.
	 *
	 * @see #getStepMode()
	 */
	protected static final StepMode STEP_MODE_EDEFAULT = StepMode.AFTER_LITERAL;

	/**
	 * The current value of the '{@link #getStepMode() <em>Step Mode</em>}'
	 * attribute.
	 *
	 * @see #getStepMode()
	 */
	protected StepMode stepMode = STEP_MODE_EDEFAULT;

	/**
	 * The set flag of the step mode attribute. The serializer writes the
	 * <code>StepMode</code> element only when this flag is <code>true</code>.
	 */
	protected boolean stepModeESet;

	/**
	 * Builds an empty series. The factory and {@link #copyInstance()} call this
	 * constructor.
	 *
	 * @see PiecewiseConstantFactory#createPiecewiseConstantSeries()
	 */
	protected PiecewiseConstantSeriesImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return PiecewiseConstantPackage.Literals.PIECEWISE_CONSTANT_SERIES;
	}

	@Override
	public StepMode getStepMode() {
		return stepMode;
	}

	@Override
	public void setStepMode(StepMode newStepMode) {
		StepMode oldStepMode = stepMode;
		stepMode = newStepMode == null ? STEP_MODE_EDEFAULT : newStepMode;
		boolean oldStepModeESet = stepModeESet;
		stepModeESet = true;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET,
					PiecewiseConstantPackage.PIECEWISE_CONSTANT_SERIES__STEP_MODE, oldStepMode, stepMode,
					!oldStepModeESet));
		}
	}

	@Override
	public void unsetStepMode() {
		StepMode oldStepMode = stepMode;
		boolean oldStepModeESet = stepModeESet;
		stepMode = STEP_MODE_EDEFAULT;
		stepModeESet = false;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.UNSET,
					PiecewiseConstantPackage.PIECEWISE_CONSTANT_SERIES__STEP_MODE, oldStepMode, STEP_MODE_EDEFAULT,
					oldStepModeESet));
		}
	}

	@Override
	public boolean isSetStepMode() {
		return stepModeESet;
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
		case PiecewiseConstantPackage.PIECEWISE_CONSTANT_SERIES__STEP_MODE:
			return getStepMode();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
		case PiecewiseConstantPackage.PIECEWISE_CONSTANT_SERIES__STEP_MODE:
			setStepMode((StepMode) newValue);
			return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
		case PiecewiseConstantPackage.PIECEWISE_CONSTANT_SERIES__STEP_MODE:
			unsetStepMode();
			return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
		case PiecewiseConstantPackage.PIECEWISE_CONSTANT_SERIES__STEP_MODE:
			return isSetStepMode();
		}
		return super.eIsSet(featureID);
	}

	@Override
	public String toString() {
		if (eIsProxy()) {
			return super.toString();
		}

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (stepMode: "); //$NON-NLS-1$
		if (stepModeESet) {
			result.append(stepMode);
		} else {
			result.append("<unset>"); //$NON-NLS-1$
		}
		result.append(')');
		return result.toString();
	}

	/**
	 * Builds an initialized piecewise constant series.
	 * <p>
	 * Constraints: BIRT's {@code ChartExtensionValueUpdater} calls this method by
	 * reflection. The method must therefore keep this exact name, must stay
	 * <code>public static</code> and must keep the return type {@link Series}.
	 * <p>
	 * Non-obvious behaviour: the method sets the set flag of the step mode and of
	 * the inherited members that {@link #initialize()} touches. The serializer
	 * therefore writes each of these members into the chart XML.
	 *
	 * @return a piecewise constant series with the set flags set
	 */
	public static Series create() {
		final PiecewiseConstantSeries sls = PiecewiseConstantFactory.eINSTANCE.createPiecewiseConstantSeries();
		((PiecewiseConstantSeriesImpl) sls).initialize();
		return sls;
	}

	/**
	 * Sets every member of this series, and of its parts, to its initial value.
	 * <p>
	 * Side effects: the method calls {@link #setStepMode(StepMode)}, so it also sets
	 * the set flag of the step mode.
	 * <p>
	 * Note: a developer writes this method by hand. EMF does not generate it.
	 */
	@Override
	protected void initialize() {
		super.initialize();
		setStepMode(StepMode.AFTER_LITERAL);
	}

	/**
	 * Builds a piecewise constant series that carries the default values.
	 * <p>
	 * Non-obvious behaviour: the method leaves every set flag clear. The serializer
	 * therefore writes no <code>StepMode</code> element for this series.
	 *
	 * @return a piecewise constant series without the set flags
	 */
	public static Series createDefault() {
		final PiecewiseConstantSeries sls = PiecewiseConstantFactory.eINSTANCE.createPiecewiseConstantSeries();
		((PiecewiseConstantSeriesImpl) sls).initDefault();
		return sls;
	}

	/**
	 * Sets every member of this series, and of its parts, to its default value.
	 * <p>
	 * Non-obvious behaviour: the method writes the step mode field directly and
	 * calls no setter. The set flag of the step mode therefore stays clear.
	 * <p>
	 * Note: a developer writes this method by hand. EMF does not generate it.
	 */
	@Override
	protected void initDefault() {
		super.initDefault();
		stepMode = StepMode.AFTER_LITERAL;
	}

	/**
	 * Returns the name that the chart engine shows for this series type.
	 * <p>
	 * Constraints: the plug-in does not translate this name.
	 *
	 * @return the display name of the series
	 */
	@Override
	public String getDisplayName() {
		return "Piecewise Constant Series"; //$NON-NLS-1$
	}

	/**
	 * Returns a copy of this series.
	 * <p>
	 * Intent: the chart engine calls this method for every render. The call builds
	 * the runtime series from the design series.
	 * <p>
	 * Constraints: this override must stay in this class. Without it the copy is a
	 * plain {@code LineSeriesImpl}. The chart engine then finds no renderer for the
	 * copy, and it draws a straight line without a report of an error.
	 *
	 * @return a copy of this series, with the step mode and with its set flag
	 */
	@Override
	public PiecewiseConstantSeries copyInstance() {
		PiecewiseConstantSeriesImpl dest = new PiecewiseConstantSeriesImpl();
		dest.set(this);
		return dest;
	}

	/**
	 * Copies the members of another piecewise constant series into this series.
	 * <p>
	 * Constraints: the caller must pass a series that is not <code>null</code>.
	 * {@link #copyInstance()} is the only caller in this plug-in.
	 * <p>
	 * Side effects: the method writes the step mode field directly and calls no
	 * setter. It therefore sends no EMF notification, and it keeps the set flag of
	 * the source series.
	 *
	 * @param src the series to copy the members from
	 */
	protected void set(PiecewiseConstantSeries src) {

		super.set(src);

		// attributes

		stepMode = src.getStepMode();

		stepModeESet = src.isSetStepMode();

	}
}
