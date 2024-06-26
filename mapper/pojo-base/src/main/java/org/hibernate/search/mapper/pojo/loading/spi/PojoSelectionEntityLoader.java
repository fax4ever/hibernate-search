/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.common.timing.Deadline;

/**
 * A loader for loading a small selection of entities, used in particular during search.
 * <p>
 * Compared to {@link PojoMassEntityLoader}, this loader:
 * <ul>
 *     <li>Receives batches of identifiers from the caller.</li>
 *     <li>Is expected to load a small number of entities, potentially in a single batch.</li>
 *     <li>Returns loaded entities as a list.</li>
 *     <li>Relies on a pre-existing loading context (a session, a transaction, ...).</li>
 *     <li>Must ensure entities remain usable (lazy-loading, ...) as long as the supporting context is active.</li>
 * </ul>
 *
 * @param <E> A supertype of the type of loaded entities.
 */
public interface PojoSelectionEntityLoader<E> {

	/**
	 * Loads the entities corresponding to the given identifiers, blocking the current thread while doing so.
	 *
	 * @param identifiers A list of identifiers for objects to load.
	 * @param deadline The deadline for loading the entities, or {@code null} if there is no deadline.
	 * Should be complied with on a best-effort basis: it's acceptable to ignore it,
	 * but it means some timeouts in Hibernate Search will not work properly.
	 * @return A list of entities, in the same order the references were given.
	 * {@code null} is inserted when an object is not found or has an excluded types
	 * (see {@link PojoSelectionLoadingStrategy#createEntityLoader(Set, PojoSelectionLoadingContext)}).
	 */
	List<E> loadBlocking(List<?> identifiers, Deadline deadline);

}
