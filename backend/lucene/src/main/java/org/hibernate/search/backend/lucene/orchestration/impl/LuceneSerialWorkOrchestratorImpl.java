/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.data.impl.SimpleHashFunction;
import org.hibernate.search.util.common.impl.Closer;

public class LuceneSerialWorkOrchestratorImpl
		extends AbstractWorkOrchestrator<LuceneBatchedWork<?>>
		implements LuceneSerialWorkOrchestrator {

	private static final ConfigurationProperty<Integer> QUEUE_COUNT =
			ConfigurationProperty.forKey( LuceneIndexSettings.INDEXING_QUEUE_COUNT )
					.asInteger()
					.withDefault( LuceneIndexSettings.Defaults.INDEXING_QUEUE_COUNT )
					.build();

	private static final ConfigurationProperty<Integer> QUEUE_SIZE =
			ConfigurationProperty.forKey( LuceneIndexSettings.INDEXING_QUEUE_SIZE )
					.asInteger()
					.withDefault( LuceneIndexSettings.Defaults.INDEXING_QUEUE_SIZE )
					.build();

	private final LuceneBatchedWorkProcessor processor;
	private final BackendThreads threads;
	private final FailureHandler failureHandler;

	private BatchingExecutor<LuceneBatchedWorkProcessor>[] executors;

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param processor A processor to use in the background thread.
	 * @param threads The threads for this backend.
	 * @param failureHandler A failure handler to report failures of the background thread.
	 */
	public LuceneSerialWorkOrchestratorImpl(
			String name, LuceneBatchedWorkProcessor processor,
			BackendThreads threads,
			FailureHandler failureHandler) {
		super( name );
		this.processor = processor;
		this.threads = threads;
		this.failureHandler = failureHandler;
	}

	@Override
	public void forceCommitInCurrentThread() {
		processor.forceCommit();
	}

	@Override
	public void forceRefreshInCurrentThread() {
		processor.forceRefresh();
	}

	@Override
	@SuppressWarnings("unchecked") // We aren't allowed to create generic arrays, so we have to use a raw type here.
	protected void doStart(ConfigurationPropertySource propertySource) {
		int queueCount = QUEUE_COUNT.get( propertySource );
		int queueSize = QUEUE_SIZE.get( propertySource );

		executors = new BatchingExecutor[queueCount];
		for ( int i = 0; i < executors.length; i++ ) {
			executors[i] = new BatchingExecutor<>(
					name() + " - " + i,
					processor,
					queueSize,
					true,
					failureHandler
			);
		}

		for ( BatchingExecutor<?> executor : executors ) {
			executor.start( threads.getWriteExecutor() );
		}
	}

	@Override
	protected void doSubmit(LuceneBatchedWork<?> work) throws InterruptedException {
		SimpleHashFunction.pick( executors, work.getQueuingKey() )
				.submit( work );
	}

	@Override
	protected CompletableFuture<?> completion() {
		CompletableFuture<?>[] completions = new CompletableFuture[executors.length];
		for ( int i = 0; i < executors.length; i++ ) {
			completions[i] = executors[i].completion();
		}
		return CompletableFuture.allOf( completions );
	}

	@Override
	protected void doStop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( BatchingExecutor::stop, executors );
		}
	}

}
