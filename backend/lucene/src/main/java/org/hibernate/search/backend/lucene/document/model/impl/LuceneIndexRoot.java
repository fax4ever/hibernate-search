/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;


public class LuceneIndexRoot
		implements LuceneIndexCompositeNode, LuceneSearchIndexCompositeNodeContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, AbstractLuceneIndexField> staticChildrenByName;
	private final Map<SearchQueryElementTypeKey<?>, AbstractLuceneCompositeNodeSearchQueryElementFactory<?>>
			queryElementFactories;

	public LuceneIndexRoot(Map<String, AbstractLuceneIndexField> notYetInitializedStaticChildren,
			Map<SearchQueryElementTypeKey<?>, AbstractLuceneCompositeNodeSearchQueryElementFactory<?>>
					queryElementFactories) {
		// We expect the children to be added to the list externally, just after the constructor call.
		this.staticChildrenByName = Collections.unmodifiableMap( notYetInitializedStaticChildren );
		this.queryElementFactories = queryElementFactories;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	public boolean isRoot() {
		return true;
	}

	@Override
	public boolean isObjectField() {
		return false;
	}

	@Override
	public LuceneIndexRoot toComposite() {
		return this;
	}

	@Override
	public LuceneIndexObjectField toObjectField() {
		throw log.invalidIndexElementTypeRootIsNotObjectField();
	}

	@Override
	public String absolutePath() {
		return null;
	}

	@Override
	public String absolutePath(String relativeFieldName) {
		return relativeFieldName;
	}

	@Override
	public IndexFieldInclusion inclusion() {
		return IndexFieldInclusion.INCLUDED;
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return Collections.emptyList();
	}

	@Override
	public Map<String, ? extends AbstractLuceneIndexField> staticChildrenByName() {
		return staticChildrenByName;
	}

	@Override
	public boolean nested() {
		return false;
	}

	@Override
	public boolean multiValuedInRoot() {
		return false;
	}

	@Override
	public EventContext eventContext() {
		return EventContexts.indexSchemaRoot();
	}

	@Override
	public boolean dynamic() {
		return false;
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchIndexScope scope) {
		AbstractLuceneCompositeNodeSearchQueryElementFactory<T> factory = queryElementFactory( key );
		if ( factory == null ) {
			EventContext eventContext = eventContext();
			throw log.cannotUseQueryElementForIndexElement( eventContext, key.toString(),
					log.missingSupportHintForCompositeIndexElement(), eventContext );
		}
		try {
			return factory.create( scope, this );
		}
		catch (SearchException e) {
			EventContext eventContext = eventContext();
			throw log.cannotUseQueryElementForIndexElementBecauseCreationException( eventContext, key.toString(),
					e.getMessage(), e, eventContext );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // The cast is safe because the key type always matches the value type.
	public <T> AbstractLuceneCompositeNodeSearchQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		return (AbstractLuceneCompositeNodeSearchQueryElementFactory<T>) queryElementFactories.get( key );
	}
}