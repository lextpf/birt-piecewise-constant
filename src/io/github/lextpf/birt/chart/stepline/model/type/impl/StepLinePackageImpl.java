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

import org.eclipse.birt.chart.model.ModelPackage;
import org.eclipse.birt.chart.model.attribute.AttributePackage;
import org.eclipse.birt.chart.model.component.ComponentPackage;
import org.eclipse.birt.chart.model.data.DataPackage;
import org.eclipse.birt.chart.model.layout.LayoutPackage;
import org.eclipse.birt.chart.model.type.TypePackage;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;

import io.github.lextpf.birt.chart.stepline.model.type.StepLineFactory;
import io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage;
import io.github.lextpf.birt.chart.stepline.model.type.StepLineSeries;
import io.github.lextpf.birt.chart.stepline.model.type.StepMode;

/**
 * An implementation of the model <b>Package</b>.
 */
public class StepLinePackageImpl extends EPackageImpl implements StepLinePackage {

	private EClass stepLineSeriesEClass = null;

	private EEnum stepModeEEnum = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the
	 * package namespace URI.
	 * <p>
	 * Note: the correct way to create the package is via the static factory method
	 * {@link #init init()}, which also performs initialization of the package, or
	 * returns the registered package, if one already exists.
	 *
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see io.github.lextpf.birt.chart.stepline.model.type.StepLinePackage#eNS_URI
	 * @see #init()
	 */
	private StepLinePackageImpl() {
		super(eNS_URI, StepLineFactory.eINSTANCE);
	}

	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and
	 * for any others upon which it depends.
	 * <p>
	 * This method is used to initialize {@link StepLinePackage#eINSTANCE} when that
	 * field is accessed. Clients should not invoke it directly. Instead, they
	 * should simply access that field to obtain the package.
	 *
	 * @return the initialized package
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 */
	public static StepLinePackage init() {
		if (isInited) {
			return (StepLinePackage) EPackage.Registry.INSTANCE.getEPackage(StepLinePackage.eNS_URI);
		}

		// Obtain or create and register package
		StepLinePackageImpl theStepLinePackage = (StepLinePackageImpl) (EPackage.Registry.INSTANCE
				.get(eNS_URI) instanceof StepLinePackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI)
						: new StepLinePackageImpl());

		isInited = true;

		// STEP_LINE_SERIES__STEP_MODE is compiled from TypePackage.LINE_SERIES_FEATURE_COUNT
		// and javac inlines that constant, so a chart engine whose LineSeries grew a
		// feature would silently collide with ours. Compare against the LineSeries
		// EClass actually present at run time instead.
		if (STEP_LINE_SERIES__STEP_MODE != TypePackage.eINSTANCE.getLineSeries().getFeatureCount()) {
			throw new IllegalStateException(
					"BIRT chart.engine feature layout changed; rebuild io.github.lextpf.birt.chart.stepline against this BIRT version"); //$NON-NLS-1$
		}

		// Initialize simple dependencies
		AttributePackage.eINSTANCE.eClass();
		ComponentPackage.eINSTANCE.eClass();
		DataPackage.eINSTANCE.eClass();
		TypePackage.eINSTANCE.eClass();
		LayoutPackage.eINSTANCE.eClass();
		ModelPackage.eINSTANCE.eClass();
		XMLTypePackage.eINSTANCE.eClass();

		// Create package meta-data objects
		theStepLinePackage.createPackageContents();

		// Initialize created meta-data
		theStepLinePackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theStepLinePackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(StepLinePackage.eNS_URI, theStepLinePackage);
		return theStepLinePackage;
	}

	@Override
	public EClass getStepLineSeries() {
		return stepLineSeriesEClass;
	}

	@Override
	public EAttribute getStepLineSeries_StepMode() {
		return (EAttribute) stepLineSeriesEClass.getEStructuralFeatures().get(0);
	}

	@Override
	public EEnum getStepMode() {
		return stepModeEEnum;
	}

	@Override
	public StepLineFactory getStepLineFactory() {
		return (StepLineFactory) getEFactoryInstance();
	}

	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package. This method is guarded to
	 * have no affect on any invocation but its first.
	 */
	public void createPackageContents() {
		if (isCreated) {
			return;
		}
		isCreated = true;

		// Create classes and their features
		stepLineSeriesEClass = createEClass(STEP_LINE_SERIES);
		createEAttribute(stepLineSeriesEClass, STEP_LINE_SERIES__STEP_MODE);

		// Create enums
		stepModeEEnum = createEEnum(STEP_MODE);
	}

	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model. This method is
	 * guarded to have no affect on any invocation but its first.
	 */
	public void initializePackageContents() {
		if (isInitialized) {
			return;
		}
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Obtain other dependent packages
		TypePackage theTypePackage = (TypePackage) EPackage.Registry.INSTANCE.getEPackage(TypePackage.eNS_URI);

		// Add supertypes to classes
		stepLineSeriesEClass.getESuperTypes().add(theTypePackage.getLineSeries());

		// Initialize classes and features; add operations and parameters
		initEClass(stepLineSeriesEClass, StepLineSeries.class, "StepLineSeries", !IS_ABSTRACT, !IS_INTERFACE, //$NON-NLS-1$
				IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getStepLineSeries_StepMode(), getStepMode(), "stepMode", "After", 0, 1, StepLineSeries.class, //$NON-NLS-1$ //$NON-NLS-2$
				!IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Initialize enums and add enum literals
		initEEnum(stepModeEEnum, StepMode.class, "StepMode"); //$NON-NLS-1$
		addEEnumLiteral(stepModeEEnum, StepMode.AFTER_LITERAL);
		addEEnumLiteral(stepModeEEnum, StepMode.BEFORE_LITERAL);
		addEEnumLiteral(stepModeEEnum, StepMode.CENTER_LITERAL);

		// Create resource
		createResource(eNS_URI);

		// Create annotations
		// http:///org/eclipse/emf/ecore/util/ExtendedMetaData
		createExtendedMetaDataAnnotations();
	}

	/**
	 * Initializes the annotations for
	 * <b>http:///org/eclipse/emf/ecore/util/ExtendedMetaData</b>.
	 */
	protected void createExtendedMetaDataAnnotations() {
		String source = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData"; //$NON-NLS-1$
		addAnnotation(stepLineSeriesEClass, source, new String[] { "name", "StepLineSeries", "kind", "elementOnly" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		addAnnotation(getStepLineSeries_StepMode(), source, new String[] { "kind", "element", "name", "StepMode" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		addAnnotation(stepModeEEnum, source, new String[] { "name", "StepMode" }); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
