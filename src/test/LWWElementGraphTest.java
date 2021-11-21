package test;

import main.LWWElementGraph;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class LWWElementGraphTest {

    @Test
    public void testAddVertex(){
        LWWElementGraph replica = new LWWElementGraph();

        // Test adding a vertex
        replica.addVertex("A", 10);
        assertTrue(replica.checkVertexExists("A"));
        Assert.assertEquals(10, replica.getVertexCreationTimestamp("A"));
        Assert.assertEquals(-1, replica.getVertexRemovalTimestamp("A"));

        // Invalid timestamp or label
        replica.addVertex("B", -5);
        assertFalse(replica.checkVertexExists("B"));
        assertEquals(-1, replica.getVertexCreationTimestamp("B"));
        replica.addVertex("", 0);
        assertFalse(replica.checkVertexExists(""));

        // Test adding a vertex with a PAST timestamp
        // Expect no changes
        replica.addVertex("A", 5);
        Assert.assertEquals(10, replica.getVertexCreationTimestamp("A"));

        // Test adding a vertex with a FUTURE timestamp
        // Expect updated creation timestamp
        replica.addVertex("A", 15);
        Assert.assertEquals(15, replica.getVertexCreationTimestamp("A"));

        // Test commutativity
        LWWElementGraph replicaOne = new LWWElementGraph();
        LWWElementGraph replicaTwo = new LWWElementGraph();

        replicaOne.addVertex("A", 1);
        replicaOne.addVertex("B", 2);

        replicaTwo.addVertex("B", 2);
        replicaTwo.addVertex("A", 1);
        assertEquals(replicaOne, replicaTwo);

        // Test associativity
        replicaOne = new LWWElementGraph();
        replicaTwo = new LWWElementGraph();
        replicaOne.addVertex("A", 1);
        replicaOne.addVertex("B", 5);
        replicaOne.addVertex("C", 10);

        replicaTwo.addVertex("B", 5);
        replicaTwo.addVertex("C", 10);
        replicaTwo.addVertex("A", 1);

        assertEquals(replicaOne, replicaTwo);
    }

    @Test
    public void testRemoveVertex(){
        LWWElementGraph replica = new LWWElementGraph();

        // Test removing a non-existent vertex
        // We expect it won't exist, but its removal timestamp should be updated
        replica.removeVertex("A", 5);
        assertFalse(replica.checkVertexExists("A"));
        assertEquals(-1, replica.getVertexCreationTimestamp("A"));
        assertEquals(5, replica.getVertexRemovalTimestamp("A"));

        // Test removing a vertex not connected to any other vertices
        // Removing a connected vertex will be tested in testEdgesAdjacentVertices
        replica.addVertex("A", 10);
        replica.addVertex("B", 11);
        replica.addVertex("C", 12);
        replica.addVertex("D", 13);

        replica.removeVertex("A", 13);
        assertEquals(13, replica.getVertexRemovalTimestamp("A"));
        assertFalse(replica.checkVertexExists("A"));

        // Removing the same vertex with a PAST timestamp should have no effect
        replica.removeVertex("A", 12);
        assertEquals(13, replica.getVertexRemovalTimestamp("A"));
        assertFalse(replica.checkVertexExists("A"));

        // Try to remove a vertex which didn't exist at the time of removal:
        // Expect this vertex should still exist, but its removal timestamp updated
        replica.removeVertex("B", 10);
        assertTrue(replica.checkVertexExists("B"));
        assertEquals(10, replica.getVertexRemovalTimestamp("B"));
        assertEquals(11, replica.getVertexCreationTimestamp("B"));

        // Remove a vertex at the same time as its creation - should be biased towards removal
        replica.removeVertex("B", 11);
        assertFalse(replica.checkVertexExists("B"));
        assertEquals(11, replica.getVertexRemovalTimestamp("B"));
        assertEquals(11, replica.getVertexCreationTimestamp("B"));

        // Try removing the same vertex - removal timestamp should be updated
        replica.removeVertex("B", 12);
        assertFalse(replica.checkVertexExists("B"));
        assertEquals(12, replica.getVertexRemovalTimestamp("B"));
        assertEquals(11, replica.getVertexCreationTimestamp("B"));

        // Recreate the vertex - should exist with updated creation timestamp
        replica.addVertex("B", 13);
        assertTrue(replica.checkVertexExists("B"));
        assertEquals(12, replica.getVertexRemovalTimestamp("B"));
        assertEquals(13, replica.getVertexCreationTimestamp("B"));

        // Remove the vertex B again, the timestamp should be the latest
        replica.removeVertex("B", 20);
        assertFalse(replica.checkVertexExists("B"));
        assertEquals(20, replica.getVertexRemovalTimestamp("B"));

        // Removing vertex again should update the timestamp (even though B doesn't exist)
        replica.removeVertex("B", 25);
        assertFalse(replica.checkVertexExists("B"));
        assertEquals(25, replica.getVertexRemovalTimestamp("B"));

        // Test commutativity
        LWWElementGraph replicaOne = new LWWElementGraph();
        LWWElementGraph replicaTwo = new LWWElementGraph();
        replicaOne.removeVertex("A", 10);
        replicaOne.removeVertex("B", 11);

        replicaTwo.removeVertex("B", 11);
        replicaTwo.removeVertex("A", 10);
        assertEquals(replicaOne, replicaTwo);

        // Test associativity
        replicaOne = new LWWElementGraph();
        replicaTwo = new LWWElementGraph();
        replicaOne.removeVertex("A", 1);
        replicaOne.removeVertex("B", 5);
        replicaOne.removeVertex("C", 10);

        replicaTwo.removeVertex("B", 5);
        replicaTwo.removeVertex("C", 10);
        replicaTwo.removeVertex("A", 1);
        assertEquals(replicaOne, replicaTwo);

        // Make sure that remove and add are order-independent
        replicaOne = new LWWElementGraph();
        replicaTwo = new LWWElementGraph();
        replicaOne.addVertex("A", 1);
        replicaOne.removeVertex("A", 2);

        replicaTwo.removeVertex("A", 2);
        replicaTwo.addVertex("A", 1);
        assertEquals(replicaOne, replicaTwo);
        assertFalse(replicaOne.checkVertexExists("A"));
        assertEquals(2, replicaOne.getVertexRemovalTimestamp("A"));
        assertEquals(1, replicaOne.getVertexCreationTimestamp("A"));
    }

    @Test
    public void testEdgesAdjacentVertices(){
        // Test simple addition of edges
        LWWElementGraph replica = new LWWElementGraph();
        replica.addVertex("A", 1);
        replica.addVertex("B", 2);
        replica.addEdge("A", "B", 3);

        assertTrue(replica.getAdjacentVertices("A").contains("B"));
        assertEquals(1, replica.getAdjacentVertices("A").size());
        assertEquals(3, replica.getEdgeCreationTimestamp("A", "B"));
        assertEquals(-1, replica.getEdgeRemovalTimestamp("A", "B"));

        // Test adding the same edge again with both past and future timestamps
        replica.addEdge("A", "B", 1);
        assertEquals(3, replica.getEdgeCreationTimestamp("A", "B"));

        replica.addEdge("A", "B", 5);
        assertEquals(5, replica.getEdgeCreationTimestamp("A", "B"));

        // Simple deletion of an edge
        replica.removeEdge("A", "B", 6);
        assertEquals(0, replica.getAdjacentVertices("A").size());
        assertEquals(6, replica.getEdgeRemovalTimestamp("A", "B"));
        assertEquals(5, replica.getEdgeCreationTimestamp("A", "B"));

        // Removing edge with both past and future timestamps
        replica.removeEdge("A", "B", 4);
        assertEquals(6, replica.getEdgeRemovalTimestamp("A", "B"));

        replica.removeEdge("A", "B", 10);
        assertEquals(10, replica.getEdgeRemovalTimestamp("A", "B"));

        // Remove a non-existent edge
        replica.removeEdge("A", "C", 15);
        assertEquals(-1, replica.getEdgeCreationTimestamp("A", "C"));
        assertEquals(15, replica.getEdgeRemovalTimestamp("A", "C"));

        // Test different orderings of add/remove edge (commutativity)
        LWWElementGraph replicaOne = new LWWElementGraph();
        LWWElementGraph replicaTwo = new LWWElementGraph();

        replicaOne.addVertex("A", 1);
        replicaOne.addVertex("B", 2);

        replicaTwo.addVertex("A", 1);
        replicaTwo.addVertex("B", 2);

        replicaOne.addEdge("A", "B", 15);
        replicaOne.removeEdge("A", "B", 10);

        replicaTwo.removeEdge("A", "B", 10);
        replicaTwo.addEdge("A", "B", 15);
        assertEquals(replicaOne, replicaTwo);
        assertEquals(10, replicaOne.getEdgeRemovalTimestamp("A", "B"));
        assertEquals(15, replicaOne.getEdgeCreationTimestamp("A", "B"));
        assertTrue(replicaOne.getAdjacentVertices("B").contains("A"));

        // Test bias towards removal
        // using previous replicaOne (which has an edge from A to B created at 15)
        replicaOne.removeEdge("A", "B", 15);
        assertEquals(0, replicaOne.getAdjacentVertices("B").size());

        // Test removing a vertex A at a time = 15 from replicaTwo
        // Edge from A to B should not exist, since the vertex A is removed
        assertEquals(1, replicaTwo.getAdjacentVertices("B").size());
        replicaTwo.removeVertex("A", 15);
        assertEquals(0, replicaTwo.getAdjacentVertices("B").size());

        // Suppose the edge was created before one of its vertices were created
        // We expect it should not be counted
        replica = new LWWElementGraph();
        replica.addVertex("A", 5);
        replica.addEdge("A", "B", 5);
        replica.addVertex("B", 6);
        assertEquals(0, replica.getAdjacentVertices("A").size());
        assertEquals(0, replica.getAdjacentVertices("B").size());

        // Adding edges with the same creation timestamp as vertices should be allowed
        replica = new LWWElementGraph();
        replica.addVertex("A", 5);
        replica.addEdge("A", "B", 5);
        replica.addVertex("B", 5);
        assertEquals(1, replica.getAdjacentVertices("A").size());
        assertEquals(1, replica.getAdjacentVertices("B").size());

        // Try getting adjacent vertices for a non-existent vertex
        assertEquals(0, replica.getAdjacentVertices("C").size());
    }

    @Test
    public void testOrderIndependence(){
        LWWElementGraph replicaOne = new LWWElementGraph();
        LWWElementGraph replicaTwo = new LWWElementGraph();

        replicaOne.addVertex("A", 1);
        replicaOne.addVertex("B", 2);
        replicaOne.addVertex("C", 3);
        replicaOne.addVertex("D", 4);
        replicaOne.addVertex("E", 5);
        replicaOne.addEdge("A", "B", 6);
        replicaOne.addEdge("C", "B", 7);
        replicaOne.addEdge("C", "D", 8);
        replicaOne.removeEdge("A", "B", 9);
        replicaOne.removeVertex("C", 10);

        replicaTwo.removeVertex("C", 10);
        replicaTwo.removeEdge("A", "B", 9);
        replicaTwo.addEdge("C", "D", 8);
        replicaTwo.addEdge("C", "B", 7);
        replicaTwo.addEdge("A", "B", 6);
        replicaTwo.addVertex("E", 5);
        replicaTwo.addVertex("D", 4);
        replicaTwo.addVertex("C", 3);
        replicaTwo.addVertex("B", 2);
        replicaTwo.addVertex("A", 1);

        assertEquals(replicaOne, replicaTwo);
    }

    @Test
    public void testPath(){
        LWWElementGraph replica = new LWWElementGraph();
        replica.addVertex("A", 1);
        replica.addVertex("B", 2);
        replica.addVertex("C", 3);
        replica.addVertex("D", 4);
        replica.addVertex("E", 5);
        replica.addVertex("F", 6);
        replica.addVertex("G", 7);
        replica.addVertex("H", 8);
        replica.addVertex("J", 9);
        replica.addEdge("A", "B", 10);
        replica.addEdge("B", "C", 11);
        replica.addEdge("C", "G", 12);
        replica.addEdge("D", "B", 13);
        replica.addEdge("E", "B", 14);
        replica.addEdge("E", "F", 15);
        replica.addEdge("H", "J", 16);

        // A - B - E - F path
        //        System.out.println(replica.findPath("A", "F"));
        assertTrue(replica.findPath("A", "F").contains("A"));
        assertTrue(replica.findPath("A", "F").contains("B"));
        assertTrue(replica.findPath("A", "F").contains("E"));
        assertTrue(replica.findPath("A", "F").contains("F"));
        assertEquals(4, replica.findPath("A", "F").size());

        // G - C - B - E - F
        //        System.out.println(replica.findPath("G", "F"));
        assertEquals(5, replica.findPath("G", "F").size());

        // No H-A path
        assertEquals(0, replica.findPath("H", "A").size());

        // A to itself path
        assertEquals(1, replica.findPath("A", "A").size());

        // Remove vertex B, there should be no path between A and F
        replica.removeVertex("B", 17);
        assertEquals(0, replica.findPath("F", "A").size());

        // Recreate vertex B, make sure there's NO path between A and F
        // There's no path because edges between A and B, and B and E are created
        // before vertex B was removed
        replica.addVertex("B", 18);
        assertEquals(0, replica.findPath("F", "A").size());

        // Recreate edges A-B and B-E, make sure that now the path exists
        replica.addEdge("A", "B", 19);
        replica.addEdge("E", "B", 19);
        assertEquals(4, replica.findPath("F", "A").size());

        // Invalid input
        assertEquals(0, replica.findPath("R", "A").size());
        assertEquals(0, replica.findPath("A", "  ").size());
    }

    @Test
    public void testMerge(){
        LWWElementGraph replicaOne = new LWWElementGraph();
        LWWElementGraph replicaTwo = new LWWElementGraph();

        /*
        The below omits duplicate edges (BA for AB, DA for AD etc.)
        Replica One structure: (vertex/edge) : (creation time) : (removal time)
        A : 5 : -1
        B : 6 : -1
        C : 1 : 10
        AB : 7 : 8
        AD : 9 : -1

        Replica Two structure:
        A : 4 : -1
        B : 5 : -1
        C : 9 : 15
        D : 10 : -1
        AB : 6 : 11
        AD : 12 : -1
        CD : 13 : -1

        Upon merge we expect the following structure:
        A : 5 : -1
        B : 6 : -1
        C : 9 : 15
        D : 10 : -1
        AB : 7 : 11
        AD : 12 : -1
        CD : 13 : -1

        In other words, if we add all operations performed on replicas one and two:
        A: add(A,5) + add (A, 4) = add (5, 1)
        B: add(B,6) + add(B, 5) = add(B,6)
        C: add(C,1) + remove(C,10) + add(C,9) + remove(C,15) = add(C,9)+remove(C,15)
        D: add(D,10) = add(D, 10)
        AB: add(AB,7) + remove(AB,8) + add(AB,6)+remove(AB,11) = add(AB,7)+remove(AB,11)
        AD: add(AD,9) + add(AD,12) = add(AD,12)
        CD: add(CD,13) = add(CD,13)
         */
        replicaOne.addVertex("A", 5);
        replicaOne.addVertex("B", 6);
        replicaOne.addVertex("C", 1);
        replicaOne.addEdge("A", "B", 7);
        replicaOne.addEdge("A", "D", 9);
        replicaOne.removeEdge("A", "B", 8);
        replicaOne.removeVertex("C", 10);

        replicaTwo.addVertex("A", 4);
        replicaTwo.addVertex("B", 5);
        replicaTwo.addVertex("C", 9);
        replicaTwo.addVertex("D", 10);
        replicaTwo.addEdge("A", "B", 6);
        replicaTwo.removeEdge("A", "B", 11);
        replicaTwo.addEdge("A", "D", 12);
        replicaTwo.addEdge("C", "D", 13);
        replicaTwo.removeVertex("C", 15);

        LWWElementGraph expected = new LWWElementGraph();
        expected.addVertex("A", 5);
        expected.addVertex("B", 6);
        expected.addVertex("C", 9);
        expected.removeVertex("C", 15);
        expected.addVertex("D", 10);
        expected.addEdge("A", "B", 7);
        expected.removeEdge("A", "B", 11);
        expected.addEdge("A", "D", 12);
        expected.addEdge("C", "D", 13);

        replicaOne.merge(replicaTwo);
        assertEquals(expected, replicaOne);

        // Test commutativity
        // Reset replicaOne first
        replicaOne = new LWWElementGraph();
        replicaOne.addVertex("A", 5);
        replicaOne.addVertex("B", 6);
        replicaOne.addVertex("C", 1);
        replicaOne.addEdge("A", "B", 7);
        replicaOne.addEdge("A", "D", 9);
        replicaOne.removeEdge("A", "B", 8);
        replicaOne.removeVertex("C", 10);

        replicaTwo.merge(replicaOne);
        assertEquals(expected, replicaTwo);

        // Test that merge(merge(f)) = merge(f) / idempotency
        replicaTwo.merge(replicaTwo);
        assertEquals(expected, replicaTwo);
        System.out.println(expected);
    }

    @Test
    public void testDirectedGraph(){
        LWWElementGraph directed = new LWWElementGraph(true);
        assertTrue(directed.isDirected());

        directed.addVertex("A");
        directed.addVertex("B");
        directed.addVertex("C");
        directed.addVertex("D");
        directed.addVertex("E");
        directed.addVertex("F");
        directed.addVertex("G");
        directed.addEdge("A", "B");

        // make sure A is connected to B, but not B to A
        assertTrue(directed.getAdjacentVertices("A").contains("B"));
        assertEquals(0, directed.getAdjacentVertices("B").size());

        directed.addEdge("D", "B");
        directed.addEdge("E", "B");
        directed.addEdge("B", "C");
        directed.addEdge("E", "F");
        directed.addEdge("C", "G");

        // E is connected to B and F
        assertEquals(2, directed.getAdjacentVertices("E").size());
        assertTrue(directed.getAdjacentVertices("E").contains("B"));
        assertTrue(directed.getAdjacentVertices("E").contains("F"));

        // A -> B -> C -> G
        assertEquals(4, directed.findPath("A", "G").size());

        // Assert there is no path from B to either A, D or E
        // B should only be connected to C
        assertEquals(0, directed.findPath("B", "A").size());
        assertEquals(0, directed.findPath("B", "D").size());
        assertEquals(0, directed.findPath("B", "E").size());
        assertEquals(1, directed.getAdjacentVertices("B").size());
        assertTrue(directed.getAdjacentVertices("B").contains("C"));

        // Remove edge from A to B, make sure there's no path from A to B and C
        directed.removeEdge("A", "B");
        assertEquals(0, directed.findPath("A", "B").size());
        assertEquals(0, directed.findPath("A", "C").size());

        // There is a path from E to G (E-B-C-G)
        assertEquals(4, directed.findPath("E", "G").size());
        // Remove vertex C, make sure this path doesn't exist
        directed.removeVertex("C");
        assertEquals(0, directed.findPath("E", "G").size());

        LWWElementGraph undirected = new LWWElementGraph();
        undirected.addVertex("G");

        // Should not be able to merge directed and undirected graphs
        undirected.merge(directed);
        assertFalse(undirected.checkVertexExists("A"));
    }
}