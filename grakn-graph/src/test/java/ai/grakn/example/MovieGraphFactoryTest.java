/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.example;

import ai.grakn.Grakn;
import ai.grakn.GraknGraph;
import ai.grakn.concept.Entity;
import ai.grakn.concept.EntityType;
import ai.grakn.concept.Resource;
import ai.grakn.concept.ResourceType;
import ai.grakn.concept.RuleType;
import ai.grakn.graph.internal.AbstractGraknGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.stream.Stream;

import static ai.grakn.util.ErrorMessage.CANNOT_LOAD_EXAMPLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MovieGraphFactoryTest {

    private static Graph graph;
    private static GraknGraph graknGraph;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws IOException{
        graknGraph = Grakn.factory(Grakn.IN_MEMORY, UUID.randomUUID().toString().replaceAll("-", "a")).getGraph();
        MovieGraphFactory.loadGraph(graknGraph);
        graph = ((AbstractGraknGraph) graknGraph).getTinkerPopGraph();
    }

    @Test
    public void failToLoad(){
        GraknGraph graknGraph = Grakn.factory(Grakn.IN_MEMORY, UUID.randomUUID().toString().replaceAll("-", "a")).getGraph();
        graknGraph.putRelationType("fake");

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(CANNOT_LOAD_EXAMPLE.getMessage());

        MovieGraphFactory.loadGraph(graknGraph);
    }

    @Test(expected=InvocationTargetException.class)
    public void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<MovieGraphFactory> c = MovieGraphFactory.class.getDeclaredConstructor();
        c.setAccessible(true);
        c.newInstance();
    }

    @Test
    public void testGraphExists() {
        assertNotNull(graph);
    }

    @Test
    public void testGraphHasConcepts() {
        assertTrue(graph.traversal().V().hasNext());
    }

    @Test
    public void testGraphHasEdges() {
        assertTrue(graph.traversal().E().hasNext());
    }

    @Test
    public void testGraphHasMovie() {
        EntityType movie = graknGraph.getEntityType("movie");
        assertTrue(movie.superType().equals(graknGraph.getEntityType("production")));
    }

    @Test
    public void testGraphHasTvShow() {
        EntityType tvShow = graknGraph.getEntityType("tv-show");
        assertTrue(tvShow.superType().equals(graknGraph.getEntityType("production")));
    }

    @Test
    public void testGodfatherHasResource() {
        ResourceType tmdbVoteCount = graknGraph.getResourceType("tmdb-vote-count");
        ResourceType<String> title = graknGraph.getResourceType("title");
        Entity godfather = title.getResource("Godfather").owner().asEntity();
        Stream<Resource<?>> resources = godfather.resources().stream();
        assertTrue(resources.anyMatch(r -> r.type().equals(tmdbVoteCount) && r.getValue().equals(1000L)));
    }

    @Test
    public void testRulesExists() {
        RuleType ruleType = graknGraph.getRuleType("a-rule-type");
        assertEquals(2, ruleType.instances().size());
    }
}