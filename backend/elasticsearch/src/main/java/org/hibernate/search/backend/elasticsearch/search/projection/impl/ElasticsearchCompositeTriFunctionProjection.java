/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.util.common.function.TriFunction;

class ElasticsearchCompositeTriFunctionProjection<P1, P2, P3, P>
		extends AbstractElasticsearchCompositeProjection<P> {

	private final TriFunction<P1, P2, P3, P> transformer;

	ElasticsearchCompositeTriFunctionProjection(ElasticsearchSearchIndexScope<?> scope, TriFunction<P1, P2, P3, P> transformer,
			ElasticsearchSearchProjection<?, P1> projection1, ElasticsearchSearchProjection<?, P2> projection2,
			ElasticsearchSearchProjection<?, P3> projection3) {
		super( scope, projection1, projection2, projection3 );
		this.transformer = transformer;
	}

	@Override
	@SuppressWarnings("unchecked")
	P doTransform(Object[] childResults) {
		return transformer.apply( (P1) childResults[0], (P2) childResults[1], (P3) childResults[2] );
	}
}
