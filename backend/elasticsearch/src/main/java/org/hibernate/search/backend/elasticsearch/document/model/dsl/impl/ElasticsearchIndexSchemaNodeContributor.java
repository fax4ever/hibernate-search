/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;


public interface ElasticsearchIndexSchemaNodeContributor {

	void contribute(ElasticsearchIndexSchemaNodeCollector collector, ElasticsearchIndexCompositeNode parentNode,
			Map<String, AbstractElasticsearchIndexField> staticChildrenByNameForParent,
			AbstractTypeMapping parentMapping);

}