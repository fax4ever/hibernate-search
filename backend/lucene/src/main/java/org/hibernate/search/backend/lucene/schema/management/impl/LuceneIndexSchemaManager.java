/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.schema.management.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.IndexManagementWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

public class LuceneIndexSchemaManager implements IndexSchemaManager {

	private final LuceneWorkFactory luceneWorkFactory;
	private final SchemaManagementIndexManagerContext indexManagerContext;
	private final LuceneIndexSchemaExportImpl export;

	public LuceneIndexSchemaManager(String indexName, LuceneWorkFactory luceneWorkFactory,
			SchemaManagementIndexManagerContext indexManagerContext) {
		this.luceneWorkFactory = luceneWorkFactory;
		this.indexManagerContext = indexManagerContext;
		this.export = new LuceneIndexSchemaExportImpl( indexName );
	}

	@Override
	public CompletableFuture<?> createIfMissing(OperationSubmitter operationSubmitter) {
		return doSubmit( luceneWorkFactory.createIndexIfMissing(), operationSubmitter );
	}

	@Override
	public CompletableFuture<?> createOrValidate(ContextualFailureCollector failureCollector,
			OperationSubmitter operationSubmitter) {
		// We don't perform any validation whatsoever.
		return createIfMissing( operationSubmitter );
	}

	@Override
	public CompletableFuture<?> createOrUpdate(OperationSubmitter operationSubmitter) {
		// We don't perform any update whatsoever.
		return createIfMissing( operationSubmitter );
	}

	@Override
	public CompletableFuture<?> dropIfExisting(OperationSubmitter operationSubmitter) {
		return doSubmit( luceneWorkFactory.dropIndexIfExisting(), operationSubmitter );
	}

	@Override
	public CompletableFuture<?> dropAndCreate(OperationSubmitter operationSubmitter) {
		return doSubmit( luceneWorkFactory.dropIndexIfExisting(), operationSubmitter )
				.thenCompose( ignored -> doSubmit( luceneWorkFactory.createIndexIfMissing(), operationSubmitter ) );
	}

	@Override
	public CompletableFuture<?> validate(ContextualFailureCollector failureCollector,
			OperationSubmitter operationSubmitter) {
		// We only check that the index exists, and we throw an exception if it doesn't.
		return doSubmit( luceneWorkFactory.validateIndexExists(), operationSubmitter );
	}

	@Override
	public void exportExpectedSchema(IndexSchemaCollector collector) {
		collector.indexSchema( indexManagerContext.backendName(), this.export.indexName(), this.export );
	}

	public CompletableFuture<Long> computeSizeInBytes(OperationSubmitter operationSubmitter) {
		IndexManagementWork<Long> computeSizeWork = luceneWorkFactory.computeSizeInBytes();
		BinaryOperator<Long> add = Math::addExact;

		CompletableFuture<Long> totalSizeFuture = CompletableFuture.completedFuture( 0L );
		for ( LuceneParallelWorkOrchestrator orchestrator : indexManagerContext.allManagementOrchestrators() ) {
			CompletableFuture<Long> shardSizeFuture = orchestrator.submit( computeSizeWork, operationSubmitter );
			totalSizeFuture = totalSizeFuture.thenCombine( shardSizeFuture, add );
		}
		return totalSizeFuture;
	}

	private CompletableFuture<?> doSubmit(IndexManagementWork<?> work, OperationSubmitter operationSubmitter) {
		Collection<LuceneParallelWorkOrchestrator> orchestrators =
				indexManagerContext.allManagementOrchestrators();
		CompletableFuture<?>[] futures = new CompletableFuture[orchestrators.size()];
		int i = 0;
		for ( LuceneParallelWorkOrchestrator orchestrator : orchestrators ) {
			futures[i] = orchestrator.submit( work, operationSubmitter );
			++i;
		}
		return CompletableFuture.allOf( futures );
	}
}
