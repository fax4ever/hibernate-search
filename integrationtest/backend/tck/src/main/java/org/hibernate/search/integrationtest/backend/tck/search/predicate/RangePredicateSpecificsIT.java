/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BooleanFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.impl.integrationtest.common.assertion.NormalizedDocRefHit;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RangePredicateSpecificsIT<F> {
	private static final List<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new ArrayList<>();
	private static final List<DataSet<?>> dataSets = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();
	static {
		for ( StandardFieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAllStandard() ) {
			if ( !GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType )
					// Booleans don't have enough values to run this test. See BooleanSortAndRangePredicateIT.
					&& !BooleanFieldTypeDescriptor.INSTANCE.equals( fieldType ) ) {
				supportedFieldTypes.add( fieldType );
			}
		}
		for ( StandardFieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
			DataSet<?> dataSet = new DataSet<>( new RangePredicateTestValues<>( fieldType ) );
			dataSets.add( dataSet );
			parameters.add( Arguments.of( dataSet ) );
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer bulkIndexer = index.bulkIndexer();
		dataSets.forEach( dataSet -> dataSet.contribute( bulkIndexer ) );
		bulkIndexer.join();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void atLeast(DataSet<F> dataSet) {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.atLeast( value( docOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal, null, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void atLeast_withDslConverter_valueConvertDefault(DataSet<F> dataSet) {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.atLeast( wrappedValue( docOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal, null, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void atLeast_withDslConverter_valueConvertNo(DataSet<F> dataSet) {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.atLeast( value( docOrdinal, dataSet ), ValueModel.INDEX ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal, null, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void greaterThan(DataSet<F> dataSet) {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.greaterThan( value( docOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal + 1, null, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void greaterThan_withDslConverter_valueConvertDefault(DataSet<F> dataSet) {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.greaterThan( wrappedValue( docOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal + 1, null, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void greaterThan_withDslConverter_valueConvertNo(DataSet<F> dataSet) {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.greaterThan( value( docOrdinal, dataSet ), ValueModel.INDEX ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal + 1, null, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void atMost(DataSet<F> dataSet) {
		int docOrdinal = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.atMost( value( docOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void atMost_withDslConverter_valueConvertDefault(DataSet<F> dataSet) {
		int docOrdinal = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.atMost( wrappedValue( docOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void atMost_withDslConverter_valueConvertNo(DataSet<F> dataSet) {
		int docOrdinal = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.atMost( value( docOrdinal, dataSet ), ValueModel.INDEX ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void lessThan(DataSet<F> dataSet) {
		int docOrdinal = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.lessThan( value( docOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal - 1, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void lessThan_withDslConverter_valueConvertDefault(DataSet<F> dataSet) {
		int docOrdinal = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.lessThan( wrappedValue( docOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal - 1, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void lessThan_withDslConverter_valueConvertNo(DataSet<F> dataSet) {
		int docOrdinal = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.lessThan( value( docOrdinal, dataSet ), ValueModel.INDEX ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal - 1, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void between(DataSet<F> dataSet) {
		int lowerValueNumber = 1;
		int upperValueNumber = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.between( value( lowerValueNumber, dataSet ), value( upperValueNumber, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void between_withDslConverter_valueConvertDefault(DataSet<F> dataSet) {
		int lowerValueNumber = 1;
		int upperValueNumber = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.between( wrappedValue( lowerValueNumber, dataSet ), wrappedValue( upperValueNumber, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void between_withDslConverter_valueConvertNo(DataSet<F> dataSet) {
		int lowerValueNumber = 1;
		int upperValueNumber = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.between( value( lowerValueNumber, dataSet ), value( upperValueNumber, dataSet ),
								ValueModel.INDEX ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void between_boundInclusion(DataSet<F> dataSet) {
		int lowerValueNumber = 1;
		int upperValueNumber = docCount( dataSet ) - 2;

		// Default is including both bounds
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.between( value( lowerValueNumber, dataSet ), value( upperValueNumber, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber, dataSet ) );

		// explicit exclusion for the lower bound
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.between( value( lowerValueNumber, dataSet ), RangeBoundInclusion.EXCLUDED,
								value( upperValueNumber, dataSet ), RangeBoundInclusion.INCLUDED ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber + 1, upperValueNumber, dataSet ) );

		// explicit exclusion for the upper bound
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.between( value( lowerValueNumber, dataSet ), RangeBoundInclusion.INCLUDED,
								value( upperValueNumber, dataSet ), RangeBoundInclusion.EXCLUDED ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber - 1, dataSet ) );

		// explicit inclusion for both bounds
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.between( value( lowerValueNumber, dataSet ), RangeBoundInclusion.INCLUDED,
								value( upperValueNumber, dataSet ), RangeBoundInclusion.INCLUDED ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber, dataSet ) );

		// explicit exclusion for both bounds
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.between( value( lowerValueNumber, dataSet ), RangeBoundInclusion.EXCLUDED,
								value( upperValueNumber, dataSet ), RangeBoundInclusion.EXCLUDED ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber + 1, upperValueNumber - 1, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void between_nullBounds(DataSet<F> dataSet) {
		int lowerDocOrdinal = 1;
		int upperDocOrdinal = docCount( dataSet ) - 2;

		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.between( value( lowerDocOrdinal, dataSet ), null ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerDocOrdinal, null, dataSet ) );

		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.between( null, value( upperDocOrdinal, dataSet ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, upperDocOrdinal, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withinAny(DataSet<F> dataSet) {
		int lowerValueNumber1 = 1;
		int upperValueNumber1 = docCount( dataSet ) - 3;

		int lowerValueNumber2 = 2;
		int upperValueNumber2 = docCount( dataSet ) - 2;

		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath( dataSet ) )
						.withinAny( Range.between( value( lowerValueNumber1, dataSet ), value( upperValueNumber1, dataSet ) ),
								Range.between( value( lowerValueNumber2, dataSet ), value( upperValueNumber2, dataSet ) ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber1, upperValueNumber2, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withinAny_withDslConverter_valueConvertDefault(DataSet<F> dataSet) {
		int lowerValueNumber1 = 1;
		int upperValueNumber1 = docCount( dataSet ) - 3;

		int lowerValueNumber2 = 2;
		int upperValueNumber2 = docCount( dataSet ) - 2;

		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.withinAny( List.of(
								Range.between( wrappedValue( lowerValueNumber1, dataSet ),
										wrappedValue( upperValueNumber1, dataSet ) ),
								Range.between( wrappedValue( lowerValueNumber2, dataSet ),
										wrappedValue( upperValueNumber2, dataSet ) ) ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber1, upperValueNumber2, dataSet ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withinAny_withDslConverter_valueConvertNo(DataSet<F> dataSet) {
		int lowerValueNumber1 = 1;
		int upperValueNumber1 = docCount( dataSet ) - 3;

		int lowerValueNumber2 = 2;
		int upperValueNumber2 = docCount( dataSet ) - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath( dataSet ) )
						.withinAny( List.of(
								Range.between( value( lowerValueNumber1, dataSet ), value( upperValueNumber1, dataSet ) ),
								Range.between( value( lowerValueNumber2, dataSet ), value( upperValueNumber2, dataSet ) )
						), ValueModel.INDEX )
				) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber1, upperValueNumber2, dataSet ) );
	}

	private String defaultDslConverterFieldPath(DataSet<F> dataSet) {
		return index.binding().defaultDslConverterField.get( dataSet.fieldType ).relativeFieldName;
	}

	private String customDslConverterFieldPath(DataSet<F> dataSet) {
		return index.binding().customDslConverterField.get( dataSet.fieldType ).relativeFieldName;
	}

	private Consumer<NormalizedDocRefHit.Builder> docIdRange(Integer firstIncludedOrNull, Integer lastIncludedOrNull,
			DataSet<F> dataSet) {
		int firstIncluded = firstIncludedOrNull == null ? 0 : firstIncludedOrNull;
		int lastIncluded = lastIncludedOrNull == null ? docCount( dataSet ) - 1 : lastIncludedOrNull;
		return b -> {
			for ( int i = firstIncluded; i <= lastIncluded; i++ ) {
				b.doc( index.typeName(), dataSet.docId( i ) );
			}
		};
	}

	private F value(int docOrdinal, DataSet<F> dataSet) {
		return dataSet.values.matchingValue( docOrdinal );
	}

	private ValueWrapper<F> wrappedValue(int docOrdinal, DataSet<F> dataSet) {
		return new ValueWrapper<>( value( docOrdinal, dataSet ) );
	}

	private int docCount(DataSet<F> dataSet) {
		return dataSet.values.size();
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType defaultDslConverterField;
		final SimpleFieldModelsByType customDslConverterField;

		IndexBinding(IndexSchemaElement root) {
			defaultDslConverterField = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"defaultDslConverterField_" );
			customDslConverterField = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"customDslConverterField_",
					c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) );
		}
	}

	private static final class DataSet<F> extends AbstractPerFieldTypePredicateDataSet<F, RangePredicateTestValues<F>> {
		public DataSet(RangePredicateTestValues<F> values) {
			super( values );
		}

		public void contribute(BulkIndexer indexer) {
			for ( int i = 0; i < values.size(); i++ ) {
				F value = values.fieldValue( i );
				indexer.add( docId( i ), routingKey, document -> initDocument( document, value ) );
			}
		}

		private void initDocument(DocumentElement document, F fieldValue) {
			IndexBinding binding = index.binding();
			document.addValue( binding.defaultDslConverterField.get( fieldType ).reference, fieldValue );
			document.addValue( binding.customDslConverterField.get( fieldType ).reference, fieldValue );
		}
	}
}
