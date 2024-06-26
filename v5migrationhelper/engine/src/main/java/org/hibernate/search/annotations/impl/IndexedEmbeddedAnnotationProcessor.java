/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

@Deprecated
public class IndexedEmbeddedAnnotationProcessor implements PropertyMappingAnnotationProcessor<IndexedEmbedded> {
	@Override
	public void process(PropertyMappingStep mappingContext, IndexedEmbedded annotation,
			PropertyMappingAnnotationProcessorContext context) {
		String cleanedUpPrefix = annotation.prefix();
		if ( ".".equals( cleanedUpPrefix ) ) {
			cleanedUpPrefix = null;
		}

		Integer cleanedUpMaxDepth = annotation.depth();
		if ( cleanedUpMaxDepth.equals( Integer.MAX_VALUE ) ) {
			cleanedUpMaxDepth = null;
		}

		String[] includePathsArray = annotation.includePaths();
		Set<String> cleanedUpIncludePaths;
		if ( includePathsArray.length > 0 ) {
			cleanedUpIncludePaths = new HashSet<>();
			Collections.addAll( cleanedUpIncludePaths, includePathsArray );
		}
		else {
			cleanedUpIncludePaths = Collections.emptySet();
		}

		Class<?> targetType = annotation.targetElement();
		if ( void.class.equals( targetType ) ) {
			targetType = null;
		}

		mappingContext.indexedEmbedded()
				.prefix( cleanedUpPrefix )
				.structure( ObjectStructure.FLATTENED )
				.includeEmbeddedObjectId( annotation.includeEmbeddedObjectId() )
				.includeDepth( cleanedUpMaxDepth )
				.includePaths( cleanedUpIncludePaths )
				.targetType( targetType );
	}
}
