/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.junit.Assume.assumeTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterOptionsStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public abstract class AbstractHighlighterIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	protected static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );
	protected static final SimpleMappedIndex<IndexBinding> matchingIndex = SimpleMappedIndex.of( IndexBinding::new )
			.name( "matchingIndex" );
	protected static final SimpleMappedIndex<NotMatchingTypeIndexBinding> notMatchingTypeIndex = SimpleMappedIndex.of( NotMatchingTypeIndexBinding::new )
			.name( "notMatchingTypeIndex" );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index )
				.withIndex( matchingIndex )
				.withIndex( notMatchingTypeIndex ).setup();

		index.bulkIndexer()
				.add( "1", d -> d.addValue( "string", "some value" ) )
				.add( "2", d -> {
					d.addValue( "string", "some other value" );
					d.addValue( "anotherString", "The quick brown fox jumps right over the little lazy dog" );
				} )
				.add( "3", d -> {
					d.addValue( "string", "some another value" );
				} )
				.add( "4", d -> {
					d.addValue( "string", "some yet another value" );
				} )
				.add( "5", d -> {
					d.addValue( "string", "foo and foo and foo much more times" );
					d.addValue(
							"anotherString",
							"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin nec ipsum ultricies, blandit velit vitae, lacinia tellus. Fusce elementum ultricies felis, ut molestie orci lacinia a. In eget euismod nulla. Praesent euismod orci vitae sapien cursus aliquet. Aenean velit ex, consequat in magna eu, ornare facilisis tellus. Ut vel diam nec sem lobortis lacinia. Sed nisi ex, faucibus nec ante pulvinar, congue feugiat mauris. Curabitur efficitur arcu et neque condimentum, vel convallis elit ultricies. Suspendisse a odio augue. Aliquam lorem turpis, molestie at sollicitudin quis, convallis id dolor. Quisque ultricies libero at consequat ornare.\n" +
									"Praesent vel accumsan lectus. Fusce tristique pulvinar pulvinar. Sed ac leo sodales, dictum sapien non, feugiat urna. Quisque dignissim id massa ut dictum. Nam nec erat luctus, sodales lorem in, congue leo. Aliquam erat volutpat. Fusce dapibus consequat dui at lobortis. Suspendisse iaculis pellentesque lacus, eu tincidunt nisi ullamcorper molestie. Vivamus ullamcorper pulvinar commodo. Vivamus at justo in risus pretium malesuada."
					);
				} )
				.add( "6", d -> {
					d.addValue( "string", "This string mentions a dog" );
					d.addValue( "anotherString", "The quick brown fox jumps right over the little lazy dog" );
				} )
				.add( "7", d -> {
					d.addValue( "string", "This string mentions a dog too" );
				} )
				.add( "8", d -> {
					d.addValue( "string", "<body><h1>This is a Heading</h1><p>This is a paragraph</p></body>" );
				} )
				.add( "9", d -> {
					d.addObject( "nested" )
							.addValue( "nestedString", "The quick brown fox jumps right over the little lazy dog" );
					d.addValue( "notAnalyzedString", "The quick brown fox jumps right over the little lazy dog" );
					d.addValue( "multiValuedString", "The quick brown fox jumps right over the little lazy dog" );
					d.addValue( "multiValuedString", "This string mentions a dog" );
					d.addValue( "multiValuedString", "This string mentions a fox" );
				} )
				.add( "10", d -> {
					d.addValue( "string", "Scorpions are a German rock band formed in Hanover in 1965 by guitarist Rudolf Schenker. Since the band's inception, its musical style has ranged from hard rock, heavy metal and glam metal to soft rock." );
				} )
				.add( "11", d -> {
					d.addValue( "string", "text that has - dash in - it from time - to some useless text in between time to see - how - boundary_chars - works" );
				} )
				.add( "12", d -> {
					d.addValue( "stringNoTermVector", "boo and boo and boo much more times" );
				} )
				.join();

		matchingIndex.bulkIndexer()
				.add( "100", d -> d.addValue( "string", "string with dog" ) )
				.join();
	}

	abstract HighlighterOptionsStep<?> highlighter(SearchHighlighterFactory factory);

	@Test
	public void highlighterNoConfigurationAtAll() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <em>another</em> value" ),
						Arrays.asList( "some yet <em>another</em> value" )
				);
	}

	@Test
	public void highlighterNoSettings() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <em>another</em> value" ),
						Arrays.asList( "some yet <em>another</em> value" )
				);
	}

	@Test
	public void highlighterNoSettingsMultipleOccurrencesWithinSameLine() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em> and <em>foo</em> much more times" )
				);
	}

	@Test
	public void customTagGlobal() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h2 -> highlighter( h2 ).tag( "<strong>", "</strong>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	public void customTagField() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" ).highlighter( "strong-tag-highlighter" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( "strong-tag-highlighter", h2 -> highlighter( h2 ).tag( "<strong>", "</strong>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	public void customTagOverride() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" ).highlighter( "strong-tag-highlighter" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h -> highlighter( h ).tag( "<custom>", "</custom>" ) )
				.highlighter( "strong-tag-highlighter", h2 -> highlighter( h2 ).tag( "<strong>", "</strong>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	public void lastTagWins() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h )
						.tag( "*", "*" )
						.tag( "**", "**" )
						.tag( "***", "***" )
						.tag( "****", "****" )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "****foo**** and ****foo**** and ****foo**** much more times" )
				);
	}

	@Test
	public void encoderGlobalHtml() {
		encoderGlobal(
				HighlighterEncoder.HTML,
				"&lt;body&gt;&lt;h1&gt;This is a <em>Heading</em>&lt;&#x2F;h1&gt;&lt;p&gt;This is a paragraph&lt;&#x2F;p&gt;&lt;&#x2F;body&gt;"
		);
	}

	@Test
	public void encoderGlobalDefault() {
		encoderGlobal(
				HighlighterEncoder.DEFAULT,
				"<body><h1>This is a <em>Heading</em></h1><p>This is a paragraph</p></body>"
		);
	}

	protected void encoderGlobal(HighlighterEncoder encoder, String result) {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "Heading" ) )
				.highlighter( h -> highlighter( h ).encoder( encoder ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( result )
				);
	}

	@Test
	public void encoderFieldHtml() {
		encoderField(
				HighlighterEncoder.HTML,
				"&lt;body&gt;&lt;h1&gt;This is a <em>Heading</em>&lt;&#x2F;h1&gt;&lt;p&gt;This is a paragraph&lt;&#x2F;p&gt;&lt;&#x2F;body&gt;"
		);
	}

	@Test
	public void encoderFieldDefault() {
		encoderField(
				HighlighterEncoder.DEFAULT,
				"<body><h1>This is a <em>Heading</em></h1><p>This is a paragraph</p></body>"
		);
	}

	public void encoderField(HighlighterEncoder encoder, String result) {
		assumeTrue(
				"This test only make sense for backends that support encoder override at field level.",
				TckConfiguration.get().getBackendFeatures().supportsHighlighterEncoderAtFieldLevel()
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" ).highlighter( "encoder" )
				)
				.where( f -> f.match().field( "string" ).matching( "Heading" ) )
				.highlighter( "encoder", h -> highlighter( h ).encoder( encoder ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( result )
				);
	}

	@Test
	public void encoderOverrideHtml() {
		encoderOverride(
				HighlighterEncoder.DEFAULT,
				HighlighterEncoder.HTML,
				"&lt;body&gt;&lt;h1&gt;This is a <em>Heading</em>&lt;&#x2F;h1&gt;&lt;p&gt;This is a paragraph&lt;&#x2F;p&gt;&lt;&#x2F;body&gt;"
		);
	}

	@Test
	public void encoderOverrideDefault() {
		encoderOverride(
				HighlighterEncoder.HTML,
				HighlighterEncoder.DEFAULT,
				"<body><h1>This is a <em>Heading</em></h1><p>This is a paragraph</p></body>"
		);
	}

	public void encoderOverride(HighlighterEncoder globalEncoder, HighlighterEncoder encoder, String result) {
		assumeTrue(
				"This test only make sense for backends that support encoder override at field level.",
				TckConfiguration.get().getBackendFeatures().supportsHighlighterEncoderAtFieldLevel()
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" ).highlighter( "encoder" )
				)
				.where( f -> f.match().field( "string" ).matching( "Heading" ) )
				.highlighter( h -> highlighter( h ).encoder( globalEncoder ) )
				.highlighter( "encoder", h -> highlighter( h ).encoder( encoder ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( result )
				);
	}

	@Test
	public void fragmentSize() {
		assumeTrue( supportsFragmentSize() );
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "anotherString" )
				)
				.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
				.highlighter( h -> highlighter( h ).fragmentSize( 18 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						fragmentSizeResult()
				);
	}

	protected boolean supportsFragmentSize() {
		return true;
	}

	protected abstract List<String> fragmentSizeResult();

	@Test
	public void numberOfFragments() {
		assumeTrue(
				"We want to ignore this test since the highlighters that cannot return multiple fragments " +
						"cannot also limit the number of \"matched fragments\"",
				supportsMultipleFragmentsAsSeparateItems()
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "anotherString" )
				)
				.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
				.highlighter( h -> highlighter( h ).numberOfFragments( 1 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						numberOfFragmentsResult()
				);
	}

	protected List<String> numberOfFragmentsResult() {
		return Arrays.asList(
				"Lorem <em>ipsum</em> dolor sit amet, consectetur adipiscing elit."
		);
	}

	@Test
	public void defaultNoMatchSize() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "anotherString" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						// by default no match size  == 0, nothing is returned
						Collections.singletonList( Collections.emptyList() )
				);
	}

	@Test
	public void noMatchSize() {
		assumeTrue( supportsNoMatchSize() );
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "anotherString" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h ).noMatchSize( 11 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList( "Lorem ipsum" )
				);
	}

	protected boolean supportsNoMatchSize() {
		return true;
	}

	@Test
	public void compositeHighlight() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> highlights = scope.query().select(
						f -> f.composite().from(
								f.highlight( "string" ),
								f.highlight( "anotherString" )
						).asList()
				)
				.where( f -> f.bool()
						.should( f.match().field( "anotherString" ).matching( "fox" ) )
						.should( f.match().field( "string" ).matching( "dog" ) )
				)
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Collections.singletonList( "This string mentions a <em>dog</em> too" ),
								Collections.emptyList()
						),
						Arrays.asList(
								Collections.singletonList( "This string mentions a <em>dog</em>" ),
								Collections.singletonList(
										"The quick brown <em>fox</em> jumps right over the little lazy dog" )
						),
						Arrays.asList(
								Collections.emptyList(),
								Collections.singletonList(
										"The quick brown <em>fox</em> jumps right over the little lazy dog" )
						)
				);
	}

	@Test
	public void compositeHighlightMultipleConfigurations() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> highlights = scope.query().select(
						f -> f.composite().from(
								f.highlight( "string" ).highlighter( "for-string" ),
								f.highlight( "anotherString" ).highlighter( "for-another-string" )
						).asList()
				)
				.where( f -> f.bool()
						.should( f.match().field( "anotherString" ).matching( "fox" ) )
						.should( f.match().field( "string" ).matching( "dog" ) )
				)
				.highlighter( h -> highlighter( h ).tag( "*", "*" ) )
				.highlighter( "for-string", h -> highlighter( h ).tag( "**", "**" ) )
				.highlighter( "for-another-string", h -> highlighter( h ).tag( "***", "***" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Collections.singletonList( "This string mentions a **dog** too" ),
								Collections.emptyList()
						),
						Arrays.asList(
								Collections.singletonList( "This string mentions a **dog**" ),
								Collections.singletonList(
										"The quick brown ***fox*** jumps right over the little lazy dog" )
						),
						Arrays.asList(
								Collections.emptyList(),
								Collections.singletonList(
										"The quick brown ***fox*** jumps right over the little lazy dog" )
						)
				);
	}

	@Test
	public void multivaluedField() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "multiValuedString" )
				)
				.where( f -> f.match().field( "multiValuedString" ).matching( "dog" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder( Collections.singletonList( multivaluedFieldResult() ) );
	}

	@Test
	public void multivaluedFieldDuplicated() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> highlights = scope.query().select(
						f -> f.composite().from(
								f.highlight( "multiValuedString" ),
								f.highlight( "multiValuedString" )
						).asList()
				)
				.where( f -> f.match().field( "multiValuedString" ).matching( "dog" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList(
								Arrays.asList(
										multivaluedFieldResult(),
										multivaluedFieldResult()
								)
						)
				);
	}

	protected boolean supportsMultipleFragmentsAsSeparateItems() {
		return true;
	}

	protected List<String> multivaluedFieldResult() {
		return supportsMultipleFragmentsAsSeparateItems() ? Arrays.asList(
				"The quick brown fox jumps right over the little lazy <em>dog</em>",
				"This string mentions a <em>dog</em>"
		) : Collections.singletonList( "The quick brown fox jumps right over the little lazy <em>dog</em>This string mentions a <em>dog</em>" );
	}

	@Test
	public void nestedField() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "nested.nestedString" )
				)
				.where( f -> f.match().field( "nested.nestedString" ).matching( "fox" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList(
								Collections.singletonList(
										"The quick brown <em>fox</em> jumps right over the little lazy dog" ) )
				);
	}

	@Test
	public void orderByScore() {
		assumeTrue(
				"We ignore this test for the highlighters that do not support multi fragments as separate items since there's nothing to sort.",
				supportsMultipleFragmentsAsSeparateItems()
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "multiValuedString" )
				)
				.where( f -> f.bool()
						.must( f.match().field( "multiValuedString" ).matching( "dog" ) )
						.should( f.match().field( "multiValuedString" ).matching( "string" ).boost( 10.0f ) ) )
				.highlighter( h -> highlighter( h ).orderByScore( true ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder( orderByScoreResult() );
	}

	protected List<List<String>> orderByScoreResult() {
		return Arrays.asList(
				Arrays.asList(
						"This <em>string</em> mentions a <em>dog</em>",
						"This <em>string</em> mentions a fox",
						"The quick brown fox jumps right over the little lazy <em>dog</em>"
				)
		);
	}

	@Test
	public void unknownNamedHighlighter() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
								f -> f.highlight( "string" ).highlighter( "not-configured-highlighter" )
						).where( f -> f.matchAll() )
						.highlighter( "some-config", h -> highlighter( h ) )
						.highlighter( "some-other-config", h -> highlighter( h ) )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot find a highlighter with name 'not-configured-highlighter'.",
						"Available highlighters are:",
						"some-config", "some-other-config"
				);
	}

	@Test
	public void highlightNonAnalyzedField() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
								f -> f.highlight( "notAnalyzedString" )
						).where( f -> f.matchAll() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:highlight' on field 'notAnalyzedString':",
						//TODO: change message for this case:
						"Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant)."
				);
	}

	@Test
	public void highlighterNullName() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
								f -> f.highlight( "string" )
						).where( f -> f.matchAll() )
						.highlighter( null, h -> highlighter( h ) )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Named highlighters cannot use a blank string as name."
				);
	}

	@Test
	public void highlighterEmptyName() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
								f -> f.highlight( "string" )
						).where( f -> f.matchAll() )
						.highlighter( "", h -> highlighter( h ) )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Named highlighters cannot use a blank string as name."
				);
	}

	@Test
	public void highlighterSameName() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
								f -> f.highlight( "string" )
						).where( f -> f.matchAll() )
						.highlighter( "same-name", h -> highlighter( h ) )
						.highlighter( "same-name", h -> highlighter( h ) )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Highlighter with name 'same-name' is already defined. Use a different name to add another highlighter."
				);
	}

	@Test
	public void prebuiltHighlighter() {
		SearchHighlighter highlighter = highlighter( index.createScope().highlighter() ).tag( "---", "---" )
				.toHighlighter();

		SearchQuery<List<String>> highlights = index.createScope().query().select(
						f -> f.highlight( "string" )
				).where( f -> f.match().field( "string" ).matching( "dog" ) )
				.highlighter( highlighter )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Collections.singletonList( "This string mentions a ---dog---" ),
								Collections.singletonList( "This string mentions a ---dog--- too" )
						)
				);
	}

	@Test
	public void prebuiltNamedHighlighter() {
		SearchHighlighter highlighter = highlighter( index.createScope().highlighter() ).tag( "---", "---" )
				.toHighlighter();

		SearchQuery<List<String>> highlights = index.createScope().query().select(
						f -> f.highlight( "string" ).highlighter( "named-highlighter" )
				).where( f -> f.match().field( "string" ).matching( "dog" ) )
				.highlighter( "named-highlighter", highlighter )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Collections.singletonList( "This string mentions a ---dog---" ),
								Collections.singletonList( "This string mentions a ---dog--- too" )
						)
				);
	}

	@Test
	public void prebuiltHighlighterWrongScope() {
		SearchHighlighter highlighter = highlighter( notMatchingTypeIndex.createScope().highlighter() ).tag( "---", "---" )
				.toHighlighter();

		assertThatThrownBy( () -> index.createScope().query().select(
						f -> f.highlight( "string" )
				).where( f -> f.match().field( "string" ).matching( "dog" ) )
				.highlighter( highlighter )
				.toQuery() ).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid highlighter",
						"You must build the highlighter from a scope targeting indexes [indexName]",
						"but the given highlighter was built from a scope targeting indexes [notMatchingTypeIndex]."
				);
	}

	private static class IndexBinding {
		final IndexFieldReference<String> stringField;
		final IndexFieldReference<String> anotherStringField;
		final IndexFieldReference<String> nestedString;
		final IndexFieldReference<String> notAnalyzedString;
		final IndexFieldReference<String> multiValuedString;
		final IndexObjectFieldReference nested;
		final IndexFieldReference<String> stringNoTermVectorField;
		final IndexFieldReference<Integer> intField;

		IndexBinding(IndexSchemaElement root) {
			stringField = root.field( "string", f -> f.asString()
					.projectable( Projectable.YES )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS )
			).toReference();

			anotherStringField = root.field( "anotherString", f -> f.asString()
					.projectable( Projectable.YES )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS )
			).toReference();

			IndexSchemaObjectField objectField = root.objectField( "nested" );
			nested = objectField.toReference();

			nestedString = objectField.field( "nestedString", f -> f.asString()
					.projectable( Projectable.YES )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS )
			).toReference();

			notAnalyzedString = root.field( "notAnalyzedString", f -> f.asString() ).toReference();

			multiValuedString = root.field( "multiValuedString", f -> f.asString()
					.projectable( Projectable.YES )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS )
			).multiValued().toReference();

			stringNoTermVectorField = root.field( "stringNoTermVector", f -> f.asString()
					.projectable( Projectable.YES )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			).toReference();

			intField = root.field( "int", f -> f.asInteger() ).toReference();
		}
	}

	private static class NotMatchingTypeIndexBinding {
		final IndexFieldReference<Integer> stringField;
		final IndexObjectFieldReference nested;
		final IndexFieldReference<LocalDate> nestedString;

		NotMatchingTypeIndexBinding(IndexSchemaElement root) {
			stringField = root.field( "string", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField objectField = root.objectField( "nested" );
			nested = objectField.toReference();

			nestedString = objectField.field( "nestedString", f -> f.asLocalDate()
					.projectable( Projectable.YES )
			).toReference();
		}
	}
}