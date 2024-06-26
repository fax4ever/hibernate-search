/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * A pattern-matching implementation for generic types that also extracts a type from matching types.
 * <p>
 * For example, such a pattern could be described with the expression {@code Collection<T> => T}.
 * It would only match against {@code Collection} and its subclasses,
 * and would extract the resolved type for parameter {@code T} in the event of a match.
 */
public interface ExtractingTypePatternMatcher extends TypePatternMatcher {

	@Override
	default boolean matches(PojoTypeModel<?> typeToInspect) {
		return extract( typeToInspect ).isPresent();
	}

	/**
	 * Attempts to match a given type against this pattern,
	 * and if matched, returns an upper bound of the extracted type.
	 *
	 * @param typeToInspect A type that may, or may not, match the pattern.
	 * @return The extracted type if there was a match, or an empty {@link Optional} otherwise.
	 */
	Optional<? extends PojoTypeModel<?>> extract(PojoTypeModel<?> typeToInspect);

}
