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

import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantFactory;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantSeries;
import io.github.lextpf.birt.chart.piecewiseconstant.model.type.StepMode;

/**
 * An implementation of the model <b>Package</b>.
 */
public class PiecewiseConstantPackageImpl extends EPackageImpl implements PiecewiseConstantPackage {

	private EClass piecewiseConstantSeriesEClass = null;

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
	 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage#eNS_URI
	 * @see #init()
	 */
	private PiecewiseConstantPackageImpl() {
		super(eNS_URI, PiecewiseConstantFactory.eINSTANCE);
	}

	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and
	 * for any others upon which it depends.
	 * <p>
	 * This method is used to initialize {@link PiecewiseConstantPackage#eINSTANCE} when that
	 * field is accessed. Clients should not invoke it directly. Instead, they
	 * should simply access that field to obtain the package.
	 *
	 * @return the initialized package
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 */
	public static PiecewiseConstantPackage init() {
		if (isInited) {
			return (PiecewiseConstantPackage) EPackage.Registry.INSTANCE.getEPackage(PiecewiseConstantPackage.eNS_URI);
		}

		// Obtain or create and register package
		PiecewiseConstantPackageImpl thePiecewiseConstantPackage = (PiecewiseConstantPackageImpl) (EPackage.Registry.INSTANCE
				.get(eNS_URI) instanceof PiecewiseConstantPackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI)
						: new PiecewiseConstantPackageImpl());

		isInited = true;

		// PIECEWISE_CONSTANT_SERIES__STEP_MODE is compiled from TypePackage.LINE_SERIES_FEATURE_COUNT
		// and javac inlines that constant, so a chart engine whose LineSeries grew a
		// feature would silently collide with ours. Compare against the LineSeries
		// EClass actually present at run time instead.
		if (PIECEWISE_CONSTANT_SERIES__STEP_MODE != TypePackage.eINSTANCE.getLineSeries().getFeatureCount()) {
			throw new IllegalStateException("BIRT chart.engine feature layout changed; rebuild " //$NON-NLS-1$
					+ "io.github.lextpf.birt.chart.piecewiseconstant against this BIRT version"); //$NON-NLS-1$
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
		thePiecewiseConstantPackage.createPackageContents();

		// Initialize created meta-data
		thePiecewiseConstantPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		thePiecewiseConstantPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(PiecewiseConstantPackage.eNS_URI, thePiecewiseConstantPackage);
		return thePiecewiseConstantPackage;
	}

	@Override
	public EClass getPiecewiseConstantSeries() {
		return piecewiseConstantSeriesEClass;
	}

	@Override
	public EAttribute getPiecewiseConstantSeries_StepMode() {
		return (EAttribute) piecewiseConstantSeriesEClass.getEStructuralFeatures().get(0);
	}

	@Override
	public EEnum getStepMode() {
		return stepModeEEnum;
	}

	@Override
	public PiecewiseConstantFactory getPiecewiseConstantFactory() {
		return (PiecewiseConstantFactory) getEFactoryInstance();
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
		piecewiseConstantSeriesEClass = createEClass(PIECEWISE_CONSTANT_SERIES);
		createEAttribute(piecewiseConstantSeriesEClass, PIECEWISE_CONSTANT_SERIES__STEP_MODE);

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
		piecewiseConstantSeriesEClass.getESuperTypes().add(theTypePackage.getLineSeries());

		// Initialize classes and features; add operations and parameters
		initEClass(piecewiseConstantSeriesEClass, PiecewiseConstantSeries.class, "PiecewiseConstantSeries", !IS_ABSTRACT, !IS_INTERFACE, //$NON-NLS-1$
				IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getPiecewiseConstantSeries_StepMode(), getStepMode(), "stepMode", "After", 0, 1, PiecewiseConstantSeries.class, //$NON-NLS-1$ //$NON-NLS-2$
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
		addAnnotation(piecewiseConstantSeriesEClass, source, new String[] { "name", "PiecewiseConstantSeries", "kind", "elementOnly" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		addAnnotation(getPiecewiseConstantSeries_StepMode(), source, new String[] { "kind", "element", "name", "StepMode" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		addAnnotation(stepModeEEnum, source, new String[] { "name", "StepMode" }); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
