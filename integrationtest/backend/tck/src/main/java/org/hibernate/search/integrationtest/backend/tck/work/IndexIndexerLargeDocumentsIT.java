/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.data.TextContent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.awaitility.Awaitility;

/**
 * Test the {@link IndexIndexer} when indexing many large documents, or a single very large document.
 * <p>
 * This is relevant when using AWS authentication in particular,
 * because large documents cause large requests, which causes a different behavior to compute the content length,
 * and AWS request signing is sensitive to the content length.
 */
@TestForIssue(jiraKey = "HSEARCH-4239")
class IndexIndexerLargeDocumentsIT {

	// This must be high, otherwise the bug won't be reproduced.
	private static final int MANY_LARGE_DOCUMENTS_COUNT = 2000;

	private static final String GREAT_EXPECTATIONS;
	static {
		try {
			GREAT_EXPECTATIONS = TextContent.readGreatExpectations();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private final DocumentRefreshStrategy refreshStrategy;

	public IndexIndexerLargeDocumentsIT() {
		refreshStrategy = TckConfiguration.get().getBackendFeatures().supportsExplicitRefresh()
				? DocumentRefreshStrategy.NONE
				// This will perform poorly, but we don't have a choice.
				: DocumentRefreshStrategy.FORCE;
	}

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	void manyLargeDocuments_add() {
		doTest( MANY_LARGE_DOCUMENTS_COUNT, this::largeValue, Operation.ADD );
	}

	@Test
	void manyLargeDocuments_addOrUpdate() {
		doTest( MANY_LARGE_DOCUMENTS_COUNT, this::largeValue, Operation.ADD_OR_UPDATE );
	}

	@Test
	void oneVeryLargeDocuments_add() {
		doTest( 1, this::veryLargeValue, Operation.ADD );
	}

	@Test
	void oneVeryLargeDocuments_addOrUpdate() {
		doTest( 1, this::veryLargeValue, Operation.ADD_OR_UPDATE );
	}

	private void doTest(int count, IntFunction<String> valueProvider, Operation operation) {
		assertThatQuery( index.query()
				.where( f -> f.matchAll() ) )
				.hasNoHits();

		indexAndWait( count, valueProvider, operation );

		if ( refreshStrategy == DocumentRefreshStrategy.NONE ) {
			// If the operation itself did not refresh, do it now.
			index.createWorkspace().refresh( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();
		}

		assertThatQuery( index.query()
				.where( f -> f.matchAll() ) )
				.hasTotalHitCount( count );
	}

	private void indexAndWait(int count, IntFunction<String> valueProvider, Operation operation) {
		IndexIndexer indexer = index.createIndexer();
		CompletableFuture<?>[] tasks = new CompletableFuture<?>[count];
		for ( int i = 0; i < count; i++ ) {
			final int id = i;
			tasks[i] = operation.apply( indexer, referenceProvider( String.valueOf( id ) ),
					document -> document.addValue( index.binding().content, valueProvider.apply( id ) ),
					refreshStrategy );
		}
		CompletableFuture<?> future = CompletableFuture.allOf( tasks );
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		assertThatFuture( future ).isSuccessful();
	}

	private String veryLargeValue(int id) {
		return GREAT_EXPECTATIONS;
	}

	private String largeValue(int id) {
		int returnedSize = 10_000;
		int length = GREAT_EXPECTATIONS.length();

		int start = ( id * returnedSize ) % length;
		int end = start + returnedSize;
		if ( end > length ) {
			end = length;
		}
		return GREAT_EXPECTATIONS.substring( start, end );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> content;

		IndexBinding(IndexSchemaElement root) {
			content = root.field(
					"content",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
		}
	}

	private enum Operation {
		ADD {
			@Override
			public CompletableFuture<?> apply(IndexIndexer indexer, DocumentReferenceProvider referenceProvider,
					DocumentContributor documentContributor, DocumentRefreshStrategy refreshStrategy) {
				return indexer.add( referenceProvider, documentContributor,
						DocumentCommitStrategy.NONE, refreshStrategy, OperationSubmitter.blocking() );
			}
		},
		ADD_OR_UPDATE {
			@Override
			public CompletableFuture<?> apply(IndexIndexer indexer, DocumentReferenceProvider referenceProvider,
					DocumentContributor documentContributor, DocumentRefreshStrategy refreshStrategy) {
				return indexer.addOrUpdate( referenceProvider, documentContributor,
						DocumentCommitStrategy.NONE, refreshStrategy, OperationSubmitter.blocking()
				);
			}
		};

		public abstract CompletableFuture<?> apply(IndexIndexer indexer, DocumentReferenceProvider referenceProvider,
				DocumentContributor documentContributor, DocumentRefreshStrategy refreshStrategy);
	}
}
