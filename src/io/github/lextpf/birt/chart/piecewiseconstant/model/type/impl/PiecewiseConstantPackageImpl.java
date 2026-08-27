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
 * The implementation of the model <b>Package</b>.
 * <p>
 * Intent: the class builds the meta model of the piecewise constant series and
 * registers it with EMF.
 * <p>
 * Constraints: callers must not build this class directly. They read
 * {@link PiecewiseConstantPackage#eINSTANCE}, which calls {@link #init()}.
 * <p>
 * Side effects: {@link #init()} writes the package into the global
 * {@code EPackage.Registry.INSTANCE}, and it also builds the BIRT model packages
 * that this package depends on.
 */
public class PiecewiseConstantPackageImpl extends EPackageImpl implements PiecewiseConstantPackage {

	private EClass piecewiseConstantSeriesEClass = null;

	private EEnum stepModeEEnum = null;

	/**
	 * Builds the package object and enters it into
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} under the
	 * namespace URI of the package.
	 * <p>
	 * Constraints: the package is empty after this call. Only {@link #init()} builds
	 * the meta model, so every caller must use {@link #init()}.
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
	 * Builds, registers and initializes the <b>Package</b> of this model, and the
	 * BIRT packages that this model depends on.
	 * <p>
	 * Intent: the field {@link PiecewiseConstantPackage#eINSTANCE} calls this
	 * method. Callers must read that field and must not call this method directly.
	 * <p>
	 * Constraints: the method builds the meta model on the first call only. Every
	 * later call returns the registered package.
	 * <p>
	 * Side effects: the method puts the package into the global
	 * {@code EPackage.Registry.INSTANCE}, and it freezes the meta model. A frozen
	 * meta model is read-only.
	 * <p>
	 * Non-obvious behaviour: the method compares the compiled feature id of the step
	 * mode against the feature count of the {@code LineSeries} of the run time. On a
	 * mismatch the method throws an {@link IllegalStateException}.
	 *
	 * @return the initialized package
	 * @throws IllegalStateException if the feature count of BIRT's
	 *                               {@code LineSeries} differs from the count that
	 *                               this plug-in was compiled against
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 */
	public static PiecewiseConstantPackage init() {
		if (isInited) {
			return (PiecewiseConstantPackage) EPackage.Registry.INSTANCE.getEPackage(PiecewiseConstantPackage.eNS_URI);
		}

		// Take the registered package, or build and register a new one.
		PiecewiseConstantPackageImpl thePiecewiseConstantPackage = (PiecewiseConstantPackageImpl) (EPackage.Registry.INSTANCE
				.get(eNS_URI) instanceof PiecewiseConstantPackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI)
						: new PiecewiseConstantPackageImpl());

		isInited = true;

		// javac inlines TypePackage.LINE_SERIES_FEATURE_COUNT into
		// PIECEWISE_CONSTANT_SERIES__STEP_MODE. If the LineSeries of the chart engine
		// holds one more feature than at compile time, then the two feature ids
		// collide. The comparison below reads the LineSeries of the run time and stops
		// the plug-in before the collision damages the data.
		if (PIECEWISE_CONSTANT_SERIES__STEP_MODE != TypePackage.eINSTANCE.getLineSeries().getFeatureCount()) {
			throw new IllegalStateException("BIRT chart.engine feature layout changed; rebuild " //$NON-NLS-1$
					+ "io.github.lextpf.birt.chart.piecewiseconstant against this BIRT version"); //$NON-NLS-1$
		}

		// Build the BIRT packages that this package depends on.
		AttributePackage.eINSTANCE.eClass();
		ComponentPackage.eINSTANCE.eClass();
		DataPackage.eINSTANCE.eClass();
		TypePackage.eINSTANCE.eClass();
		LayoutPackage.eINSTANCE.eClass();
		ModelPackage.eINSTANCE.eClass();
		XMLTypePackage.eINSTANCE.eClass();

		// Build the meta objects of this package.
		thePiecewiseConstantPackage.createPackageContents();

		// Fill in the meta objects.
		thePiecewiseConstantPackage.initializePackageContents();

		// Freeze the meta model. It is read-only from here on.
		thePiecewiseConstantPackage.freeze();

		// Register the package under its namespace URI and return it.
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
	 * Builds the empty meta objects of this package.
	 * <p>
	 * Constraints: the method builds the meta objects on the first call only. A
	 * later call returns at once. {@link #init()} is the only caller in this
	 * plug-in.
	 */
	public void createPackageContents() {
		if (isCreated) {
			return;
		}
		isCreated = true;

		// Build the classes and their features.
		piecewiseConstantSeriesEClass = createEClass(PIECEWISE_CONSTANT_SERIES);
		createEAttribute(piecewiseConstantSeriesEClass, PIECEWISE_CONSTANT_SERIES__STEP_MODE);

		// Build the enumerations.
		stepModeEEnum = createEEnum(STEP_MODE);
	}

	private boolean isInitialized = false;

	/**
	 * Fills in the meta objects of this package.
	 * <p>
	 * Constraints: the method does its work on the first call only. A later call
	 * returns at once. The caller must run {@link #createPackageContents()} first,
	 * and BIRT's {@code TypePackage} must be registered.
	 * <p>
	 * Side effects: the method makes BIRT's <code>LineSeries</code> the supertype of
	 * the piecewise constant series, and it builds an EMF resource for the namespace
	 * URI.
	 */
	public void initializePackageContents() {
		if (isInitialized) {
			return;
		}
		isInitialized = true;

		// Name the package.
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Take the BIRT type package from the registry.
		TypePackage theTypePackage = (TypePackage) EPackage.Registry.INSTANCE.getEPackage(TypePackage.eNS_URI);

		// Make LineSeries the supertype of the piecewise constant series.
		piecewiseConstantSeriesEClass.getESuperTypes().add(theTypePackage.getLineSeries());

		// Fill in the classes and the features.
		initEClass(piecewiseConstantSeriesEClass, PiecewiseConstantSeries.class, "PiecewiseConstantSeries", !IS_ABSTRACT, !IS_INTERFACE, //$NON-NLS-1$
				IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getPiecewiseConstantSeries_StepMode(), getStepMode(), "stepMode", "After", 0, 1, PiecewiseConstantSeries.class, //$NON-NLS-1$ //$NON-NLS-2$
				!IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Fill in the enumeration and add its literals.
		initEEnum(stepModeEEnum, StepMode.class, "StepMode"); //$NON-NLS-1$
		addEEnumLiteral(stepModeEEnum, StepMode.AFTER_LITERAL);
		addEEnumLiteral(stepModeEEnum, StepMode.BEFORE_LITERAL);
		addEEnumLiteral(stepModeEEnum, StepMode.CENTER_LITERAL);

		// Build the EMF resource of the package.
		createResource(eNS_URI);

		// Add the annotations that name the XML elements.
		// http:///org/eclipse/emf/ecore/util/ExtendedMetaData
		createExtendedMetaDataAnnotations();
	}

	/**
	 * Adds the annotations of the source
	 * <b>http:///org/eclipse/emf/ecore/util/ExtendedMetaData</b>.
	 * <p>
	 * Intent: these annotations state the names that the serializer writes into the
	 * chart XML. The class becomes the element <code>PiecewiseConstantSeries</code>,
	 * and the attribute becomes the child element <code>StepMode</code>.
	 * <p>
	 * Constraints: a change to one of these names makes a saved chart unreadable.
	 */
	protected void createExtendedMetaDataAnnotations() {
		String source = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData"; //$NON-NLS-1$
		addAnnotation(piecewiseConstantSeriesEClass, source, new String[] { "name", "PiecewiseConstantSeries", "kind", "elementOnly" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		addAnnotation(getPiecewiseConstantSeries_StepMode(), source, new String[] { "kind", "element", "name", "StepMode" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		addAnnotation(stepModeEEnum, source, new String[] { "name", "StepMode" }); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
