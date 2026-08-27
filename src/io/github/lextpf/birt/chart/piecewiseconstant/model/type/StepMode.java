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
 * A representation of the literals of the enumeration '<em><b>Step Mode</b></em>',
 * and utility methods for working with them.
 * <p>
 * The mode decides where the piecewise constant line jumps from one value to
 * the next. The names of the literals are kept as they are; the scientific
 * synonyms for each of them are given alongside:
 * <ul>
 * <li><b>After</b> - hold each value until the next point's position: the jump
 * happens at the <em>next</em> point. This is the <em>zero-order hold</em>, or
 * <em>sample-and-hold</em>: the right-continuous (c&agrave;dl&agrave;g) step
 * function.</li>
 * <li><b>Before</b> - jump to the next value at the current position: the value
 * of a point is already in force when the previous point is left. This is the
 * <em>left-continuous step</em>.</li>
 * <li><b>Center</b> - step halfway between two points; on a category axis that
 * half-way position is the category boundary. This is the <em>midpoint
 * step</em>.</li>
 * </ul>
 *
 * @see io.github.lextpf.birt.chart.piecewiseconstant.model.type.PiecewiseConstantPackage#getStepMode()
 */
public enum StepMode implements Enumerator {
	/**
	 * The '<em><b>After</b></em>' literal object: hold each value until the next
	 * point's position - the zero-order hold, or sample-and-hold, the
	 * right-continuous (c&agrave;dl&agrave;g) step function.
	 *
	 * @see #AFTER
	 */
	AFTER_LITERAL(0, "After", "After"), //$NON-NLS-1$ //$NON-NLS-2$
	/**
	 * The '<em><b>Before</b></em>' literal object: jump to the next value at the
	 * current position - the left-continuous step.
	 *
	 * @see #BEFORE
	 */
	BEFORE_LITERAL(1, "Before", "Before"), //$NON-NLS-1$ //$NON-NLS-2$
	/**
	 * The '<em><b>Center</b></em>' literal object: step halfway between points -
	 * the midpoint step; on a category axis this is the category boundary.
	 *
	 * @see #CENTER
	 */
	CENTER_LITERAL(2, "Center", "Center"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * The '<em><b>After</b></em>' literal value.
	 *
	 * @see #AFTER_LITERAL
	 */
	public static final int AFTER = 0;

	/**
	 * The '<em><b>Before</b></em>' literal value.
	 *
	 * @see #BEFORE_LITERAL
	 */
	public static final int BEFORE = 1;

	/**
	 * The '<em><b>Center</b></em>' literal value.
	 *
	 * @see #CENTER_LITERAL
	 */
	public static final int CENTER = 2;

	/**
	 * An array of all the '<em><b>Step Mode</b></em>' enumerators.
	 */
	private static final StepMode[] VALUES_ARRAY = { AFTER_LITERAL, BEFORE_LITERAL, CENTER_LITERAL, };

	/**
	 * A public read-only list of all the '<em><b>Step Mode</b></em>' enumerators.
	 */
	public static final List<StepMode> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Step Mode</b></em>' literal with the specified literal
	 * value.
	 *
	 * @param literal the literal, as written in the chart XML
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
	 * Returns the '<em><b>Step Mode</b></em>' literal with the specified name.
	 *
	 * @param name the enumerator name
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
	 * Returns the '<em><b>Step Mode</b></em>' literal with the specified integer
	 * value.
	 *
	 * @param value the enumerator value
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
	 * Only this class can construct instances.
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
	 * Returns the literal value of the enumerator, which is its string
	 * representation.
	 */
	@Override
	public String toString() {
		return literal;
	}
}
