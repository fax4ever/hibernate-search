/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;

/**
 * The initial and final step in a "searchable/projectable" index field type definition, where optional parameters can be set.
 * <p>
 * Ths step combines optional parameters common to other more specific index field types like "standard" or "vector".
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
public interface SearchableProjectableIndexFieldTypeOptionsStep<
		S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>,
		F>
		extends IndexFieldTypeOptionsStep<S, F> {

	/**
	 * @param searchable Whether this field should be searchable.
	 * @return {@code this}, for method chaining.
	 * @see Searchable
	 */
	S searchable(Searchable searchable);

	/**
	 * @param projectable Defines whether this field should be projectable.
	 * @return {@code this}, for method chaining.
	 */
	S projectable(Projectable projectable);

	/**
	 * @param indexNullAs A value used instead of null values when indexing.
	 * @return {@code this}, for method chaining.
	 */
	S indexNullAs(F indexNullAs);

}
