/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.FacetsCollectorFactory;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

/**
 * @param <F> The type of field values.
 * @param <K> The type of returned value. It can be {@code F}, {@link Double}
 * or a different type if value converters are used.
 */
public class LuceneMetricNumericFieldAggregation<F, E extends Number, K> extends AbstractLuceneNestableAggregation<K> {

	private final Set<String> indexNames;
	private final AbstractLuceneNumericFieldCodec<F, E> codec;
	private final LuceneNumericDomain<E> numericDomain;
	private final Comparator<E> termComparator;
	private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
	private final String operation;

	LuceneMetricNumericFieldAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.indexNames = builder.scope.hibernateSearchIndexNames();
		this.codec = builder.codec;
		this.numericDomain = codec.getDomain();
		this.termComparator = numericDomain.createComparator();
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.operation = builder.operation;
	}

	@Override
	public Extractor<K> request(AggregationRequestContext context) {
		context.requireCollector( FacetsCollectorFactory.INSTANCE );
		return new LuceneNumericMetricFieldAggregationExtraction();
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	private class LuceneNumericMetricFieldAggregationExtraction implements Extractor<K> {

		@Override
		public K extract(AggregationExtractContext context) throws IOException {
			return null;
		}
	}

	public static class Factory<F>
			extends AbstractLuceneCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector,
					F,
					AbstractLuceneNumericFieldCodec<F, ?>> {

		private final String operation;

		public Factory(AbstractLuceneNumericFieldCodec<F, ?> codec, String operation) {
			super( codec );
			this.operation = operation;
		}

		@Override
		public FieldMetricAggregationBuilder.TypeSelector create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( codec, scope, field, operation );
		}
	}

	private static class TypeSelector<F> implements FieldMetricAggregationBuilder.TypeSelector {
		private final AbstractLuceneNumericFieldCodec<F, ?> codec;
		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexValueFieldContext<F> field;
		private final String operation;

		private TypeSelector(AbstractLuceneNumericFieldCodec<F, ?> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field,
				String operation) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
			this.operation = operation;
		}

		@Override
		public <T> Builder<F, ?, T> type(Class<T> expectedType, ValueConvert convert) {
			return new Builder<>( codec, scope, field,
					field.type().projectionConverter( convert ).withConvertedType( expectedType, field ),
					operation
			);
		}
	}

	private static class Builder<F, E extends Number, K> extends AbstractBuilder<K>
			implements FieldMetricAggregationBuilder<K> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
		private final String operation;

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter,
				String operation) {
			super( scope, field );
			this.codec = codec;
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.operation = operation;
		}

		@Override
		public LuceneMetricNumericFieldAggregation<F, E, K> build() {
			return new LuceneMetricNumericFieldAggregation<>( this );
		}
	}
}
