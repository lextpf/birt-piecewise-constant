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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

/**
 * The literals of the enumeration '<em><b>Step Mode</b></em>', and the methods
 * that look up one literal.
 * <p>
 * Intent: the step mode states where the piecewise constant series puts the step
 * between two consecutive values. The renderer places the corner vertex from
 * this choice.
 * <p>
 * Constraints: the literal string of each mode goes into the chart XML. If a
 * literal name changes, then the chart engine cannot read a chart that holds the
 * old name.
 * <p>
 * Side effects: none.
 * <p>
 * Non-obvious behaviour: each mode has an exact mathematical meaning.
 * <ul>
 * <li><b>After</b> - the series holds each value until the position of the next
 * point. The step is at the next point. The function is right-continuous
 * (c&agrave;dl&agrave;g).</li>
 * <li><b>Before</b> - the series takes the value of a point already at the
 * position of the previous point. The function is left-continuous.</li>
 * <li><b>Center</b> - the step is halfway between two points. On a category axis
 * this position is the category boundary.</li>
 * </ul>
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage#getStepMode()
 */
public enum StepMode implements Enumerator {
	/**
	 * The '<em><b>After</b></em>' literal object. The series holds each value until
	 * the position of the next point. The function is right-continuous
	 * (c&agrave;dl&agrave;g). This mode is the default step mode.
	 *
	 * @see #AFTER
	 */
	AFTER_LITERAL(0, "After", "After"), //$NON-NLS-1$ //$NON-NLS-2$
	/**
	 * The '<em><b>Before</b></em>' literal object. The series takes the value of a
	 * point already at the position of the previous point. The function is
	 * left-continuous.
	 *
	 * @see #BEFORE
	 */
	BEFORE_LITERAL(1, "Before", "Before"), //$NON-NLS-1$ //$NON-NLS-2$
	/**
	 * The '<em><b>Center</b></em>' literal object. The step is halfway between two
	 * points. On a category axis this position is the category boundary.
	 *
	 * @see #CENTER
	 */
	CENTER_LITERAL(2, "Center", "Center"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * The integer value of the '<em><b>After</b></em>' literal. EMF stores this
	 * value in the meta model.
	 *
	 * @see #AFTER_LITERAL
	 */
	public static final int AFTER = 0;

	/**
	 * The integer value of the '<em><b>Before</b></em>' literal. EMF stores this
	 * value in the meta model.
	 *
	 * @see #BEFORE_LITERAL
	 */
	public static final int BEFORE = 1;

	/**
	 * The integer value of the '<em><b>Center</b></em>' literal. EMF stores this
	 * value in the meta model.
	 *
	 * @see #CENTER_LITERAL
	 */
	public static final int CENTER = 2;

	/**
	 * All '<em><b>Step Mode</b></em>' literals, in the order of their integer
	 * values. The lookup methods of this class read this array.
	 */
	private static final StepMode[] VALUES_ARRAY = { AFTER_LITERAL, BEFORE_LITERAL, CENTER_LITERAL, };

	/**
	 * All '<em><b>Step Mode</b></em>' literals as a read-only list. EMF reads this
	 * list when it builds the enumeration of the meta model.
	 */
	public static final List<StepMode> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Step Mode</b></em>' literal with the given literal string.
	 * <p>
	 * Constraints: the comparison is case-sensitive. If no literal matches, then the
	 * method returns <code>null</code>.
	 *
	 * @param literal the literal string, as written in the chart XML
	 * @return the matching literal object, or <code>null</code>
	 */
	public static StepMode get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			StepMode result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Step Mode</b></em>' literal with the given name.
	 * <p>
	 * Constraints: the comparison is case-sensitive. If no literal matches, then the
	 * method returns <code>null</code>.
	 *
	 * @param name the name of the literal
	 * @return the matching literal object, or <code>null</code>
	 */
	public static StepMode getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			StepMode result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Step Mode</b></em>' literal with the given integer value.
	 * <p>
	 * Constraints: if no literal has this value, then the method returns
	 * <code>null</code>.
	 *
	 * @param value the integer value of the literal
	 * @return the matching literal object, or <code>null</code>
	 */
	public static StepMode get(int value) {
		switch (value) {
		case AFTER:
			return AFTER_LITERAL;
		case BEFORE:
			return BEFORE_LITERAL;
		case CENTER:
			return CENTER_LITERAL;
		default:
			return null;
		}
	}

	private final int value;

	private final String name;

	private final String literal;

	/**
	 * Builds one literal. Only the literal declarations above can call this
	 * constructor.
	 *
	 * @param value   the integer value of the literal
	 * @param name    the name of the literal
	 * @param literal the literal string, as written in the chart XML
	 */
	StepMode(int value, String name, String literal) {
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLiteral() {
		return literal;
	}

	/**
	 * Returns the literal string of this mode. The EMF serializer writes this string
	 * into the chart XML.
	 *
	 * @return the literal string
	 */
	@Override
	public String toString() {
		return literal;
	}
}
