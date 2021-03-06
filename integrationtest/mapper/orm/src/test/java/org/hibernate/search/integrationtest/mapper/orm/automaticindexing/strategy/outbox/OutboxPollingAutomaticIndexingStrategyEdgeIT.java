/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.strategy.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.AutomaticIndexingStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.awaitility.core.ThrowingRunnable;

/**
 * Extensive tests with edge cases for automatic indexing with {@link AutomaticIndexingStrategyNames#OUTBOX_POLLING}.
 */
public class OutboxPollingAutomaticIndexingStrategyEdgeIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.automaticIndexingStrategy( AutomaticIndexingStrategyExpectations.outboxPolling() );

	private final FilteringOutboxEventFinder outboxEventFinder = new FilteringOutboxEventFinder()
			// Disable the filter by default: only some of the tests actually need it.
			.enableFilter( false );

	private SessionFactory sessionFactory;
	private TestFailureHandler failureHandler;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b.field( "indexedField", String.class ) );
		failureHandler = new TestFailureHandler();
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.background_failure_handler", failureHandler )
				.withProperty( "hibernate.search.automatic_indexing.outbox_event_finder", outboxEventFinder )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void multipleChangesSameTransaction() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue I" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue I" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue I" );
			session.persist( entity3 );

			entity1.setIndexedField( "initialValue II" );
			session.update( entity1 );
			entity2.setIndexedField( "initialValue II" );
			session.update( entity2 );
			entity3.setIndexedField( "initialValue II" );
			session.update( entity3 );

			entity1.setIndexedField( "initialValue III" );
			session.update( entity1 );
			entity2.setIndexedField( "initialValue III" );
			session.update( entity2 );
			entity3.setIndexedField( "initialValue III" );
			session.update( entity3 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", "initialValue III" )
					)
					.add( "2", b -> b
							.field( "indexedField", "initialValue III" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue III" )
					);
		} );
		backendMock.verifyExpectationsMet();

		assertThat( failureHandler.genericFailures ).isEmpty();
		assertThat( failureHandler.entityFailures ).isEmpty();
	}

	@Test
	public void massiveInsert() {
		for ( int i = 0; i < 15; i++ ) {
			int finalI = i;
			OrmUtils.withinTransaction( sessionFactory, session -> {
				BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks( IndexedEntity.INDEX );

				for ( int j = 0; j < 500; j++ ) {
					int index = finalI * 500 + j;

					IndexedEntity entity = new IndexedEntity();
					entity.setId( index );
					entity.setIndexedField( "indexed value: " + index );
					session.persist( entity );

					if ( j % 25 == 0 ) {
						session.flush();
						session.clear();
					}

					context.add( index + "", b -> b.field( "indexedField", "indexed value: " + index ) );
				}
			} );
			backendMock.verifyExpectationsMet();
		}

		assertThat( failureHandler.genericFailures ).isEmpty();
		assertThat( failureHandler.entityFailures ).isEmpty();
	}

	@Test
	public void backendFailure() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue" );
			session.persist( entity3 );

			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work #2 failed!" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.createAndExecuteFollowingWorks()
					.add( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					.createAndExecuteFollowingWorks( failingFuture )
					.add( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (succeeds):
					.createAndExecuteFollowingWorks()
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

		} );
		backendMock.verifyExpectationsMet();

		assertThat( failureHandler.genericFailures ).isEmpty();

		List<EntityIndexingFailureContext> entityFailures = failureHandler.entityFailures.get( 2 );
		awaitFor( () -> assertThat( entityFailures ).hasSize( 1 ) );

		EntityIndexingFailureContext entityFailure = entityFailures.get( 0 );
		checkId2EntityEventFailure( entityFailure );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4230")
	public void backendFailure_failedDeleteThenAdd() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", "initialValue" ) );
		} );
		backendMock.verifyExpectationsMet();

		outboxEventFinder.enableFilter( true );

		// Delete the entity (but don't trigger indexing yet: events are being filtered)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.load( IndexedEntity.class, 1 );
			session.delete( entity1 );
		} );

		// Remember the events at this point
		List<Long> eventIdsUpToDelete = new ArrayList<>();
		withinTransaction( sessionFactory, session -> {
			eventIdsUpToDelete.addAll( outboxEventFinder.findOutboxEventIdsNoFilter( session ) );
		} );

		// Re-create the entity (but don't trigger indexing yet: events are being filtered)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "updatedValue" );
			session.persist( entity1 );
		} );

		// This is the point of this test:
		// simulate the processing of the delete (which fails) then the second add (which succeeds),
		// and also the processing of the delete event's retry (which succeeds).
		// The retry needs to be processed before the add, otherwise the entity will be missing from the index.

		// Delete (failure, schedules a retry)
		CompletableFuture<?> failingFuture = new CompletableFuture<>();
		failingFuture.completeExceptionally( new SimulatedFailure( "Delete work on #1 failed!" ) );
		backendMock.expectWorks( IndexedEntity.INDEX )
				.createAndExecuteFollowingWorks( failingFuture )
				.delete( "1" );
		outboxEventFinder.showOnlyEvents( eventIdsUpToDelete );
		backendMock.verifyExpectationsMet();

		List<EntityIndexingFailureContext> entityFailures = failureHandler.entityFailures.get( 1 );
		awaitFor( () -> assertThat( entityFailures ).hasSize( 1 ) );

		// Delete retry + add
		backendMock.expectWorks( IndexedEntity.INDEX )
				.addOrUpdate( "1", b -> b
						.field( "indexedField", "updatedValue" ) );
		outboxEventFinder.enableFilter( false );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void backendFailure_twoFailuresOfTheSameIndexingWork() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue" );
			session.persist( entity3 );

			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work #2 failed!" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					.createAndExecuteFollowingWorks( failingFuture )
					.add( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (fails too):
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

			// finally it works:
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

		} );
		backendMock.verifyExpectationsMet();

		assertThat( failureHandler.genericFailures ).isEmpty();

		List<EntityIndexingFailureContext> entityFailures = failureHandler.entityFailures.get( 2 );
		awaitFor( () -> assertThat( entityFailures ).hasSize( 2 ) );

		EntityIndexingFailureContext entityFailure = entityFailures.get( 0 );
		checkId2EntityEventFailure( entityFailure );

		entityFailure = entityFailures.get( 1 );
		checkId2EntityEventFailure( entityFailure );
	}

	@Test
	public void backendFailure_numberOfTrialsExhausted() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue" );
			session.persist( entity3 );

			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work #2 failed!" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					.createAndExecuteFollowingWorks( failingFuture )
					.add( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (fails too):
					.createAndExecuteFollowingWorks( failingFuture )
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (fails too):
					.createAndExecuteFollowingWorks( failingFuture )
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

			// no more retry
		} );
		backendMock.verifyExpectationsMet();

		assertThat( failureHandler.genericFailures ).isEmpty();

		List<EntityIndexingFailureContext> entityFailures = failureHandler.entityFailures.get( 2 );
		awaitFor( () -> assertThat( entityFailures ).hasSize( 4 ) );

		for ( int i = 0; i < 3; i++ ) {
			EntityIndexingFailureContext entityFailure = entityFailures.get( i );
			checkId2EntityEventFailure( entityFailure );
		}

		EntityIndexingFailureContext entityFailure = entityFailures.get( 3 );
		assertThat( entityFailure.failingOperation() ).isEqualTo( "Processing an outbox event." );
		assertThat( entityFailure.throwable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Max '3' retries exhausted to process the event. Event will be lost." );
		hasOneReference( entityFailure.entityReferences(), "indexed", 2 );
	}

	private void checkId2EntityEventFailure(EntityIndexingFailureContext entityFailure) {
		assertThat( entityFailure.failingOperation() ).isEqualTo( "Processing an outbox event." );
		assertThat( entityFailure.throwable() )
				.isInstanceOf( SimulatedFailure.class )
				.hasMessageContaining( "Indexing work #2 failed!" );
		hasOneReference( entityFailure.entityReferences(), "indexed", 2 );
	}

	@SuppressWarnings("unchecked")
	private void hasOneReference(List<Object> entityReferences, String entityName, Object id) {
		assertThat( entityReferences ).hasSize( 1 );
		EntityReference entityReference = (EntityReference) entityReferences.get( 0 );
		assertThat( entityReference.name() ).isEqualTo( entityName );
		assertThat( entityReference.id() ).isEqualTo( id );
	}

	private static void awaitFor(ThrowingRunnable assertion) {
		await().timeout( 2, TimeUnit.SECONDS ).untilAsserted( assertion );
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}

	private static class TestFailureHandler implements FailureHandler {
		// there are no concurrent write in this test,
		// using volatile list / concurrent hashmap only to make the changes on value lists visible by the main thread
		private volatile List<FailureContext> genericFailures = new ArrayList<>();
		private Map<Integer, List<EntityIndexingFailureContext>> entityFailures = new ConcurrentHashMap<>();

		@Override
		public void handle(FailureContext context) {
			genericFailures.add( context );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void handle(EntityIndexingFailureContext context) {
			for ( Object item : context.entityReferences() ) {
				EntityReference entityReference = (EntityReference) item;
				Integer id = (Integer) entityReference.id();

				entityFailures.computeIfAbsent( id, key -> new ArrayList<>() );
				entityFailures.get( id ).add( context );
			}
		}
	}

}
