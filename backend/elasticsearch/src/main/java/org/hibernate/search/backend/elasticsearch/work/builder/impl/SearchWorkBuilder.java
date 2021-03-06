/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.engine.common.timing.Deadline;


public interface SearchWorkBuilder<R> extends ElasticsearchWorkBuilder<NonBulkableWork<R>> {

	SearchWorkBuilder<R> index(URLEncodedString indexName);

	SearchWorkBuilder<R> paging(Integer limit, Integer offset);

	SearchWorkBuilder<R> scrolling(int scrollSize, String scrollTimeout);

	SearchWorkBuilder<R> routingKeys(Set<String> routingKeys);

	SearchWorkBuilder<R> requestTransformer(Function<ElasticsearchRequest, ElasticsearchRequest> requestTransformer);

	SearchWorkBuilder<R> deadline(Deadline deadline, boolean allowPartialResults);

	SearchWorkBuilder<R> disableTrackTotalHits();

	SearchWorkBuilder<R> totalHitCountThreshold(Long totalHitCountThreshold);
}
