/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.EntityReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;


public final class EntityReferenceProjectionOptionsStepImpl<R>
		implements EntityReferenceProjectionOptionsStep<EntityReferenceProjectionOptionsStepImpl<R>, R> {

	private final EntityReferenceProjectionBuilder<R> entityReferenceProjectionBuilder;

	public EntityReferenceProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext) {
		this.entityReferenceProjectionBuilder = dslContext.scope().projectionBuilders().entityReference();
	}

	@Override
	/*
	 * The backend has no control over the type of entities.
	 * This cast is only safe because we make sure to only use SearchProjectionFactory
	 * with generic type arguments that are consistent with the type of entity loaders.
	 * See comments in MappedIndexScope.
	 */
	public SearchProjection<R> toProjection() {
		return entityReferenceProjectionBuilder.build();
	}

}
