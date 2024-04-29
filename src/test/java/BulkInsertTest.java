import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class BulkInsertTest {

    @Container
    @SuppressWarnings("resource")
    public Neo4jContainer<?> container = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.5.0"))
            .withoutAuthentication(); // Disable password

    private static final int ITEM_COUNT = 1_000;

    @Test
    void testReadFromNeo4jConnector() {
        insertNodes("items-1");
        insertNodes("items-2");
        long countedNodes = countNodes();
        assertEquals(2000, countedNodes);
    }

    private void insertNodes(String prefix) {
        String boltUrl = container.getBoltUrl();
        List<Map<String, Object>> nodes = new ArrayList<>();

        try (Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.none()); Session session = driver.session()) {
            for (int i = 0; i < ITEM_COUNT; i++) {
                Map<String, Object> node = new HashMap<>();
                node.put("name", prefix + "-name-" + i);
                node.put("value", prefix + "-value-" + i);
                node.put("timestamp", System.currentTimeMillis());
                nodes.add(node);
            }

            // Use the UNWIND clause to iterate over a parameter named $nodes. I
            String sb = "UNWIND $nodes AS node " +
                        // Create a node of type TestSource using the properties from the current element in the list.
                        "CREATE (:TestSource {name: node.name, value: node.value, timestamp: node.timestamp});";

            long start = System.currentTimeMillis();
            // Execute the query with the list of nodes
            session.run(sb, Collections.singletonMap("nodes", nodes));
            System.out.println("Time Taken to process and save 10000 records to Neo4j Batch Insert took " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    private long countNodes() {
        String boltUrl = container.getBoltUrl();
        try (Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.none()); Session session = driver.session()) {
            // Construct a query to count the number of nodes
            return session.run("MATCH (n:TestSource) RETURN count(n)").single().get("count(n)").asLong();
        }
    }
}
