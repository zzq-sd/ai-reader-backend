package com.aireader.backend.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Neo4j索引和约束配置
 */
@Configuration
public class Neo4jIndexAndConstraintConfig {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jIndexAndConstraintConfig.class);

    private final Driver driver;

    @Autowired
    public Neo4jIndexAndConstraintConfig(Driver driver) {
        this.driver = driver;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createConstraintsAndIndices() {
        logger.info("Ensuring Neo4j constraints and indices are created...");
        try (Session session = driver.session()) {
            // Constraint for ConceptNode name uniqueness
            String conceptNameConstraint = "CREATE CONSTRAINT concept_name_unique IF NOT EXISTS FOR (c:Concept) REQUIRE c.name IS UNIQUE";
            session.run(conceptNameConstraint);
            logger.info("Successfully ensured constraint 'concept_name_unique'.");
            
            // Constraint for TagNode name uniqueness (anticipating similar need)
            String tagNameConstraint = "CREATE CONSTRAINT tag_name_unique IF NOT EXISTS FOR (t:Tag) REQUIRE t.name IS UNIQUE";
            session.run(tagNameConstraint);
            logger.info("Successfully ensured constraint 'tag_name_unique'.");

            // Add other constraints or indices here if needed in the future
            // Example: CREATE INDEX article_title_index IF NOT EXISTS FOR (a:Article) ON (a.title)

        } catch (Exception e) {
            logger.error("Error creating Neo4j constraints/indices: {}", e.getMessage());
        }
    }
} 