/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.ThreadSpy;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.massindexing.MassIndexer;
import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingStrategies;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;

/**
 * Test interruption of a currently executing {@link MassIndexer}.
 */
public class MassIndexingInterruptionIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper
			= JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public ThreadSpy threadSpy = new ThreadSpy();

	private SearchMapping mapping;

	private Map<Integer, Book> booksmap = new LinkedHashMap<>();

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.INDEX );

		mapping = setupHelper.start()
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void interrupt() {
		int expectedThreadCount = 1 // Workspace
				+ 1 // ID loading
				+ 1; // Entity loading

		AtomicBoolean interrupted = new AtomicBoolean( false );
		AtomicBoolean interruptFlagAfterInterruption = new AtomicBoolean( false );

		Thread massIndexingThread = new Thread( () -> {
			MassIndexer massIndexer = prepareMassIndexingThatWillNotTerminate();
			try {
				massIndexer.startAndWait();
			}
			catch (InterruptedException e) {
				interrupted.set( true );
				interruptFlagAfterInterruption.set( Thread.currentThread().isInterrupted() );
			}
		} );

		massIndexingThread.start();

		waitForMassIndexingThreadsToSpawn( expectedThreadCount );

		// inLenientMode since with interrupt final flush, refresh and merge segment are invoked
		backendMock.inLenientMode( () -> {
			// Interrupt the thread that triggered mass indexing
			massIndexingThread.interrupt();

			waitForMassIndexingThreadsToTerminate( expectedThreadCount );
		} );

		assertThat( interrupted ).isTrue();
		// Most JDK methods unset the interrupt flag when they throw an InterruptedException:
		// the MassIndexer should do the same.
		assertThat( interruptFlagAfterInterruption ).isFalse();
	}

	@Test
	public void cancel() {
		int expectedThreadCount = 1 // Coordinator
				+ 1 // Workspace
				+ 1 // ID loading
				+ 1; // Entity loading

		MassIndexer massIndexer = prepareMassIndexingThatWillNotTerminate();
		CompletableFuture<?> future = massIndexer.start().toCompletableFuture();

		waitForMassIndexingThreadsToSpawn( expectedThreadCount );

		// Cancel mass indexing
		future.cancel( true );

		waitForMassIndexingThreadsToTerminate( expectedThreadCount );
	}

	private MassIndexer prepareMassIndexingThatWillNotTerminate() {
		MassIndexer indexer = createSessionFromMap()
				.massIndexer()
				.typesToIndexInParallel( 1 )
				.threadsToLoadObjects( 1 );

		backendMock.expectIndexScaleWorks( Book.INDEX )
				.purge()
				.mergeSegments();

		backendMock.expectWorks(
				Book.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		)
				.add( "1", b -> b
				.field( "title", TITLE_1 )
				.field( "author", AUTHOR_1 )
				)
				// Return a CompletableFuture that will never complete
				.createdThenExecuted( new CompletableFuture<>() );

		return indexer;
	}

	private void waitForMassIndexingThreadsToSpawn(int expectedThreadCount) {
		// Wait until the work future is requested
		Awaitility.await().untilAsserted(
				() -> backendMock.verifyExpectationsMet()
		);

		// Check that we have exactly the expected number of threads
		List<Thread> massIndexingThreads = threadSpy.getCreatedThreads( "mass index" );
		assertThat( massIndexingThreads )
				.as( "Mass indexing threads" )
				.hasSize( expectedThreadCount );

		// Check that all mass indexing threads are still running
		assertThat( massIndexingThreads )
				// The ID Loading thread may have terminated already, ignore that one
				.filteredOn( t -> !t.getName().contains( "ID loading" ) )
				.hasSize( expectedThreadCount - 1 )
				.allSatisfy( t -> assertThat( t )
				.extracting( Thread::getState )
				.isIn(
						Thread.State.RUNNABLE, Thread.State.TIMED_WAITING,
						Thread.State.WAITING, Thread.State.BLOCKED
				)
				);
	}

	private void waitForMassIndexingThreadsToTerminate(int expectedThreadCount) {
		Awaitility.await().untilAsserted(
				() -> assertThat( threadSpy.getCreatedThreads( "mass index" ) )
						.as( "Mass indexing threads" )
						.hasSize( expectedThreadCount )
						.allSatisfy( t -> assertThat( t )
						.extracting( Thread::getState )
						.isEqualTo( Thread.State.TERMINATED )
						)
		);
	}

	private SearchSession createSessionFromMap() {
		return mapping.createSessionWithOptions().loading( (o) -> {
			o.registerLoader( Book.class, (identifiers) -> {
				return identifiers.stream()
						.map( (identifier) -> booksmap.get( (Integer) identifier ) )
						.collect( Collectors.toList() );
			} );

			o.massIndexingLoadingStrategy( Book.class, JavaBeanIndexingStrategies.from( booksmap ) );
		} ).build();
	}

	private void initData() {
		persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
	}

	private void persist(Book book) {
		booksmap.put( book.id, book );
	}

	@Indexed(index = Book.INDEX)
	public static class Book {

		public static final String INDEX = "Book";

		@DocumentId
		private Integer id;

		@GenericField
		private String title;

		@GenericField
		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}
}