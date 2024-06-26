/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationDefaultValues;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

abstract class AbstractNonFullTextFieldAnnotationProcessor<A extends Annotation>
		extends
		AbstractStandardFieldAnnotationProcessor<A> {

	@Override
	final PropertyMappingStandardFieldOptionsStep<?> initStandardFieldMappingContext(PropertyMappingStep mappingContext,
			A annotation, String fieldName) {
		PropertyMappingNonFullTextFieldOptionsStep<?> fieldContext = initSortableFieldMappingContext(
				mappingContext, annotation, fieldName
		);

		Sortable sortable = getSortable( annotation );
		if ( !Sortable.DEFAULT.equals( sortable ) ) {
			fieldContext.sortable( sortable );
		}

		Aggregable aggregable = getAggregable( annotation );
		if ( !Aggregable.DEFAULT.equals( aggregable ) ) {
			fieldContext.aggregable( aggregable );
		}

		String indexNullAs = getIndexNullAs( annotation );
		if ( indexNullAs != null && !AnnotationDefaultValues.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
			fieldContext.indexNullAs( indexNullAs );
		}

		return fieldContext;
	}

	abstract PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(
			PropertyMappingStep mappingContext,
			A annotation, String fieldName);

	abstract Sortable getSortable(A annotation);

	abstract Aggregable getAggregable(A annotation);

	abstract String getIndexNullAs(A annotation);
}
