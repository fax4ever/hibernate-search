/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final StubSearchIndexScope scope;

	public StubSearchProjectionBuilderFactory(StubSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new DocumentReferenceProjectionBuilder() {
			@Override
			public SearchProjection<DocumentReference> build() {
				return StubDefaultSearchProjection.get();
			}
		};
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return StubEntitySearchProjection::get;
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return StubReferenceSearchProjection::get;
	}

	@Override
	public <I> IdProjectionBuilder<I> id(Class<I> identifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new StubIdSearchProjection.Builder<>(
				identifier.projectionConverter().withConvertedType( identifierType, identifier ) );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new ScoreProjectionBuilder() {
			@Override
			public SearchProjection<Float> build() {
				return StubDefaultSearchProjection.get();
			}
		};
	}

	@Override
	public <T> CompositeProjectionBuilder<T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		return new CompositeProjectionBuilder<T>() {
			@Override
			public SearchProjection<T> build() {
				return new StubCompositeListSearchProjection<>( transformer,
						Arrays.stream( projections ).map( p -> toImplementation( p ) ).collect( Collectors.toList() ) );
			}

			private <U> StubSearchProjection<U> toImplementation(SearchProjection<U> projection) {
				if ( !( projection instanceof StubSearchProjection ) ) {
					throw new AssertionFailure( "Projection " + projection + " must be a StubSearchProjection" );
				}
				return (StubSearchProjection<U>) projection;
			}
		};
	}

	@Override
	public <P, T> CompositeProjectionBuilder<T> composite(Function<P, T> transformer,
			SearchProjection<P> projection) {
		return new CompositeProjectionBuilder<T>() {
			@Override
			public SearchProjection<T> build() {
				return new StubCompositeFunctionSearchProjection<>( transformer, toImplementation( projection ) );
			}
		};
	}

	@Override
	public <P1, P2, T> CompositeProjectionBuilder<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new CompositeProjectionBuilder<T>() {
			@Override
			public SearchProjection<T> build() {
				return new StubCompositeBiFunctionSearchProjection<>( transformer, toImplementation( projection1 ),
						toImplementation( projection2 ) );
			}
		};
	}

	@Override
	public <P1, P2, P3, T> CompositeProjectionBuilder<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new CompositeProjectionBuilder<T>() {
			@Override
			public SearchProjection<T> build() {
				return new StubCompositeTriFunctionSearchProjection<>( transformer, toImplementation( projection1 ),
						toImplementation( projection2 ), toImplementation( projection3 ) );
			}
		};
	}

	private <U> StubSearchProjection<U> toImplementation(SearchProjection<U> projection) {
		if ( !( projection instanceof StubSearchProjection ) ) {
			throw new AssertionFailure( "Projection " + projection + " must be a StubSearchProjection" );
		}
		return (StubSearchProjection<U>) projection;
	}

}
