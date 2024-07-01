/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.tmp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LuceneMetricAggregationsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final AggregationKey<Integer> sumIntegers = AggregationKey.of( "sumIntegers" );
	private final AggregationKey<Map<Integer, Long>> termIntegers = AggregationKey.of( "termIntegers" );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndexes( mainIndex ).setup().integration();
		initData();
	}

	@Test
	public void test_termIntegers() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.aggregation( termIntegers, f -> f.terms().field( "integer", Integer.class ) )
				.toQuery();

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( termIntegers ) ).isNotNull();
	}

	@Test
	public void test_filteringResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQueryOptionsStep<?, DocumentReference, StubLoadingOptionsStep, ?, ?> options = scope.query()
				.where( f -> f.match().field( "style" ).matching( "bla" ) );
		SearchQuery<DocumentReference> query = defineAggregations( options );

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( sumIntegers ) ).isEqualTo( 29 );
	}

	@Test
	public void test_allResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQueryOptionsStep<?, DocumentReference, StubLoadingOptionsStep, ?, ?> options = scope.query()
				.where( f -> f.matchAll() );
		SearchQuery<DocumentReference> query = defineAggregations( options );

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( sumIntegers ) ).isEqualTo( 55 );
	}

	private SearchQuery<DocumentReference> defineAggregations(
			SearchQueryOptionsStep<?, DocumentReference, StubLoadingOptionsStep, ?, ?> options) {
		return options
				.aggregation( sumIntegers, f -> f.sum().field( "integer", Integer.class ) )
				.toQuery();
	}

	private void initData() {
		int[] integers = new int[] { 9, 18, 3, 18, 7, -10, 3, 0, 7, 0 };
		String[] styles = new String[] { "bla", "aaa" };

		BulkIndexer bulkIndexer = mainIndex.bulkIndexer();
		for ( int i = 0; i < integers.length; i++ ) {
			int value = integers[i];
			String style = styles[i % 2];
			String id = i + ":" + value + ":" + style;

			bulkIndexer.add( id, document -> {
				document.addValue( mainIndex.binding().integer, value );
				document.addValue( mainIndex.binding().converted, value );
				document.addValue( mainIndex.binding().style, style );

				DocumentElement object = document.addObject( mainIndex.binding().object );
				object.addValue( mainIndex.binding().nestedInteger, value );
			} );
		}
		bulkIndexer.add( "empty", document -> {} )
				.join();
	}

	@SuppressWarnings("unused")
	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<Integer> converted;
		final IndexFieldReference<String> style;
		final IndexObjectFieldReference object;
		final IndexFieldReference<Integer> nestedInteger;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().aggregable( Aggregable.YES ) ).toReference();
			converted = root.field( "converted", f -> f.asInteger().aggregable( Aggregable.YES )
					.projectionConverter( String.class, (value, context) -> value.toString() ) ).toReference();
			style = root.field( "style", f -> f.asString() ).toReference();

			IndexSchemaObjectField nested = root.objectField( "object", ObjectStructure.NESTED );
			object = nested.toReference();
			nestedInteger = nested.field( "nestedInteger", f -> f.asInteger().aggregable( Aggregable.YES ) )
					.toReference();
		}
	}
}
