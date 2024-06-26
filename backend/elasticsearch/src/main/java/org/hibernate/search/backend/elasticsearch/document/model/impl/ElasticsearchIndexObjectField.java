/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexCompositeNodeType;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexObjectField;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;

public final class ElasticsearchIndexObjectField
		extends AbstractIndexObjectField<
				ElasticsearchIndexObjectField,
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchIndexCompositeNodeType,
				ElasticsearchIndexCompositeNode,
				ElasticsearchIndexField>
		implements ElasticsearchIndexCompositeNode, ElasticsearchIndexField {

	public ElasticsearchIndexObjectField(ElasticsearchIndexCompositeNode parent, String relativeFieldName,
			ElasticsearchIndexCompositeNodeType type, TreeNodeInclusion inclusion, boolean multiValued,
			Map<String, ElasticsearchIndexField> notYetInitializedStaticChildren) {
		super( parent, relativeFieldName, type, inclusion, multiValued, notYetInitializedStaticChildren );
	}

	@Override
	protected ElasticsearchIndexObjectField self() {
		return this;
	}

	@Override
	public ElasticsearchIndexCompositeNode toComposite() {
		return this;
	}

	@Override
	public ElasticsearchIndexValueField<?> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

}
