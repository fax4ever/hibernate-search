/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

public interface PojoAdditionalMetadataCollectorPropertyNode extends PojoAdditionalMetadataCollector {

	PojoAdditionalMetadataCollectorValueNode value(ContainerExtractorPath extractorPath);

	void markerBinder(MarkerBinder definition, Map<String, Object> params);

}
