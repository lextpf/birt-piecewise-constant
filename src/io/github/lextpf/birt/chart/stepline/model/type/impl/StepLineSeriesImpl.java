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

import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.type.impl.LineSeriesImpl;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

import io.github.lextpf.birt.chart.stepline.model.type.StepLineFactory;
import io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage;
import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;

/**
 * An implementation of the model object '<em><b>Step Line Series</b></em>'.
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link StepLineSeriesImpl#getStepMode <em>Step Mode</em>}</li>
 * </ul>
 */
public class StepLineSeriesImpl extends LineSeriesImpl implements StepLineSeries {

	/**
	 * The default value of the '{@link #getStepMode() <em>Step Mode</em>}'
	 * attribute.
	 *
	 * @see #getStepMode()
	 */
	protected static final StepMode STEP_MODE_EDEFAULT = StepMode.AFTER_LITERAL;

	/**
	 * The cached value of the '{@link #getStepMode() <em>Step Mode</em>}'
	 * attribute.
	 *
	 * @see #getStepMode()
	 */
	protected StepMode stepMode = STEP_MODE_EDEFAULT;

	/**
	 * This is true if the Step Mode attribute has been set.
	 */
	protected boolean stepModeESet;

	protected StepLineSeriesImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return StepLinePackage.Literals.STEP_LINE_SERIES;
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
			eNotify(new ENotificationImpl(this, Notification.SET, StepLinePackage.STEP_LINE_SERIES__STEP_MODE,
					oldStepMode, stepMode, !oldStepModeESet));
		}
	}

	@Override
	public void unsetStepMode() {
		StepMode oldStepMode = stepMode;
		boolean oldStepModeESet = stepModeESet;
		stepMode = STEP_MODE_EDEFAULT;
		stepModeESet = false;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.UNSET, StepLinePackage.STEP_LINE_SERIES__STEP_MODE,
					oldStepMode, STEP_MODE_EDEFAULT, oldStepModeESet));
		}
	}

	@Override
	public boolean isSetStepMode() {
		return stepModeESet;
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
		case StepLinePackage.STEP_LINE_SERIES__STEP_MODE:
			return getStepMode();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
		case StepLinePackage.STEP_LINE_SERIES__STEP_MODE:
			setStepMode((StepMode) newValue);
			return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
		case StepLinePackage.STEP_LINE_SERIES__STEP_MODE:
			unsetStepMode();
			return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
		case StepLinePackage.STEP_LINE_SERIES__STEP_MODE:
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
	 * A convenience method to create an initialized 'Series' instance.
	 *
	 * @return a step line series instance with the 'isSet' flags set
	 */
	public static Series create() {
		final StepLineSeries sls = StepLineFactory.eINSTANCE.createStepLineSeries();
		((StepLineSeriesImpl) sls).initialize();
		return sls;
	}

	/**
	 * Initializes all member variables within this object recursively.
	 *
	 * Note: Manually written
	 */
	@Override
	protected void initialize() {
		super.initialize();
		setStepMode(StepMode.AFTER_LITERAL);
	}

	/**
	 * A convenience method to create an initialized 'Series' instance.
	 *
	 * @return a step line series instance without the 'isSet' flags set
	 */
	public static Series createDefault() {
		final StepLineSeries sls = StepLineFactory.eINSTANCE.createStepLineSeries();
		((StepLineSeriesImpl) sls).initDefault();
		return sls;
	}

	/**
	 * Initializes all member variables within this object recursively.
	 *
	 * Note: Manually written
	 */
	@Override
	protected void initDefault() {
		super.initDefault();
		stepMode = StepMode.AFTER_LITERAL;
	}

	@Override
	public String getDisplayName() {
		return "Step Line Series"; //$NON-NLS-1$
	}

	@Override
	public StepLineSeries copyInstance() {
		StepLineSeriesImpl dest = new StepLineSeriesImpl();
		dest.set(this);
		return dest;
	}

	protected void set(StepLineSeries src) {

		super.set(src);

		// attributes

		stepMode = src.getStepMode();

		stepModeESet = src.isSetStepMode();

	}
}
