/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.metadata;

import org.hibernate.search.mapper.javabean.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.javabean.loading.SelectionLoadingStrategy;

/**
 * A context allowing the definition of entity configuration.
 *
 * @see EntityConfigurer
 */
public interface EntityConfigurationContext<E> {

	/**
	 * @param strategy The strategy for selection loading, used in particular during search.
	 */
	void selectionLoadingStrategy(SelectionLoadingStrategy<? super E> strategy);

	/**
	 * @param strategy The strategy for mass loading, used in particular during mass indexing.
	 */
	void massLoadingStrategy(MassLoadingStrategy<? super E, ?> strategy);

}