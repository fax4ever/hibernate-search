/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Collections;

import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexCompositeNodeType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

public class ElasticsearchIndexObjectFieldTemplate
		extends AbstractElasticsearchIndexFieldTemplate<ElasticsearchIndexCompositeNodeType> {

	public ElasticsearchIndexObjectFieldTemplate(ElasticsearchIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, ElasticsearchIndexCompositeNodeType type,
			TreeNodeInclusion inclusion, boolean multiValued) {
		super( declaringParent, absolutePathGlob, type, inclusion, multiValued );
	}

	@Override
	protected ElasticsearchIndexField createNode(ElasticsearchIndexCompositeNode parent, String relativePath,
			ElasticsearchIndexCompositeNodeType type, TreeNodeInclusion inclusion, boolean multiValued) {
		return new ElasticsearchIndexObjectField( parent, relativePath, type, inclusion, multiValued,
				Collections.emptyMap() );
	}
}
