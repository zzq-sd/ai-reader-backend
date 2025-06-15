package com.aireader.backend.service.impl;

import com.aireader.backend.service.AiService;
import com.aireader.backend.dto.ai.ArticleAnalysisResult;
import com.aireader.backend.dto.ai.NoteAnalysisResult;
import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.model.constant.Neo4jRelationshipTypes;
import com.aireader.backend.dto.ConceptDetailDTO;
import com.aireader.backend.dto.ConceptStatisticsDTO;
import com.aireader.backend.dto.GraphDataDTO;
import com.aireader.backend.dto.GraphVisualizationDto;
import com.aireader.backend.dto.RelatedItemDto;
import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.Note;
import com.aireader.backend.model.mongo.ArticleContent;
import com.aireader.backend.model.neo4j.ArticleConceptRelationship;
import com.aireader.backend.model.neo4j.ArticleNode;
import com.aireader.backend.model.neo4j.ConceptNode;
import com.aireader.backend.model.neo4j.NoteConceptRelationship;
import com.aireader.backend.model.neo4j.NoteNode;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.mongo.ArticleContentRepository;
import com.aireader.backend.repository.NoteRepository;
import com.aireader.backend.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.neo4j.driver.types.Node;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识关联服务实现类
 */
@Service
@Slf4j
public class KnowledgeServiceImpl implements KnowledgeService {

    @Autowired
    private Neo4jTemplate neo4jTemplate;
    
    @Autowired
    private Neo4jClient neo4jClient;
    
    @Autowired
    private ArticleMetadataRepository articleMetadataRepository;
    
    @Autowired
    private ArticleContentRepository articleContentRepository;
    
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private AiService aiService;

    @Override
    @Transactional
    public boolean buildKnowledgeGraphFromArticle(String articleId) {
        log.info("开始从文章构建知识图谱，文章ID: {}", articleId);
        
        try {
            // 1. 获取文章元数据
            Optional<ArticleMetadata> articleMetadataOpt = articleMetadataRepository.findById(articleId);
            if (!articleMetadataOpt.isPresent()) {
                log.error("文章不存在，ID: {}", articleId);
                return false;
            }
            ArticleMetadata metadata = articleMetadataOpt.get();
            
            // 2. 获取文章内容
            Optional<ArticleContent> contentOpt = articleContentRepository.findByMysqlMetadataId(articleId);
            if (!contentOpt.isPresent() || contentOpt.get().getPlainTextContent() == null) {
                log.error("文章内容不存在或为空，文章ID: {}", articleId);
                return false;
            }
            
            // 3. 使用AI服务分析文章
            ArticleAnalysisResult analysisResult = aiService.analyzeArticle(
                articleId,
                metadata.getTitle(),
                contentOpt.get().getPlainTextContent()
            );
            
            // 4. 保存或更新文章节点
            ArticleNode articleNode = saveOrUpdateArticleNode(metadata);
            
            // 5. 保存概念节点并创建关系
            createConceptsAndRelationships(articleNode, analysisResult);
            
            log.info("文章知识图谱构建完成，文章ID: {}", articleId);
            return true;
        } catch (Exception e) {
            log.error("构建文章知识图谱失败，文章ID: " + articleId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean buildKnowledgeGraphFromNote(String noteId) {
        log.info("开始从笔记构建知识图谱，笔记ID: {}", noteId);
        
        try {
            // 1. 获取笔记
            Optional<Note> noteOpt = noteRepository.findById(noteId);
            if (!noteOpt.isPresent()) {
                log.error("笔记不存在，ID: {}", noteId);
                return false;
            }
            Note note = noteOpt.get();
            
            // 2. 使用AI服务分析笔记
            NoteAnalysisResult analysisResult = aiService.analyzeNote(
                noteId,
                note.getTitle(),
                note.getContent()
            );
            
            // 3. 保存或更新笔记节点
            NoteNode noteNode = saveOrUpdateNoteNode(note);
            
            // 4. 保存概念节点并创建关系
            createConceptsAndRelationshipsForNote(noteNode, analysisResult);
            
            log.info("笔记知识图谱构建完成，笔记ID: {}", noteId);
            return true;
        } catch (Exception e) {
            log.error("构建笔记知识图谱失败，笔记ID: " + noteId, e);
            return false;
        }
    }

    @Override
    public List<RelatedItemDto> getRelatedItemsForArticle(String articleId, int limit) {
        log.info("获取文章相关项，文章ID: {}，限制数量: {}", articleId, limit);
        
        try {
            // 执行Cypher查询，通过共享概念找到相关内容
            String cypher = 
                "MATCH (article:ArticleNode {id: $articleId})-[:MENTIONS_CONCEPT]->(concept:ConceptNode)" +
                "<-[r]-(related) " + 
                "WHERE type(r) IN ['MENTIONS_CONCEPT', 'CONTAINS_CONCEPT'] " +
                "AND NOT related:ArticleNode OR related.id <> $articleId " +
                "WITH related, count(concept) AS commonConcepts, collect(concept.name) AS conceptNames " +
                "ORDER BY commonConcepts DESC " +
                "LIMIT $limit " +
                "RETURN related, commonConcepts, conceptNames";
            
            Map<String, Object> params = Map.of(
                "articleId", articleId,
                "limit", limit
            );
            
            // 使用neo4jClient执行查询，将结果收集到ArrayList
            Collection<Map<String, Object>> resultCollection = neo4jClient.query(cypher)
                .bindAll(params)
                .fetch()
                .all();
                
            ArrayList<Map<String, Object>> results = new ArrayList<>(resultCollection);
            
            return transformToRelatedItemDto(results);
        } catch (Exception e) {
            log.error("获取文章相关项失败，文章ID: " + articleId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<RelatedItemDto> getRelatedItemsForNote(String noteId, int limit) {
        log.info("获取笔记相关项，笔记ID: {}，限制数量: {}", noteId, limit);
        
        try {
            // 执行Cypher查询，通过共享概念找到相关内容，支持多种标签形式
            String cypher = 
                "MATCH (note)-[:CONTAINS_CONCEPT]->(concept)" +
                "<-[r]-(related) " + 
                "WHERE (note:NoteNode OR note:Note) AND note.mysql_id = $noteId " +
                "AND (concept:ConceptNode OR concept:Concept) " +
                "AND type(r) IN ['MENTIONS_CONCEPT', 'CONTAINS_CONCEPT'] " +
                "AND ((NOT related:NoteNode AND NOT related:Note) OR related.mysql_id <> $noteId) " +
                "WITH related, count(concept) AS commonConcepts, collect(concept.name) AS conceptNames " +
                "ORDER BY commonConcepts DESC " +
                "LIMIT $limit " +
                "RETURN related, commonConcepts, conceptNames";
            
            Map<String, Object> params = Map.of(
                "noteId", noteId,
                "limit", limit
            );
            
            log.info("执行笔记相关查询: {}", cypher);
            
            // 使用neo4jClient执行查询，将结果收集到ArrayList
            Collection<Map<String, Object>> resultCollection = neo4jClient.query(cypher)
                .bindAll(params)
                .fetch()
                .all();
                
            ArrayList<Map<String, Object>> results = new ArrayList<>(resultCollection);
            log.info("笔记相关查询结果数: {}", results.size());
            
            return transformToRelatedItemDto(results);
        } catch (Exception e) {
            log.error("获取笔记相关项失败，笔记ID: " + noteId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<RelatedItemDto> getRelatedItemsForConcept(String conceptId, int limit) {
        log.info("获取概念相关项，概念ID: {}，限制数量: {}", conceptId, limit);
        
        try {
            // 执行Cypher查询，找到与概念相关的内容，支持多种标签形式
            String cypher = 
                "MATCH (concept)-[r]-(related) " +
                "WHERE (concept:ConceptNode OR concept:Concept) AND elementId(concept) = $conceptId " +
                "AND type(r) IN ['MENTIONS_CONCEPT', 'CONTAINS_CONCEPT'] " +
                "WITH related, type(r) AS relationType " +
                "ORDER BY relationType " +
                "LIMIT $limit " +
                "RETURN related, relationType";
            
            Map<String, Object> params = Map.of(
                "conceptId", conceptId,
                "limit", limit
            );
            
            log.info("执行概念相关查询: {}", cypher);
            
            Collection<Map<String, Object>> resultCollection = neo4jClient.query(cypher)
                .bindAll(params)
                .fetch()
                .all();
                
            List<Map<String, Object>> results = new ArrayList<>(resultCollection);
            log.info("概念相关查询结果数: {}", results.size());
            
            List<RelatedItemDto> relatedItems = new ArrayList<>();
            for (Map<String, Object> row : results) {
                Object related = row.get("related");
                String relationType = (String) row.get("relationType");
                
                if (related instanceof Map) {
                    Map<String, Object> node = (Map<String, Object>) related;
                    List<String> labels = (List<String>) node.get("labels");
                    
                    String itemType = "";
                    String id = "";
                    String title = "";
                    
                    // 判断节点类型，支持多种标签形式
                    if (labels.contains("ArticleNode") || labels.contains("Article")) {
                        itemType = "ARTICLE";
                        id = (String) node.get("id");
                        title = (String) node.get("title");
                    } else if (labels.contains("NoteNode") || labels.contains("Note")) {
                        itemType = "NOTE";
                        id = (String) node.get("mysql_id");
                        if (id == null) id = (String) node.get("id");
                        title = (String) node.get("title");
                    } else if (labels.contains("ConceptNode") || labels.contains("Concept")) {
                        itemType = "CONCEPT";
                        id = node.get("id") != null ? node.get("id").toString() : null;
                        title = (String) node.get("name");
                    }
                    
                    if (id != null && !id.isEmpty() && title != null && !title.isEmpty()) {
                        RelatedItemDto dto = RelatedItemDto.builder()
                            .itemId(id)
                            .title(title)
                            .type(RelatedItemDto.ItemType.valueOf(itemType))
                            .relevanceScore(1.0) // 简单计分
                            .reason(relationType.equals("MENTIONS_CONCEPT") ? 
                                    "提及相关概念" : "包含相关概念")
                            .build();
                        
                        relatedItems.add(dto);
                    }
                }
            }
            
            return relatedItems;
        } catch (Exception e) {
            log.error("获取概念相关项失败，概念ID: " + conceptId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public GraphVisualizationDto getGraphVisualizationData(String centerNodeId, String nodeType, int depth) {
        log.info("获取知识图谱可视化数据，中心节点ID: {}，节点类型: {}，深度: {}", centerNodeId, nodeType, depth);
        
        try {
            // 构建查询开始部分
            String startMatch = "";
            if ("article".equalsIgnoreCase(nodeType)) {
                startMatch = "MATCH (center:Article {id: $nodeId})";
            } else if ("note".equalsIgnoreCase(nodeType)) {
                startMatch = "MATCH (center:Note {id: $nodeId})";
            } else if ("concept".equalsIgnoreCase(nodeType)) {
                startMatch = "MATCH (center:Concept) WHERE elementId(center) = $nodeId";
            } else {
                throw new IllegalArgumentException("不支持的节点类型: " + nodeType);
            }
            
            // 节点查询
            String nodeQuery = startMatch +
                " CALL apoc.path.subgraphNodes(center, {maxLevel: $depth}) YIELD node" +
                " RETURN elementId(node) as id, labels(node) as labels, properties(node) as props";
            
            // 关系查询
            String relationshipQuery = startMatch +
                " CALL apoc.path.spanningTree(center, {maxLevel: $depth})" +
                " YIELD path" +
                " WITH relationships(path) as rels" +
                " UNWIND rels as rel" +
                " RETURN elementId(rel) as id, elementId(startNode(rel)) as source, elementId(endNode(rel)) as target," +
                " type(rel) as type, properties(rel) as props";
            
            Map<String, Object> params = Map.of(
                "nodeId", centerNodeId,
                "depth", depth
            );
            
            // 执行节点查询
            Collection<Map<String, Object>> nodeResultCollection = neo4jClient.query(nodeQuery)
                .bindAll(params)
                .fetch()
                .all();
                
            List<Map<String, Object>> nodeResults = new ArrayList<>(nodeResultCollection);
            
            // 执行关系查询
            Collection<Map<String, Object>> edgeResultCollection = neo4jClient.query(relationshipQuery)
                .bindAll(params)
                .fetch()
                .all();
                
            List<Map<String, Object>> edgeResults = new ArrayList<>(edgeResultCollection);
            
            // 构建可视化DTO
            List<GraphVisualizationDto.Node> nodes = nodeResults.stream()
                .map(row -> {
                    Long id = (Long) row.get("id");
                    List<String> labels = (List<String>) row.get("labels");
                    Map<String, Object> props = (Map<String, Object>) row.get("props");
                    
                    String nodeLabel = "";
                    String nodeType2 = labels.isEmpty() ? "Unknown" : labels.get(0);
                    
                    if (nodeType2.equals("Article")) {
                        nodeLabel = (String) props.getOrDefault("title", "未知文章");
                    } else if (nodeType2.equals("Note")) {
                        nodeLabel = (String) props.getOrDefault("title", "未知笔记");
                    } else if (nodeType2.equals("Concept")) {
                        nodeLabel = (String) props.getOrDefault("name", "未知概念");
                    } else {
                        nodeLabel = nodeType2;
                    }
                    
                    return GraphVisualizationDto.Node.builder()
                        .id(id.toString())
                        .label(nodeLabel)
                        .type(nodeType2)
                        .properties(props)
                        .build();
                })
                .collect(Collectors.toList());
            
            List<GraphVisualizationDto.Edge> edges = edgeResults.stream()
                .map(row -> GraphVisualizationDto.Edge.builder()
                    .id(row.get("id").toString())
                    .source(row.get("source").toString())
                    .target(row.get("target").toString())
                    .label((String) row.get("type"))
                    .properties((Map<String, Object>) row.get("props"))
                    .build()
                )
                .collect(Collectors.toList());
            
            return GraphVisualizationDto.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
        } catch (Exception e) {
            log.error("获取知识图谱可视化数据失败", e);
            return GraphVisualizationDto.builder()
                .nodes(Collections.emptyList())
                .edges(Collections.emptyList())
                .build();
        }
    }

    @Override
    public List<ConceptNode> searchConcepts(String conceptName, int limit) {
        log.info("搜索概念，名称: {}，限制数量: {}", conceptName, limit);
        
        try {
            // 使用neo4jClient执行查询
            String cypher = "MATCH (c:Concept) WHERE c.name CONTAINS $name RETURN c ORDER BY c.name LIMIT $limit";
            Map<String, Object> params = Map.of(
                "name", conceptName,
                "limit", limit
            );
            
            return neo4jClient.query(cypher)
                .bindAll(params)
                .fetchAs(ConceptNode.class)
                .all()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索概念失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public ConceptNode saveOrUpdateConcept(String name, String description) {
        log.info("保存或更新概念，名称: {}", name);
        
        try {
            // 使用neo4jClient查询现有概念
            String cypher = "MATCH (c:Concept) WHERE c.name = $name RETURN c";
            Map<String, Object> params = Map.of("name", name);
            
            List<ConceptNode> existingConcepts = neo4jClient.query(cypher)
                .bindAll(params)
                .fetchAs(ConceptNode.class)
                .all()
                .stream()
                .collect(Collectors.toList());
            
            if (!existingConcepts.isEmpty()) {
                ConceptNode existing = existingConcepts.get(0);
                if (description != null && !description.isBlank() && 
                    (existing.getDescription() == null || existing.getDescription().isBlank())) {
                    existing.setDescription(description);
                    return neo4jTemplate.save(existing);
                }
                return existing;
            } else {
                // 创建新概念
                ConceptNode newConcept = new ConceptNode();
                newConcept.setName(name);
                newConcept.setDescription(description);
                newConcept.setCreatedAt(LocalDateTime.now());
                return neo4jTemplate.save(newConcept);
            }
        } catch (Exception e) {
            log.error("保存或更新概念失败，名称: " + name, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public int createArticleConceptRelationships(String articleId, List<String> conceptNames) {
        log.info("为文章创建概念关系，文章ID: {}，概念数量: {}", articleId, conceptNames.size());
        
        try {
            // 1. 查找文章元数据
            Optional<ArticleMetadata> articleOpt = articleMetadataRepository.findById(articleId);
            if (!articleOpt.isPresent()) {
                log.error("文章不存在，ID: {}", articleId);
                return 0;
            }
            
            // 2. 获取或创建文章节点
            ArticleNode articleNode = saveOrUpdateArticleNode(articleOpt.get());
            
            // 3. 为每个概念名称创建概念节点（如果不存在）并建立关系
            int count = 0;
            for (String conceptName : conceptNames) {
                if (conceptName != null && !conceptName.trim().isEmpty()) {
                    // 保存或更新概念节点
                    ConceptNode conceptNode = saveOrUpdateConcept(conceptName.trim(), null);
                    
                    // 建立文章-概念关系
                    String cypher = 
                        "MATCH (a:Article {id: $articleId}) " +
                        "MATCH (c:Concept) WHERE elementId(c) = $conceptId " +
                        "MERGE (a)-[r:MENTIONS_CONCEPT]->(c) " +
                        "ON CREATE SET r.relevance = 0.8, r.manuallyAdded = true, r.createdAt = datetime() " +
                        "ON MATCH SET r.relevance = 0.8, r.manuallyAdded = true, r.updatedAt = datetime() " +
                        "RETURN r";
                    
                    Map<String, Object> params = Map.of(
                        "articleId", articleId,
                        "conceptId", conceptNode.getId().toString()
                    );
                    
                    neo4jClient.query(cypher)
                        .bindAll(params)
                        .run();
                    
                    count++;
                }
            }
            
            log.info("文章概念关系创建完成，文章ID: {}，创建数量: {}", articleId, count);
            return count;
        } catch (Exception e) {
            log.error("为文章创建概念关系失败: " + e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public int createNoteConceptRelationships(String noteId, List<String> conceptNames) {
        log.info("为笔记创建概念关系，笔记ID: {}，概念数量: {}", noteId, conceptNames.size());
        
        try {
            // 1. 查找笔记
            Optional<Note> noteOpt = noteRepository.findById(noteId);
            if (!noteOpt.isPresent()) {
                log.error("笔记不存在，ID: {}", noteId);
                return 0;
            }
            
            // 2. 获取或创建笔记节点
            NoteNode noteNode = saveOrUpdateNoteNode(noteOpt.get());
            
            // 3. 为每个概念名称创建概念节点（如果不存在）并建立关系
            int count = 0;
            for (String conceptName : conceptNames) {
                if (conceptName != null && !conceptName.trim().isEmpty()) {
                    // 保存或更新概念节点
                    ConceptNode conceptNode = saveOrUpdateConcept(conceptName.trim(), null);
                    
                    // 建立笔记-概念关系
                    String cypher = 
                        "MATCH (n:Note {mysql_id: $noteId}) " +
                        "MATCH (c:Concept) WHERE elementId(c) = $conceptId " +
                        "MERGE (n)-[r:CONTAINS_CONCEPT]->(c) " +
                        "ON CREATE SET r.relevance = 0.8, r.manuallyAdded = true, r.createdAt = datetime() " +
                        "ON MATCH SET r.relevance = 0.8, r.manuallyAdded = true, r.updatedAt = datetime() " +
                        "RETURN r";
                    
                    Map<String, Object> params = Map.of(
                        "noteId", noteId,
                        "conceptId", conceptNode.getId().toString()
                    );
                    
                    neo4jClient.query(cypher)
                        .bindAll(params)
                        .run();
                    
                    count++;
                }
            }
            
            log.info("笔记概念关系创建完成，笔记ID: {}，创建数量: {}", noteId, count);
            return count;
        } catch (Exception e) {
            log.error("为笔记创建概念关系失败: " + e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public Map<String, Object> getKnowledgeGraphStatistics() {
        log.info("获取知识图谱统计数据");
        
        try {
            // 查询不同节点和关系的计数
            Map<String, Object> statistics = new HashMap<>();
            
            String countQuery = 
                "MATCH (n) " +
                "WITH labels(n)[0] AS nodeType, count(n) AS count " +
                "RETURN nodeType, count " +
                "ORDER BY nodeType";
            
            Collection<Map<String, Object>> nodeCountCollection = neo4jClient.query(countQuery)
                .fetch().all();
                
            List<Map<String, Object>> nodeCount = new ArrayList<>(nodeCountCollection);
            
            // 处理节点计数
            Map<String, Integer> nodeStats = new HashMap<>();
            for (Map<String, Object> row : nodeCount) {
                String nodeType = (String) row.get("nodeType");
                Long count = (Long) row.get("count");
                nodeStats.put(nodeType, count.intValue());
            }
            statistics.put("nodes", nodeStats);
            
            // 查询关系统计
            String relCountQuery = 
                "MATCH ()-[r]->() " +
                "WITH type(r) AS relType, count(r) AS count " +
                "RETURN relType, count " +
                "ORDER BY relType";
            
            Collection<Map<String, Object>> relCountCollection = neo4jClient.query(relCountQuery)
                .fetch().all();
                
            List<Map<String, Object>> relCount = new ArrayList<>(relCountCollection);
            
            // 处理关系计数
            Map<String, Integer> relStats = new HashMap<>();
            for (Map<String, Object> row : relCount) {
                String relType = (String) row.get("relType");
                Long count = (Long) row.get("count");
                relStats.put(relType, count.intValue());
            }
            statistics.put("relationships", relStats);
            
            return statistics;
        } catch (Exception e) {
            log.error("获取知识图谱统计数据失败", e);
            return Map.of("error", "获取统计数据失败: " + e.getMessage());
        }
    }

    // 辅助方法：保存或更新文章节点
    private ArticleNode saveOrUpdateArticleNode(ArticleMetadata metadata) {
        // 查找现有节点
        String cypher = "MATCH (a:Article) WHERE a.id = $id RETURN a";
        Map<String, Object> params = Map.of("id", metadata.getId());
        
        List<ArticleNode> existingNodes = neo4jClient.query(cypher)
            .bindAll(params)
            .fetchAs(ArticleNode.class)
            .all()
            .stream()
            .collect(Collectors.toList());
        
        ArticleNode node;
        if (!existingNodes.isEmpty()) {
            node = existingNodes.get(0);
            // 更新属性
            node.setTitle(metadata.getTitle());
            node.setAuthor(metadata.getAuthor());
            node.setPublishDate(metadata.getPublicationDate());
            node.setUpdatedAt(LocalDateTime.now());
        } else {
            // 创建新节点
            node = new ArticleNode();
            node.setId(metadata.getId());
            node.setTitle(metadata.getTitle());
            node.setAuthor(metadata.getAuthor());
            node.setPublishDate(metadata.getPublicationDate());
            node.setCreatedAt(LocalDateTime.now());
            node.setUpdatedAt(LocalDateTime.now());
            node.setSourceId(metadata.getRssSourceId());
        }
        
        return neo4jTemplate.save(node);
    }
    
    // 辅助方法：保存或更新笔记节点
    private NoteNode saveOrUpdateNoteNode(Note note) {
        // 查找现有节点 - 使用mysql_id字段查询
        String cypher = "MATCH (n:Note) WHERE n.mysql_id = $mysqlId RETURN n";
        Map<String, Object> params = Map.of("mysqlId", note.getId());
        
        List<NoteNode> existingNodes = neo4jClient.query(cypher)
            .bindAll(params)
            .fetchAs(NoteNode.class)
            .all()
            .stream()
            .collect(Collectors.toList());
        
        NoteNode node;
        if (!existingNodes.isEmpty()) {
            node = existingNodes.get(0);
            // 更新属性
            node.setTitle(note.getTitle());
            node.setUpdatedAt(LocalDateTime.now());
            log.info("更新现有笔记节点，MySQL ID: {}, Neo4j ID: {}", note.getId(), node.getId());
        } else {
            // 创建新节点
            node = new NoteNode();
            node.setMysqlId(note.getId()); // 使用正确的方法设置MySQL ID
            node.setTitle(note.getTitle());
            node.setUserId(note.getUserId());
            node.setCreatedAt(LocalDateTime.now());
            node.setUpdatedAt(LocalDateTime.now());
            log.info("创建新笔记节点，MySQL ID: {}", note.getId());
        }
        
        return neo4jTemplate.save(node);
    }
    
    // 辅助方法：从AI分析结果创建概念和关系
    private void createConceptsAndRelationships(ArticleNode articleNode, ArticleAnalysisResult analysisResult) {
        // 处理关键词作为概念
        for (String keyword : analysisResult.getKeywords()) {
            ConceptNode concept = saveOrUpdateConcept(keyword, null);
            
            ArticleConceptRelationship relationship = new ArticleConceptRelationship();
            relationship.setArticle(articleNode);
            relationship.setConcept(concept);
            relationship.setRelationType("KEYWORD");
            relationship.setCreatedAt(LocalDateTime.now());
            
            neo4jTemplate.save(relationship);
        }
        
        // 处理实体
        for (Map<String, Object> entity : analysisResult.getEntities()) {
            String name = (String) entity.get("name");
            String type = (String) entity.get("type");
            
            if (name != null && !name.isBlank()) {
                ConceptNode concept = saveOrUpdateConcept(name, "实体类型: " + type);
                
                ArticleConceptRelationship relationship = new ArticleConceptRelationship();
                relationship.setArticle(articleNode);
                relationship.setConcept(concept);
                relationship.setRelationType("ENTITY");
                relationship.setEntityType(type);
                relationship.setCreatedAt(LocalDateTime.now());
                
                neo4jTemplate.save(relationship);
            }
        }
        
        // 处理概念
        for (ArticleAnalysisResult.ConceptEntity conceptEntity : analysisResult.getConcepts()) {
            String name = conceptEntity.getName();
            String description = conceptEntity.getContext();
            
            if (name != null && !name.isBlank()) {
                ConceptNode concept = saveOrUpdateConcept(name, description);
                
                ArticleConceptRelationship relationship = new ArticleConceptRelationship();
                relationship.setArticle(articleNode);
                relationship.setConcept(concept);
                relationship.setRelationType("CONCEPT");
                relationship.setRelevance(conceptEntity.getConfidence());
                relationship.setOccurrences(conceptEntity.getFrequency());
                relationship.setCreatedAt(LocalDateTime.now());
                
                neo4jTemplate.save(relationship);
            }
        }
        
        // 添加类别作为概念
        if (analysisResult.getCategory() != null && !analysisResult.getCategory().isBlank()) {
            ConceptNode concept = saveOrUpdateConcept(analysisResult.getCategory(), "文章类别");
            
            ArticleConceptRelationship relationship = new ArticleConceptRelationship();
            relationship.setArticle(articleNode);
            relationship.setConcept(concept);
            relationship.setRelationType("CATEGORY");
            relationship.setCreatedAt(LocalDateTime.now());
            
            neo4jTemplate.save(relationship);
        }
    }
    
    // 辅助方法：从AI分析结果创建笔记的概念和关系
    private void createConceptsAndRelationshipsForNote(NoteNode noteNode, NoteAnalysisResult analysisResult) {
        // 处理关键词作为概念
        for (String keyword : analysisResult.getKeywords()) {
            ConceptNode concept = saveOrUpdateConcept(keyword, null);
            noteNode.addConcept(concept, 0.7, Neo4jRelationshipTypes.KEYWORD);
        }
        
        // 处理实体
        for (Map<String, Object> entity : analysisResult.getEntities()) {
            String name = (String) entity.get("name");
            String type = (String) entity.get("type");
            
            if (name != null && !name.isBlank()) {
                ConceptNode concept = saveOrUpdateConcept(name, "实体类型: " + type);
                noteNode.addConcept(concept, 0.8, Neo4jRelationshipTypes.ENTITY);
            }
        }
        
        // 处理笔记中的主题
        if (analysisResult.getTopic() != null && !analysisResult.getTopic().isBlank()) {
            ConceptNode concept = saveOrUpdateConcept(analysisResult.getTopic(), "笔记主题");
            noteNode.addConcept(concept, 0.9, Neo4jRelationshipTypes.TOPIC);
        }
        
        // 处理关键观点
        for (String keyPoint : analysisResult.getKeyPoints()) {
            if (keyPoint != null && !keyPoint.isBlank()) {
                // 关键观点可能较长，作为概念处理时可能需要截断
                String conceptName = keyPoint.length() > 100 ? keyPoint.substring(0, 100) + "..." : keyPoint;
                ConceptNode concept = saveOrUpdateConcept(conceptName, "笔记观点: " + keyPoint);
                noteNode.addConcept(concept, 0.6, Neo4jRelationshipTypes.KEY_POINT);
            }
        }
        
        // 处理提取的概念
        for (ArticleAnalysisResult.ConceptEntity conceptEntity : analysisResult.getExtractedConcepts()) {
            if (conceptEntity.getName() != null && !conceptEntity.getName().isBlank()) {
                ConceptNode concept = saveOrUpdateConcept(conceptEntity.getName(), conceptEntity.getContext());
                noteNode.addConcept(concept, conceptEntity.getConfidence(), Neo4jRelationshipTypes.EXTRACTED_CONCEPT);
            }
        }
        
        // 一次性保存更新后的笔记节点（会自动保存所有关系）
        neo4jTemplate.save(noteNode);
        
        log.info("笔记知识图谱关系创建完成，笔记ID: {}", noteNode.getMysqlId());
    }
    
    // 辅助方法：转换查询结果为RelatedItemDto
    private List<RelatedItemDto> transformToRelatedItemDto(Collection<Map<String, Object>> results) {
        List<RelatedItemDto> dtos = new ArrayList<>();
        
        for (Map<String, Object> row : results) {
            Object related = row.get("related");
            Integer commonConcepts = ((Number) row.get("commonConcepts")).intValue();
            List<String> conceptNames = (List<String>) row.get("conceptNames");
            
            if (related instanceof Map) {
                Map<String, Object> node = (Map<String, Object>) related;
                List<String> labels = (List<String>) node.get("labels");
                
                String itemType = "";
                String id = "";
                String title = "";
                String summary = "";
                
                if (labels.contains("Article")) {
                    itemType = "ARTICLE";
                    id = (String) node.get("id");
                    title = (String) node.get("title");
                    
                    // 尝试获取摘要，使用String类型的ID
                    Optional<ArticleMetadata> metadata = articleMetadataRepository.findById(id);
                    if (metadata.isPresent()) {
                        summary = metadata.get().getSummary();
                    }
                } else if (labels.contains("Note")) {
                    itemType = "NOTE";
                    id = (String) node.get("id");
                    title = (String) node.get("title");
                    
                    // 尝试获取摘要，使用String类型的ID
                    Optional<Note> note = noteRepository.findById(id);
                    if (note.isPresent()) {
                        String content = note.get().getContent();
                        summary = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                    }
                } else if (labels.contains("Concept")) {
                    itemType = "CONCEPT";
                    id = node.get("id").toString();
                    title = (String) node.get("name");
                    summary = (String) node.getOrDefault("description", "");
                }
                
                if (!id.isEmpty() && !title.isEmpty()) {
                    RelatedItemDto dto = RelatedItemDto.builder()
                        .itemId(id)
                        .title(title)
                        .type(RelatedItemDto.ItemType.valueOf(itemType))
                        .summary(summary)
                        .relevanceScore((double) commonConcepts)
                        .reason("共享 " + commonConcepts + " 个概念：" + 
                                String.join(", ", conceptNames.subList(0, Math.min(3, conceptNames.size()))))
                        .build();
                    
                    dtos.add(dto);
                }
            }
        }
        
        return dtos;
    }

    @Override
    // @Cacheable(value = "graph-data", key = "#nodeType + ':' + #search + ':' + #limit", 
    //            cacheManager = "knowledgeGraphCacheManager")  // 暂时禁用缓存进行测试
    public GraphDataDTO getGraphData(String nodeType, String search, int limit) {
        log.info("【知识图谱服务】执行 getGraphData: nodeType={}, search={}, limit={}", nodeType, search, limit);

        String nodeFilter;
        switch (nodeType.toUpperCase()) {
            case "ARTICLE":
                nodeFilter = "n:ArticleNode";
                break;
            case "CONCEPT":
                nodeFilter = "n:ConceptNode";
                break;
            case "NOTE":
                nodeFilter = "n:NoteNode";
                break;
            default:
                nodeFilter = "n"; // ALL
                break;
        }

        String searchFilter = "";
        if (search != null && !search.trim().isEmpty()) {
            searchFilter = "WHERE n.name CONTAINS $search OR n.title CONTAINS $search OR n.id CONTAINS $search";
        }

        String cypherQuery = 
            "MATCH (" + nodeFilter + ") " +
            searchFilter + " " +
            "WITH n LIMIT $limit " +
            "MATCH (n)-[r]-(m) " +
            "WITH n, r, m " +
            "RETURN collect(DISTINCT n) + collect(DISTINCT m) AS nodes, collect(DISTINCT r) AS relationships";

        log.info("【知识图谱服务】执行现代化Cypher查询: {}", cypherQuery);

        Map<String, Object> params = new HashMap<>();
        params.put("limit", limit);
        if (!searchFilter.isEmpty()) {
            params.put("search", search);
        }

        try {
            Collection<Map<String, Object>> queryResults = neo4jClient.query(cypherQuery)
                .bindAll(params)
                .fetch()
                .all();

            if (queryResults.isEmpty() || queryResults.iterator().next().get("nodes") == null) {
                log.warn("【知识图谱服务】查询未返回任何数据或返回空节点集。");
                return new GraphDataDTO(Collections.emptyList(), Collections.emptyList());
            }
            
            // 结果现在应该只有一行，包含nodes和relationships
            Map<String, Object> result = queryResults.iterator().next();
            
            return convertNeo4jResultToGraphDataDTO(result);

        } catch (Exception e) {
            log.error("【知识图谱服务】执行Cypher查询时出错: {}", e.getMessage(), e);
            throw new RuntimeException("执行知识图谱查询失败", e);
        }
    }

    private GraphDataDTO convertNeo4jResultToGraphDataDTO(Map<String, Object> neo4jResult) {
        log.info("【知识图谱服务】开始将Neo4j查询结果转换为GraphDataDTO...");

        List<Node> nodeObjects = (List<Node>) neo4jResult.getOrDefault("nodes", Collections.emptyList());
        List<org.neo4j.driver.types.Relationship> relationshipObjects = 
            (List<org.neo4j.driver.types.Relationship>) neo4jResult.getOrDefault("relationships", Collections.emptyList());

        log.info("【知识图谱服务】从查询结果中提取到 {} 个节点和 {} 个关系。", nodeObjects.size(), relationshipObjects.size());

        Set<String> nodeIds = new HashSet<>();
        List<GraphDataDTO.NodeDTO> finalNodes = new ArrayList<>();

        for (Node node : nodeObjects) {
            String nodeId = String.valueOf(node.id());
            if (!nodeIds.contains(nodeId)) {
                GraphDataDTO.NodeDTO nodeDTO = convertNeo4jNodeToDTO(node);
                finalNodes.add(nodeDTO);
                nodeIds.add(nodeId);
            }
        }
        
        List<GraphDataDTO.EdgeDTO> finalEdges = relationshipObjects.stream()
            .map(this::convertNeo4jRelationshipToDTO)
            .collect(Collectors.toList());

        log.info("【知识图谱服务】DTO转换完成。最终节点数: {}, 边数: {}", finalNodes.size(), finalEdges.size());
        
        return new GraphDataDTO(finalNodes, finalEdges);
    }
    
    private GraphDataDTO.NodeDTO convertNeo4jNodeToDTO(Node node) {
        String primaryLabel = node.labels().iterator().next(); // 假设每个节点至少有一个标签作为其类型
        
        String name;
        if (node.containsKey("name")) {
            name = node.get("name").asString();
        } else if (node.containsKey("title")) {
            name = node.get("title").asString();
        } else {
            name = "Unnamed";
        }
        
        String id = String.valueOf(node.id());

        return GraphDataDTO.NodeDTO.builder()
            .id(id)
            .label(name)
            .type(primaryLabel)
            .color(getNodeColor(primaryLabel))
            .size(10.0) // 默认大小
            .properties(node.asMap())
            .build();
    }

    private GraphDataDTO.EdgeDTO convertNeo4jRelationshipToDTO(org.neo4j.driver.types.Relationship rel) {
        double weight = 1.0;
        if (rel.containsKey("weight")) {
            weight = rel.get("weight").asDouble();
        }

        return GraphDataDTO.EdgeDTO.builder()
            .id(String.valueOf(rel.id()))
            .source(String.valueOf(rel.startNodeId()))
            .target(String.valueOf(rel.endNodeId()))
            .type(rel.type())
            .label(rel.type())
            .weight(weight)
            .color("#999")
            .properties(rel.asMap())
            .build();
    }

    /**
     * @deprecated 旧的数据转换逻辑，由 convertNeo4jResultToGraphDataDTO 替代
     */
    private GraphDataDTO convertQueryResultToGraphDataDTO(List<Map<String, Object>> queryResults) {
        // ... 此处保留旧方法的完整实现，但标记为废弃 ...
        log.info("【知识图谱】开始将查询结果转换为GraphDataDTO，结果数量: {}", queryResults.size());
        // log.info("【知识图谱】开始将查询结果转换为GraphDataDTO，结果数量: {}", queryResults.size()); // 已有此行
        if (queryResults.isEmpty()) {
            log.warn("【知识图谱】传入convertQueryResultToGraphDataDTO的queryResults为空！");
        }
        List<GraphDataDTO.NodeDTO> nodes = new ArrayList<>();
        List<GraphDataDTO.EdgeDTO> edges = new ArrayList<>();
        
        // 为了防止重复添加相同的节点和边
        Set<String> processedNodeIds = new HashSet<>();
        Set<String> processedEdgeIds = new HashSet<>();
        
        int articleCount = 0, noteCount = 0, conceptCount = 0;
        int errorCount = 0;
        
        for (int i = 0; i < queryResults.size(); i++) {
            try {
                Map<String, Object> row = queryResults.get(i);
                // Map<String, Object> row = queryResults.get(i); // 已有此行
                log.info("【知识图谱】[转换器] 正在处理行 #{}: {}", i, row); // 记录正在处理的行
                if (row == null) {
                    log.warn("【知识图谱】结果 #{} 为空，跳过", i);
                    continue;
                }
                
                // 输出行的结构，帮助调试
                if (i == 0) {
                    log.info("【知识图谱】结果行结构：keys = {}", row.keySet());
                }
                
                // 提取节点数据 - 支持Neo4j Driver返回的Node对象
                Object nodeObj = row.get("node");
                if (nodeObj == null) {
                    // 尝试旧的字段名作为备选
                    nodeObj = row.get("n");
                    if (nodeObj == null) {
                        log.warn("【知识图谱】[转换器] 结果 #{} 中缺少节点数据 ('node' 或 'n')，跳过该行", i);
                        continue;
                    }
                }

                Map<String, Object> nodeDataProperties; // 用于存储节点属性
                String neo4jElementId = null; // 用于存储Neo4j内部ID

                // 优先使用查询返回的nodeProperties字段
                Object nodePropertiesObj = row.get("nodeProperties");
                if (nodePropertiesObj instanceof Map) {
                    nodeDataProperties = new HashMap<>((Map<String, Object>) nodePropertiesObj);
                    log.info("【知识图谱】[转换器] 行 #{}：✅使用查询返回的nodeProperties，属性: {}", 
                             i, nodeDataProperties.keySet());
                } else if (nodeObj instanceof org.neo4j.driver.types.Node) {
                    // 处理Neo4j Driver返回的Node对象
                    org.neo4j.driver.types.Node neo4jNode = (org.neo4j.driver.types.Node) nodeObj;
                    nodeDataProperties = new HashMap<>(neo4jNode.asMap()); // 将节点属性转换为Map
                    neo4jElementId = neo4jNode.elementId(); // 获取Neo4j 5+ 的元素ID
                    
                    log.info("【知识图谱】[转换器] 行 #{}：✅成功处理org.neo4j.driver.types.Node，elementId: {}, 属性: {}", 
                             i, neo4jElementId, nodeDataProperties.keySet());
                    
                } else if (nodeObj instanceof Map) {
                    // 如果已经是Map（可能来自其他查询或旧逻辑），直接使用
                    nodeDataProperties = (Map<String, Object>) nodeObj;
                    
                    // 尝试从Map中获取ID，优先使用elementId
                    Object idFromMap = nodeDataProperties.get("elementId");
                    if (idFromMap == null) idFromMap = nodeDataProperties.get("id"); // 兼容旧的id()
                    if (idFromMap != null) {
                        neo4jElementId = idFromMap.toString();
                    }
                    
                    log.info("【知识图谱】[转换器] 行 #{}：✅处理Map作为节点数据，属性: {}", i, nodeDataProperties.keySet());
                    
                } else {
                    log.error("【知识图谱】[转换器] ❌结果 #{} 中节点数据类型无法处理！", i);
                    log.error("【知识图谱】[转换器]   期望类型: org.neo4j.driver.types.Node 或 Map");
                    log.error("【知识图谱】[转换器]   实际类型: {}", nodeObj.getClass().getName());
                    log.error("【知识图谱】[转换器]   节点对象: {}", nodeObj);
                    continue;
                }

                // 使用处理后的属性Map
                Map<String, Object> nodeData = nodeDataProperties;
                
                // 提取节点标签
                List<String> labels = new ArrayList<>();
                Object labelsObj = row.get("nodeLabels");
                
                if (labelsObj != null && labelsObj instanceof List) {
                    labels = new ArrayList<>((List<String>) labelsObj);
                    log.info("【知识图谱】[转换器] 行 #{}：从查询获取标签: {}", i, labels);
                } else {
                    log.warn("【知识图谱】[转换器] 行 #{} nodeLabels不可用，类型: {}", 
                           i, labelsObj != null ? labelsObj.getClass().getName() : "null");
                }
                
                // 如果标签列表为空，使用强化的推断逻辑
                if (labels.isEmpty()) {
                    log.info("【知识图谱】[转换器] 行 #{} 标签为空，开始推断节点类型", i);
                    
                    // 强化的标签推断逻辑
                    if (nodeDataProperties.containsKey("name") && !nodeDataProperties.containsKey("title")) {
                        labels.add("ConceptNode");
                        log.info("【知识图谱】[转换器] 行 #{} 推断为ConceptNode（有name无title）", i);
                    } else if (nodeDataProperties.containsKey("title")) {
                        if (nodeDataProperties.containsKey("author") || nodeDataProperties.containsKey("publishDate")) {
                            labels.add("ArticleNode");
                            log.info("【知识图谱】[转换器] 行 #{} 推断为ArticleNode（有title和author/publishDate）", i);
                        } else if (nodeDataProperties.containsKey("mysql_id") || nodeDataProperties.containsKey("userId")) {
                            labels.add("NoteNode");
                            log.info("【知识图谱】[转换器] 行 #{} 推断为NoteNode（有title和mysql_id/userId）", i);
                        } else {
                            labels.add("NoteNode");  // 默认为笔记
                            log.info("【知识图谱】[转换器] 行 #{} 推断为NoteNode（仅有title）", i);
                        }
                    } else if (nodeDataProperties.containsKey("mysql_id")) {
                        labels.add("NoteNode");
                        log.info("【知识图谱】[转换器] 行 #{} 推断为NoteNode（有mysql_id）", i);
                    } else {
                        // 默认为概念节点
                        labels.add("ConceptNode");
                        log.info("【知识图谱】[转换器] 行 #{} 默认推断为ConceptNode", i);
                    }
                }
                
                log.info("【知识图谱】[转换器] 行 #{} 最终标签: {}", i, labels);
                
                log.debug("【知识图谱】处理节点 #{}, 标签: {}", i, labels);
                
                // 确定节点类型
                String nodeType = determineNodeType(labels);
                
                // 如果节点类型仍然未知，基于属性特征尝试确定
                if ("UNKNOWN".equals(nodeType)) {
                    log.warn("【知识图谱】结果 #{} 节点类型未知，尝试基于属性确定", i);
                    if (nodeData.containsKey("name") && !nodeData.containsKey("title")) {
                        nodeType = "CONCEPT";
                    } else if (nodeData.containsKey("title") && nodeData.containsKey("author")) {
                        nodeType = "ARTICLE";
                    } else if (nodeData.containsKey("title") || nodeData.containsKey("mysql_id")) {
                        nodeType = "NOTE";
                    }
                }
                
                if ("UNKNOWN".equals(nodeType)) {
                    log.warn("【知识图谱】结果 #{} 无法确定节点类型，使用默认类型CONCEPT", i);
                    nodeType = "CONCEPT"; // 默认处理为概念节点
                }
                
                // 生成节点ID - 优先使用查询返回的nodeId
                String nodeIdFromQuery = (row.get("nodeId") != null) ? row.get("nodeId").toString() : null;
                String finalNodeId;
                if (nodeIdFromQuery != null && !nodeIdFromQuery.trim().isEmpty()) {
                    finalNodeId = nodeType.toLowerCase() + "_" + nodeIdFromQuery;
                    log.debug("【知识图谱】[转换器] 行 #{}：使用查询返回的nodeId: {}", i, finalNodeId);
                } else if (neo4jElementId != null && !neo4jElementId.trim().isEmpty()) {
                    finalNodeId = nodeType.toLowerCase() + "_" + neo4jElementId;
                    log.debug("【知识图谱】[转换器] 行 #{}：使用Neo4j elementId: {}", i, finalNodeId);
                } else {
                    finalNodeId = generateNodeId(nodeData, nodeType);
                    log.debug("【知识图谱】[转换器] 行 #{}：使用属性生成的nodeId: {}", i, finalNodeId);
                }

                if (finalNodeId == null || finalNodeId.trim().isEmpty()) {
                    log.warn("【知识图谱】[转换器] 行 #{} 生成的节点ID为空，跳过该节点", i);
                    continue;
                }
                
                // 提取节点名称/标签
                String nodeLabel = extractNodeLabel(nodeData, nodeType);
                
                // 计算节点大小
                Double nodeSize = calculateNodeSize(nodeType);
                
                log.info("【知识图谱】[转换器] 节点信息预处理: nodeId={}, determinedType={}, extractedLabel={}, calculatedSize={}",
                 finalNodeId, nodeType, nodeLabel, nodeSize);
                // 更新统计计数
                if ("CONCEPT".equals(nodeType)) {
                    conceptCount++;
                } else if ("ARTICLE".equals(nodeType)) {
                    articleCount++;
                } else if ("NOTE".equals(nodeType)) {
                    noteCount++;
                }
                
                // 避免重复添加相同ID的节点
                if (!processedNodeIds.contains(finalNodeId)) {
                    // 清理properties - 移除不可序列化的字段和复杂对象
                    Map<String, Object> cleanProperties = new HashMap<>();
                    for (Map.Entry<String, Object> entry : nodeData.entrySet()) {
                        Object value = entry.getValue();
                        // 只保留基本类型和字符串，避免序列化问题
                        if (value instanceof String || value instanceof Number || 
                            value instanceof Boolean || value == null) {
                            cleanProperties.put(entry.getKey(), value);
                        } else if (value instanceof java.time.LocalDateTime) {
                            // 将LocalDateTime转换为字符串
                            cleanProperties.put(entry.getKey(), value.toString());
                        } else if (value instanceof java.time.LocalDate) {
                            // 将LocalDate转换为字符串
                            cleanProperties.put(entry.getKey(), value.toString());
                        } else {
                            // 其他复杂对象转换为字符串，避免序列化问题
                            cleanProperties.put(entry.getKey(), String.valueOf(value));
                        }
                    }
                    
                    GraphDataDTO.NodeDTO nodeDTO = GraphDataDTO.NodeDTO.builder()
                            .id(finalNodeId)
                            .label(nodeLabel)
                            .type(nodeType)
                            .size(nodeSize)
                            .color(getNodeColor(nodeType))
                            .importance(nodeSize / 25.0)
                            .properties(cleanProperties)
                            .build();
                    
                    nodes.add(nodeDTO);
                    processedNodeIds.add(finalNodeId);
                    log.info("【知识图谱】[转换器] 成功添加NodeDTO: id={}", finalNodeId);
                    log.debug("【知识图谱】添加节点: id={}, label={}, type={}", finalNodeId, nodeLabel, nodeType);
                }
                
                // 处理关系数据
                Object relationshipsObj = row.get("relationships");
                if (relationshipsObj instanceof List) {
                    List<Map<String, Object>> relationships = (List<Map<String, Object>>) relationshipsObj;
                    log.debug("【知识图谱】节点 #{} 有 {} 个关系", i, relationships.size());
                    processRelationships(relationships, finalNodeId, edges, processedEdgeIds);
                } else {
                    log.debug("【知识图谱】节点 #{} 没有关系数据", i);
                }
                
            } catch (Exception e) {
                errorCount++;
                log.warn("【知识图谱】处理节点 #{} 时出错: {}", i, e.getMessage());
                log.debug("【知识图谱】处理节点异常详情", e);
                
                // 如果错误太多，打印一个详细的错误记录
                if (errorCount > 10 && errorCount % 10 == 0) {
                    log.error("【知识图谱】处理中累计出现{}个错误，最新错误：", errorCount, e);
                }
            }
        }
        
        // 计算统计信息
        GraphDataDTO.StatisticsDTO statistics = GraphDataDTO.StatisticsDTO.builder()
                .totalNodes(nodes.size())
                .totalEdges(edges.size())
                .articleCount(articleCount)
                .noteCount(noteCount)
                .conceptCount(conceptCount)
                .avgConnectivity(nodes.isEmpty() ? 0.0 : (double) edges.size() / nodes.size())
                .build();
        
        log.info("【知识图谱】转换完成: nodes={}, edges={}, statistics={}", 
                nodes.size(), edges.size(), statistics);
        
        // 如果没有获取到任何节点，则返回测试数据
        if (nodes.isEmpty()) {
            log.warn("【知识图谱】转换结果为空，返回测试数据");
            return createTestGraphData();
        }
        
        return GraphDataDTO.builder()
                .nodes(nodes)
                .edges(edges)
                .statistics(statistics)
                .build();
    }
    
    // 辅助方法：从节点数据推断标签
    private List<String> inferLabelsFromNodeData(Map<String, Object> nodeData) {
        List<String> inferredLabels = new ArrayList<>();
        
        try {
            // 如果节点已有标签，则使用现有标签
            if (nodeData.containsKey("labels") && nodeData.get("labels") instanceof List<?>) {
                List<?> existingLabels = (List<?>) nodeData.get("labels");
                for (Object label : existingLabels) {
                    if (label instanceof String) {
                        inferredLabels.add((String) label);
                    }
                }
                
                if (!inferredLabels.isEmpty()) {
                    log.debug("【知识图谱】使用节点现有标签: {}", inferredLabels);
                    return inferredLabels;
                }
            }
            
            // 根据属性推断节点类型
            if (nodeData.containsKey("name") && !nodeData.containsKey("title")) {
                inferredLabels.add("Concept");
                log.debug("【知识图谱】通过'name'属性推断为Concept节点");
            } else if (nodeData.containsKey("title")) {
                if (nodeData.containsKey("publishDate") || nodeData.containsKey("author")) {
                    inferredLabels.add("Article");
                    log.debug("【知识图谱】通过'title'和'publishDate/author'属性推断为Article节点");
                } else {
                    inferredLabels.add("Note");
                    log.debug("【知识图谱】通过'title'属性(无publishDate/author)推断为Note节点");
                }
            } else {
                // 尝试使用其他指标
                if (nodeData.containsKey("mysql_id")) {
                    inferredLabels.add("Note");
                    log.debug("【知识图谱】通过'mysql_id'属性推断为Note节点");
                } else if (nodeData.containsKey("description") || nodeData.containsKey("context")) {
                    inferredLabels.add("Concept");
                    log.debug("【知识图谱】通过'description/context'属性推断为Concept节点");
                }
            }
        } catch (Exception e) {
            log.warn("【知识图谱】从节点数据推断标签时出错: {}", e.getMessage());
        }
        
        log.debug("【知识图谱】最终推断标签结果: {}", inferredLabels);
        return inferredLabels;
    }
    
    // 辅助方法：确定节点类型
    private String determineNodeType(List<String> labels) {
        try {
            // 检查每个标签，支持更多变体
            for (String label : labels) {
                // 忽略空标签
                if (label == null || label.trim().isEmpty()) {
                    continue;
                }
                
                String normalized = label.toLowerCase().trim();
                
                // 概念节点检查 - 支持更多变体
                if (normalized.contains("concept") || 
                    normalized.equals("c") || 
                    normalized.equals("conceptnode") || 
                    normalized.contains("知识点") ||
                    normalized.contains("概念")) {
                    log.debug("【知识图谱】标签'{}'判定为CONCEPT类型", label);
                    return "CONCEPT";
                }
                // 文章节点检查 - 支持更多变体
                else if (normalized.contains("article") || 
                         normalized.contains("post") || 
                         normalized.equals("a") || 
                         normalized.equals("articlenode") ||
                         normalized.contains("文章")) {
                    log.debug("【知识图谱】标签'{}'判定为ARTICLE类型", label);
                    return "ARTICLE";
                }
                // 笔记节点检查 - 支持更多变体
                else if (normalized.contains("note") || 
                         normalized.contains("memo") || 
                         normalized.equals("n") || 
                         normalized.equals("notenode") ||
                         normalized.contains("笔记")) {
                    log.debug("【知识图谱】标签'{}'判定为NOTE类型", label);
                    return "NOTE";
                }
            }
        } catch (Exception e) {
            log.warn("【知识图谱】确定节点类型时出错: {}", e.getMessage());
        }
        
        log.debug("【知识图谱】无法确定节点类型，标签列表: {}", labels);
        return "UNKNOWN";
    }
    
    // 辅助方法：生成节点ID
    private String generateNodeId(Map<String, Object> nodeData, String nodeType) {
        try {
            // 根据节点类型使用不同的策略生成有意义的ID
            if ("CONCEPT".equals(nodeType)) {
                // 对于概念节点，优先使用名称生成ID，而不是技术性的Neo4j ID
                if (nodeData.containsKey("name")) {
                    String name = (String) nodeData.get("name");
                    if (name != null && !name.trim().isEmpty()) {
                        // 清理名称，生成安全的ID
                        String safeName = name.trim()
                            .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\s]", "") // 保留中文、英文、数字、下划线、短横线和空格
                            .replaceAll("\\s+", "_") // 将空格转换为下划线
                            .replaceAll("_{2,}", "_") // 将多个连续下划线合并为一个
                            .replaceAll("^_|_$", ""); // 去除首尾下划线
                        
                        if (!safeName.isEmpty()) {
                            String conceptId = "concept_" + safeName;
                            log.debug("【知识图谱】概念节点使用name生成ID: {} -> {}", name, conceptId);
                            return conceptId;
                        }
                    }
                }
                
                // 如果名称不可用，尝试其他字段
                for (String idField : Arrays.asList("id", "_id", "conceptId")) {
                    if (nodeData.containsKey(idField) && nodeData.get(idField) != null) {
                        Object dbId = nodeData.get(idField);
                        String conceptId = "concept_" + dbId.toString();
                        log.debug("【知识图谱】概念节点使用{}字段作为ID: {}", idField, conceptId);
                        return conceptId;
                    }
                }
                
                // 最后备选：生成随机ID
                String randomId = "concept_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                log.debug("【知识图谱】概念节点生成随机ID: {}", randomId);
                return randomId;
                
            } else if ("ARTICLE".equals(nodeType)) {
                // 优先使用有意义的文章ID
                for (String idField : Arrays.asList("id", "mysql_id", "articleId", "_id")) {
                    if (nodeData.containsKey(idField) && nodeData.get(idField) != null) {
                        Object articleDbId = nodeData.get(idField);
                        String articleId = "article_" + articleDbId.toString();
                        log.debug("【知识图谱】文章节点使用{}字段作为ID: {}", idField, articleId);
                        return articleId;
                    }
                }
                
                // 如果没有找到ID，使用标题作为ID
                if (nodeData.containsKey("title")) {
                    String title = (String) nodeData.get("title");
                    if (title != null && !title.isEmpty()) {
                        String safeTitle = title.trim()
                            .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\s]", "")
                            .replaceAll("\\s+", "_")
                            .substring(0, Math.min(title.length(), 30));
                        String articleId = "article_" + safeTitle;
                        log.debug("【知识图谱】文章节点使用title生成ID: {} -> {}", title, articleId);
                        return articleId;
                    }
                }
                
                // 生成随机文章ID
                String randomId = "article_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                log.debug("【知识图谱】文章节点生成随机ID: {}", randomId);
                return randomId;
                
            } else if ("NOTE".equals(nodeType)) {
                // 优先使用有意义的笔记ID
                for (String idField : Arrays.asList("mysql_id", "id", "noteId", "_id")) {
                    if (nodeData.containsKey(idField) && nodeData.get(idField) != null) {
                        Object noteDbId = nodeData.get(idField);
                        String noteId = "note_" + noteDbId.toString();
                        log.debug("【知识图谱】笔记节点使用{}字段作为ID: {}", idField, noteId);
                        return noteId;
                    }
                }
                
                // 如果没有找到ID，使用标题作为ID
                if (nodeData.containsKey("title")) {
                    String title = (String) nodeData.get("title");
                    if (title != null && !title.isEmpty()) {
                        String safeTitle = title.trim()
                            .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\s]", "")
                            .replaceAll("\\s+", "_")
                            .substring(0, Math.min(title.length(), 30));
                        String noteId = "note_" + safeTitle;
                        log.debug("【知识图谱】笔记节点使用title生成ID: {} -> {}", title, noteId);
                        return noteId;
                    }
                }
                
                // 生成随机笔记ID
                String randomId = "note_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                log.debug("【知识图谱】笔记节点生成随机ID: {}", randomId);
                return randomId;
            }
            // 未知节点类型的最后备选方案
            String randomId = nodeType.toLowerCase() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            log.debug("【知识图谱】未知节点类型生成随机ID: {}", randomId);
            return randomId;
            
        } catch (Exception e) {
            log.warn("【知识图谱】生成节点ID时出错: {}", e.getMessage());
            return nodeType.toLowerCase() + "_" + System.currentTimeMillis();
        }
    }
    
    // 辅助方法：提取节点标签
    private String extractNodeLabel(Map<String, Object> nodeData, String nodeType) {
        log.debug("【知识图谱】提取节点标签: nodeType={}, nodeData={}", nodeType, nodeData);
        
        if ("CONCEPT".equals(nodeType)) {
            // 尝试多个可能的名称字段，按优先级顺序
            String[] nameFields = {"name", "title", "label", "conceptName", "text"};
            
            for (String field : nameFields) {
                String value = (String) nodeData.get(field);
                if (value != null && !value.trim().isEmpty() && !value.equals("概念")) {
                    log.debug("【知识图谱】概念节点使用字段'{}'作为标签: {}", field, value.trim());
                    return value.trim();
                }
            }
            
            // 尝试从ID中提取有意义的名称
            String id = (String) nodeData.get("id");
            if (id != null && !id.trim().isEmpty() && !id.startsWith("concept_") && !id.contains(":")) {
                log.debug("【知识图谱】概念节点使用id字段作为标签: {}", id.trim());
                return id.trim();
            }
            
            // 检查是否有description字段可以作为名称
            String description = (String) nodeData.get("description");
            if (description != null && !description.trim().isEmpty() && description.length() < 50) {
                log.debug("【知识图谱】概念节点使用description字段作为标签: {}", description.trim());
                return description.trim();
            }
            
            // 最后的备选方案：生成描述性名称
            log.warn("【知识图谱】概念节点缺少有效名称字段，节点数据: {}", nodeData);
            return "未命名概念_" + System.currentTimeMillis();
        } else {
            // 对于文章和笔记节点
            String[] titleFields = {"title", "name", "label", "subject"};
            
            for (String field : titleFields) {
                String value = (String) nodeData.get(field);
                if (value != null && !value.trim().isEmpty()) {
                    log.debug("【知识图谱】{}节点使用字段'{}'作为标签: {}", nodeType, field, value.trim());
                    return value.trim();
                }
            }
            
            log.warn("【知识图谱】{}节点缺少有效标题字段，节点数据: {}", nodeType, nodeData);
            return ("ARTICLE".equals(nodeType) ? "未命名文章" : "未命名笔记") + "_" + System.currentTimeMillis();
        }
    }
    
    // 辅助方法：计算节点大小
    private Double calculateNodeSize(String nodeType) {
        if ("CONCEPT".equals(nodeType)) {
            return 15.0;
        } else if ("ARTICLE".equals(nodeType)) {
            return 18.0;
        } else { // NOTE
            return 12.0;
        }
    }
    
    // 辅助方法：处理关系，使用更健壮的错误处理
    private void processRelationships(List<Map<String, Object>> relationships, String sourceNodeId, 
                                     List<GraphDataDTO.EdgeDTO> edges, Set<String> processedEdgeIds) {
        if (relationships == null || relationships.isEmpty()) {
            log.debug("【知识图谱】无关系数据需要处理");
            return;
        }
        
        int errorCount = 0;
        
        for (int i = 0; i < relationships.size(); i++) {
            try {
                Map<String, Object> relData = relationships.get(i);
                if (relData == null) {
                    log.debug("【知识图谱】跳过空的关系数据");
                    continue;
                }
                
                // 尝试获取关系对象
                Object relObj = relData.get("rel");
                Object targetObj = relData.get("target");
                
                // 验证关系和目标节点不为空
                if (relObj == null) {
                    log.debug("【知识图谱】关系对象为空，跳过");
                    continue;
                }
                
                if (targetObj == null) {
                    log.debug("【知识图谱】目标节点为空，跳过");
                    continue;
                }
                
                // 处理关系对象 - 支持Neo4j Driver的Relationship对象
                Map<String, Object> rel = new HashMap<>();
                if (relObj instanceof org.neo4j.driver.types.Relationship) {
                    org.neo4j.driver.types.Relationship relationship = (org.neo4j.driver.types.Relationship) relObj;
                    rel.put("type", relationship.type());
                    rel.put("id", relationship.elementId());
                    rel.putAll(relationship.asMap()); // 添加所有属性
                    log.debug("【知识图谱】处理Neo4j Relationship对象: type={}, id={}", relationship.type(), relationship.elementId());
                } else if (relObj instanceof Map) {
                    rel = (Map<String, Object>) relObj;
                    log.debug("【知识图谱】处理Map类型关系对象");
                } else {
                    log.warn("【知识图谱】关系对象类型不支持: {}, 跳过", relObj.getClass().getName());
                    continue;
                }
                
                // 处理目标节点对象 - 支持Neo4j Driver的Node对象
                Map<String, Object> targetNode = new HashMap<>();
                if (targetObj instanceof org.neo4j.driver.types.Node) {
                    org.neo4j.driver.types.Node node = (org.neo4j.driver.types.Node) targetObj;
                    targetNode.putAll(node.asMap()); // 添加所有属性
                    targetNode.put("id", node.elementId());
                    log.debug("【知识图谱】处理Neo4j Node对象: elementId={}, 属性数量={}", node.elementId(), node.asMap().size());
                } else if (targetObj instanceof Map) {
                    targetNode = (Map<String, Object>) targetObj;
                    log.debug("【知识图谱】处理Map类型目标节点对象");
                } else {
                    log.warn("【知识图谱】目标节点对象类型不支持: {}, 跳过", targetObj.getClass().getName());
                    continue;
                }
                
                // 获取目标节点标签
                List<String> targetLabels = null;
                Object targetLabelsObj = relData.get("targetLabels");
                
                if (targetLabelsObj != null) {
                    try {
                        targetLabels = (List<String>) targetLabelsObj;
                    } catch (ClassCastException e) {
                        log.debug("【知识图谱】目标标签类型转换失败: {}", e.getMessage());
                    }
                }
                
                // 确定目标节点类型
                String targetType = "UNKNOWN";
                
                // 使用标签确定类型
                if (targetLabels != null && !targetLabels.isEmpty()) {
                    targetType = determineNodeType(targetLabels);
                    log.debug("【知识图谱】基于标签确定目标节点类型: {}", targetType);
                } 
                
                // 如果类型仍未知，尝试从节点属性推断
                if ("UNKNOWN".equals(targetType)) {
                    // 尝试从目标节点数据推断标签
                    List<String> inferredLabels = inferLabelsFromNodeData(targetNode);
                    if (!inferredLabels.isEmpty()) {
                        targetType = determineNodeType(inferredLabels);
                        log.debug("【知识图谱】通过推断标签确定目标节点类型: {}", targetType);
                    }
                }
                
                // 如果类型仍未知，基于属性直接确定
                if ("UNKNOWN".equals(targetType)) {
                    if (targetNode.containsKey("name") && !targetNode.containsKey("title")) {
                        targetType = "CONCEPT";
                    } else if (targetNode.containsKey("title") && targetNode.containsKey("author")) {
                        targetType = "ARTICLE";
                    } else if (targetNode.containsKey("title") || targetNode.containsKey("mysql_id")) {
                        targetType = "NOTE";
                    } else {
                        // 如果实在无法确定，默认为概念类型
                        targetType = "CONCEPT";
                    }
                    log.debug("【知识图谱】基于属性确定目标节点类型: {}", targetType);
                }
                
                // 生成目标节点ID - 使用健壮的方法
                String targetId = generateNodeId(targetNode, targetType);
                
                // 获取关系类型，提供默认值和安全处理
                String relType;
                if (rel.containsKey("type") && rel.get("type") != null) {
                    relType = rel.get("type").toString();
                } else {
                    // 查找其他可能包含类型信息的字段
                    if (rel.containsKey("relationType") && rel.get("relationType") != null) {
                        relType = rel.get("relationType").toString();
                    } else {
                        relType = "RELATED_TO";  // 默认关系类型
                    }
                }
                
                // 生成关系ID
                String relId;
                if (rel.containsKey("id") && rel.get("id") != null) {
                    relId = rel.get("id").toString();
                } else if (rel.containsKey("elementId") && rel.get("elementId") != null) {
                    relId = rel.get("elementId").toString();
                } else {
                    // 生成唯一ID
                    relId = "rel_" + relType + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
                }
                
                // 确保不重复添加相同的边
                String edgeKey = sourceNodeId + "_" + targetId + "_" + relType;
                if (!processedEdgeIds.contains(edgeKey)) {
                    // 计算边权重
                    double weight = 1.0;
                    if (rel.containsKey("relevance") && rel.get("relevance") instanceof Number) {
                        weight = ((Number) rel.get("relevance")).doubleValue();
                    } else if (rel.containsKey("weight") && rel.get("weight") instanceof Number) {
                        weight = ((Number) rel.get("weight")).doubleValue();
                    } else if (rel.containsKey("strength") && rel.get("strength") instanceof Number) {
                        weight = ((Number) rel.get("strength")).doubleValue();
                    }
                    
                    // 设置边标签
                    String edgeLabel = relType;
                    if (edgeLabel.contains("_")) {
                        // 美化边标签，将"MENTIONS_CONCEPT"转换为"mentions"
                        edgeLabel = edgeLabel.toLowerCase().split("_")[0];
                    }
                    
                    // 清理关系properties - 移除不可序列化的字段
                    Map<String, Object> cleanRelProperties = new HashMap<>();
                    for (Map.Entry<String, Object> entry : rel.entrySet()) {
                        Object value = entry.getValue();
                        // 只保留基本类型和字符串，避免序列化问题
                        if (value instanceof String || value instanceof Number || 
                            value instanceof Boolean || value == null) {
                            cleanRelProperties.put(entry.getKey(), value);
                        } else if (value instanceof java.time.LocalDateTime) {
                            cleanRelProperties.put(entry.getKey(), value.toString());
                        } else if (value instanceof java.time.LocalDate) {
                            cleanRelProperties.put(entry.getKey(), value.toString());
                        } else {
                            cleanRelProperties.put(entry.getKey(), String.valueOf(value));
                        }
                    }
                    
                    GraphDataDTO.EdgeDTO edgeDTO = GraphDataDTO.EdgeDTO.builder()
                            .id(relId)
                            .source(sourceNodeId)
                            .target(targetId)
                            .label(edgeLabel)
                            .type(relType)
                            .weight(weight)
                            .color(getEdgeColor(relType))
                            .properties(cleanRelProperties)
                            .build();
                    
                    edges.add(edgeDTO);
                    processedEdgeIds.add(edgeKey);
                    log.debug("【知识图谱】添加边: {} -> {}, type={}", sourceNodeId, targetId, relType);
                }
            } catch (Exception e) {
                errorCount++;
                log.debug("【知识图谱】处理关系 #{} 时出错: {}", i, e.getMessage());
                
                if (errorCount > 5 && errorCount % 5 == 0) {
                    log.warn("【知识图谱】关系处理中累计出现{}个错误", errorCount);
                }
            }
        }
    }
    
    // 辅助方法：处理优化查询的关系数据
    private void processOptimizedRelationships(List<Map<String, Object>> relationships, String sourceNodeId, 
                                     List<GraphDataDTO.EdgeDTO> edges, Set<String> processedEdgeIds) {
        if (relationships == null || relationships.isEmpty()) {
            log.debug("【知识图谱】无关系数据需要处理");
            return;
        }
        
        int errorCount = 0;
        int validRelCount = 0;
        
        for (int i = 0; i < relationships.size(); i++) {
            try {
                Map<String, Object> relData = relationships.get(i);
                if (relData == null || relData.isEmpty()) {
                    log.debug("【知识图谱】跳过空的关系数据 #{}", i);
                    continue;
                }
                
                // 获取关系类型 - 新的优化查询结构
                String relType = (String) relData.get("type");
                if (relType == null || relType.trim().isEmpty()) {
                    log.debug("【知识图谱】关系 #{} 缺少类型信息，跳过", i);
                    continue;
                }
                
                // 获取目标节点ID - 新的优化查询结构
                Object targetIdObj = relData.get("targetId");
                if (targetIdObj == null) {
                    log.debug("【知识图谱】关系 #{} 缺少目标节点ID，跳过", i);
                    continue;
                }
                
                String targetNodeInternalId = targetIdObj.toString();
                
                // 获取目标节点标签
                List<String> targetLabels = (List<String>) relData.get("targetLabels");
                if (targetLabels == null || targetLabels.isEmpty()) {
                    log.debug("【知识图谱】关系 #{} 缺少目标节点标签，跳过", i);
                    continue;
                }
                
                // 确定目标节点类型
                String targetType = determineNodeType(targetLabels);
                if ("UNKNOWN".equals(targetType)) {
                    // 为未知类型设置默认值
                    targetType = "CONCEPT";
                    log.debug("【知识图谱】关系 #{} 目标节点类型未知，使用默认值CONCEPT", i);
                }
                
                // 构造目标节点ID（假设我们需要为目标节点生成标准ID）
                String targetNodeId = targetType.toLowerCase() + "_" + targetNodeInternalId;
                
                // 生成边ID，确保唯一性
                String edgeKey = sourceNodeId + "_" + targetNodeId + "_" + relType;
                if (processedEdgeIds.contains(edgeKey)) {
                    log.debug("【知识图谱】关系 #{} 已存在，跳过重复边: {}", i, edgeKey);
                    continue;
                }
                
                // 设置边标签（美化显示）
                String edgeLabel = relType;
                if (edgeLabel.contains("_")) {
                    edgeLabel = edgeLabel.toLowerCase().split("_")[0];
                }
                
                // 创建边DTO
                GraphDataDTO.EdgeDTO edgeDTO = GraphDataDTO.EdgeDTO.builder()
                        .id("edge_" + sourceNodeId + "_" + targetNodeId + "_" + System.currentTimeMillis())
                        .source(sourceNodeId)
                        .target(targetNodeId)
                        .label(edgeLabel)
                        .type(relType)
                        .weight(0.5) // 默认权重
                        .color(getEdgeColor(relType))
                        .build();
                
                edges.add(edgeDTO);
                processedEdgeIds.add(edgeKey);
                validRelCount++;
                
                log.debug("【知识图谱】添加优化边: {} -> {}, type={}", sourceNodeId, targetNodeId, relType);
                
            } catch (Exception e) {
                errorCount++;
                log.debug("【知识图谱】处理优化关系 #{} 时出错: {}", i, e.getMessage());
                
                if (errorCount > 5 && errorCount % 5 == 0) {
                    log.warn("【知识图谱】优化关系处理中累计出现{}个错误", errorCount);
                }
            }
        }
        
        log.debug("【知识图谱】优化关系处理完成: 成功处理{}个关系，错误{}个", validRelCount, errorCount);
    }

    // 辅助方法：获取边的颜色
    private String getEdgeColor(String relationType) {
        switch (relationType) {
            case "MENTIONS_CONCEPT":
            case "MENTIONS":
                return "#2196f3";  // 蓝色
            case "CONTAINS_CONCEPT":
            case "CONTAINS":
                return "#4caf50";  // 绿色
            case "RELATED_TO":
                return "#9c27b0";  // 紫色
            case "KEYWORD":
            case "IS_KEYWORD":
                return "#ff9800";  // 橙色
            case "ENTITY":
            case "IS_ENTITY":
                return "#f44336";  // 红色
            default:
                return "#999999";  // 灰色
        }
    }

    @Override
    @Cacheable(value = "related-articles", key = "#conceptName + ':' + #limit",
               cacheManager = "knowledgeGraphCacheManager")
    public List<ArticleDTO> findRelatedArticlesByConcept(String conceptName, int limit) {
        log.info("查询概念相关文章: concept={}, limit={}", conceptName, limit);
        
        try {
            String cypher = """
                MATCH (c:ConceptNode {name: $conceptName})<-[r]-(a:ArticleNode)
                RETURN a, r
                ORDER BY r.relevance DESC, r.occurrences DESC
                LIMIT $limit
                """;
                
            Map<String, Object> params = Map.of(
                "conceptName", conceptName,
                "limit", limit
            );
            
            Collection<Map<String, Object>> resultCollection = neo4jClient.query(cypher)
                .bindAll(params)
                .fetch()
                .all();
            
            List<ArticleDTO> articles = new ArrayList<>();
            for (Map<String, Object> row : resultCollection) {
                Map<String, Object> articleNode = (Map<String, Object>) row.get("a");
                Map<String, Object> relationship = (Map<String, Object>) row.get("r");
                
                String articleId = (String) articleNode.get("id");
                
                // 从MySQL获取完整的文章信息
                Optional<ArticleMetadata> metadataOpt = articleMetadataRepository.findById(articleId);
                if (metadataOpt.isPresent()) {
                    ArticleMetadata metadata = metadataOpt.get();
                    
                    ArticleDTO dto = ArticleDTO.builder()
                            .id(metadata.getId())
                            .title(metadata.getTitle())
                            .summary(metadata.getSummaryText())
                            .publicationDate(metadata.getPublicationDate())
                            .author(metadata.getAuthor())
                            .originalUrl(metadata.getLinkToOriginal())
                            .build();
                    
                    articles.add(dto);
                }
            }
            
            return articles;
            
        } catch (Exception e) {
            log.error("查询概念相关文章失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = "concept-stats", key = "#conceptName",
               cacheManager = "knowledgeGraphCacheManager")
    public ConceptStatisticsDTO getConceptStatistics(String conceptName) {
        log.info("获取概念统计信息: {}", conceptName);
        
        try {
            String cypher = """
                MATCH (c:ConceptNode {name: $conceptName})
                OPTIONAL MATCH (c)<-[ar]-(a:ArticleNode)
                OPTIONAL MATCH (c)<-[nr]-(n:NoteNode) 
                OPTIONAL MATCH (c)-[rr]-(rc:ConceptNode)
                RETURN c,
                       COUNT(DISTINCT a) as articleCount,
                       COUNT(DISTINCT n) as noteCount,
                       COUNT(DISTINCT rc) as relatedConceptCount,
                       AVG(ar.relevance) as avgConfidence,
                       MIN(ar.createdAt) as firstMentioned,
                       MAX(ar.createdAt) as lastMentioned
                """;
                
            Map<String, Object> params = Map.of("conceptName", conceptName);
            
            Optional<Map<String, Object>> resultOpt = neo4jClient.query(cypher)
                .bindAll(params)
                .fetch()
                .one();
            
            if (resultOpt.isPresent()) {
                Map<String, Object> row = resultOpt.get();
                Map<String, Object> conceptNode = (Map<String, Object>) row.get("c");
                
                if (conceptNode == null) {
                    return null; // 概念不存在
                }
                
                return ConceptStatisticsDTO.builder()
                        .conceptName(conceptName)
                        .conceptType((String) conceptNode.get("type"))
                        .relatedArticleCount(((Number) row.get("articleCount")).intValue())
                        .relatedNoteCount(((Number) row.get("noteCount")).intValue())
                        .relatedConceptCount(((Number) row.get("relatedConceptCount")).intValue())
                        .avgRelevanceScore((Double) row.get("avgConfidence"))
                        .firstMentionAt((LocalDateTime) row.get("firstMentioned"))
                        .lastMentionAt((LocalDateTime) row.get("lastMentioned"))
                        .build();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("获取概念统计信息失败", e);
            return null;
        }
    }

    @Override
    public ConceptDetailDTO getConceptDetail(String conceptName) {
        log.info("获取概念详情: {}", conceptName);
        
        try {
            String cypher = """
                MATCH (c:ConceptNode {name: $conceptName})
                OPTIONAL MATCH (c)-[rr:RELATED_TO]-(rc:ConceptNode)
                RETURN c, collect(DISTINCT {concept: rc, strength: rr.strength}) as relatedConcepts
                """;
                
            Map<String, Object> params = Map.of("conceptName", conceptName);
            
            Optional<Map<String, Object>> resultOpt = neo4jClient.query(cypher)
                .bindAll(params)
                .fetch()
                .one();
            
            if (resultOpt.isPresent()) {
                Map<String, Object> row = resultOpt.get();
                Map<String, Object> conceptNode = (Map<String, Object>) row.get("c");
                
                if (conceptNode == null) {
                    return null; // 概念不存在
                }
                
                List<Map<String, Object>> relatedConceptsData = (List<Map<String, Object>>) row.get("relatedConcepts");
                List<ConceptDetailDTO.RelatedConceptDTO> relatedConcepts = new ArrayList<>();
                
                for (Map<String, Object> relatedData : relatedConceptsData) {
                    Map<String, Object> relatedConcept = (Map<String, Object>) relatedData.get("concept");
                    if (relatedConcept != null) {
                        Double strength = (Double) relatedData.get("strength");
                        
                        ConceptDetailDTO.RelatedConceptDTO relatedDTO = ConceptDetailDTO.RelatedConceptDTO.builder()
                                .id(relatedConcept.get("id").toString())
                                .name((String) relatedConcept.get("name"))
                                .relationStrength(strength != null ? strength : 0.0)
                                .build();
                        
                        relatedConcepts.add(relatedDTO);
                    }
                }
                
                return ConceptDetailDTO.builder()
                        .conceptId(conceptNode.get("id").toString())
                        .conceptName((String) conceptNode.get("name"))
                        .conceptType((String) conceptNode.get("type"))
                        .description((String) conceptNode.get("description"))
                        .synonyms(conceptNode.get("synonyms") != null ? 
                                 List.of((String[]) conceptNode.get("synonyms")) : null)
                        .relatedConcepts(relatedConcepts)
                        .build();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("获取概念详情失败", e);
            return null;
        }
    }

    // 辅助方法：获取节点颜色
    private String getNodeColor(String nodeType) {
        switch (nodeType) {
            case "CONCEPT": return "#9c27b0";
            case "ARTICLE": return "#2196f3";
            case "NOTE": return "#ff9800";
            default: return "#666666";
        }
    }
    
    // 新增测试数据生成方法
    private GraphDataDTO createTestGraphData() {
        List<GraphDataDTO.NodeDTO> nodes = new ArrayList<>();
        List<GraphDataDTO.EdgeDTO> edges = new ArrayList<>();
        
        // 添加测试数据
        addTestData(nodes, edges);
        
        // 创建统计信息
        GraphDataDTO.StatisticsDTO statistics = GraphDataDTO.StatisticsDTO.builder()
                .totalNodes(nodes.size())
                .totalEdges(edges.size())
                .articleCount((int)nodes.stream().filter(n -> "ARTICLE".equals(n.getType())).count())
                .noteCount((int)nodes.stream().filter(n -> "NOTE".equals(n.getType())).count())
                .conceptCount((int)nodes.stream().filter(n -> "CONCEPT".equals(n.getType())).count())
                .avgConnectivity(nodes.isEmpty() ? 0.0 : (double) edges.size() / nodes.size())
                .build();
        
        return GraphDataDTO.builder()
                .nodes(nodes)
                .edges(edges)
                .statistics(statistics)
                .build();
    }

    // 辅助方法：添加测试数据
    private void addTestData(List<GraphDataDTO.NodeDTO> nodes, List<GraphDataDTO.EdgeDTO> edges) {
        // 添加概念节点
        for (int i = 1; i <= 10; i++) {
            GraphDataDTO.NodeDTO conceptNode = GraphDataDTO.NodeDTO.builder()
                    .id("concept_测试概念" + i)
                    .label("测试概念" + i)
                    .type("CONCEPT")
                    .size(10.0 + i)
                    .color(getNodeColor("CONCEPT"))
                    .importance(0.6)
                    .build();
            nodes.add(conceptNode);
        }
        
        // 添加文章节点
        for (int i = 1; i <= 5; i++) {
            GraphDataDTO.NodeDTO articleNode = GraphDataDTO.NodeDTO.builder()
                    .id("article_" + i)
                    .label("测试文章" + i)
                    .type("ARTICLE")
                    .size(15.0)
                    .color(getNodeColor("ARTICLE"))
                    .importance(0.7)
                    .build();
            nodes.add(articleNode);
        }
        
        // 添加笔记节点
        for (int i = 1; i <= 3; i++) {
            GraphDataDTO.NodeDTO noteNode = GraphDataDTO.NodeDTO.builder()
                    .id("note_" + i)
                    .label("测试笔记" + i)
                    .type("NOTE")
                    .size(12.0)
                    .color(getNodeColor("NOTE"))
                    .importance(0.5)
                    .build();
            nodes.add(noteNode);
        }
        
        // 添加边 - 概念和文章之间
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 3; j++) {
                if (Math.random() > 0.3) {  // 随机添加部分关系
                    GraphDataDTO.EdgeDTO edge = GraphDataDTO.EdgeDTO.builder()
                            .id("edge_concept_article_" + i + "_" + j)
                            .source("concept_测试概念" + i)
                            .target("article_" + j)
                            .label("MENTIONS")
                            .type("MENTIONS")
                            .weight(0.8)
                            .color("#999999")
                            .build();
                    edges.add(edge);
                }
            }
        }
        
        // 添加边 - 概念和笔记之间
        for (int i = 6; i <= 10; i++) {
            for (int j = 1; j <= 3; j++) {
                if (Math.random() > 0.4) {  // 随机添加部分关系
                    GraphDataDTO.EdgeDTO edge = GraphDataDTO.EdgeDTO.builder()
                            .id("edge_concept_note_" + i + "_" + j)
                            .source("concept_测试概念" + i)
                            .target("note_" + j)
                            .label("CONTAINS")
                            .type("CONTAINS")
                            .weight(0.7)
                            .color("#999999")
                            .build();
                    edges.add(edge);
                }
            }
        }
        
        // 添加边 - 概念和概念之间
        for (int i = 1; i <= 5; i++) {
            for (int j = 6; j <= 10; j++) {
                if (Math.random() > 0.7) {  // 随机添加部分关系
                    GraphDataDTO.EdgeDTO edge = GraphDataDTO.EdgeDTO.builder()
                            .id("edge_concept_concept_" + i + "_" + j)
                            .source("concept_测试概念" + i)
                            .target("concept_测试概念" + j)
                            .label("RELATED_TO")
                            .type("RELATED_TO")
                            .weight(0.6)
                            .color("#999999")
                            .build();
                    edges.add(edge);
                }
            }
        }
        
        log.info("🔥 添加测试数据完成: nodes={}, edges={}", nodes.size(), edges.size());
    }

    // 备选数据处理方法 - 当常规转换失败时使用
    private GraphDataDTO processResultsWithFallback(List<Map<String, Object>> queryResults) {
        log.info("【知识图谱】开始备选数据处理，原始结果数: {}", queryResults.size());
        
        List<GraphDataDTO.NodeDTO> nodes = new ArrayList<>();
        List<GraphDataDTO.EdgeDTO> edges = new ArrayList<>();
        
        // 节点ID映射 - 用于创建边
        Map<Object, String> nodeIdMap = new HashMap<>();
        
        // 节点类型计数
        int conceptCount = 0, articleCount = 0, noteCount = 0;
        
        // 首先处理所有节点
        for (int i = 0; i < queryResults.size(); i++) {
            try {
                Map<String, Object> row = queryResults.get(i);
                
                // 提取节点数据
                Object nodeObj = row.get("n");
                if (nodeObj == null || !(nodeObj instanceof Map)) {
                    continue;
                }
                
                Map<String, Object> nodeData = (Map<String, Object>) nodeObj;
                Object nodeId = nodeData.get("id");  // 尝试获取节点ID
                
                // 确定节点类型和标签
                String nodeType = "UNKNOWN";
                String nodeLabel = "未命名节点";
                
                // 从标签或属性决定节点类型
                Object labelsObj = row.get("nodeLabels");
                List<String> labels = new ArrayList<>();
                
                if (labelsObj instanceof List) {
                    labels = (List<String>) labelsObj;
                }
                
                // 根据标签或属性确定节点类型
                if (labels.contains("Concept") || labels.contains("ConceptNode") || 
                    (nodeData.containsKey("name") && !nodeData.containsKey("title"))) {
                    nodeType = "CONCEPT";
                    nodeLabel = nodeData.containsKey("name") ? (String) nodeData.get("name") : "概念";
                    conceptCount++;
                } else if (labels.contains("Article") || labels.contains("ArticleNode") ||
                          (nodeData.containsKey("title") && nodeData.containsKey("author"))) {
                    nodeType = "ARTICLE";
                    nodeLabel = nodeData.containsKey("title") ? (String) nodeData.get("title") : "文章";
                    articleCount++;
                } else if (labels.contains("Note") || labels.contains("NoteNode") ||
                          (nodeData.containsKey("title") && !nodeData.containsKey("author"))) {
                    nodeType = "NOTE";
                    nodeLabel = nodeData.containsKey("title") ? (String) nodeData.get("title") : "笔记";
                    noteCount++;
                }
                
                // 生成唯一ID
                String uniqueNodeId;
                if (nodeId != null) {
                    uniqueNodeId = nodeType.toLowerCase() + "_" + nodeId;
                } else if (nodeData.containsKey("name")) {
                    uniqueNodeId = nodeType.toLowerCase() + "_" + nodeData.get("name").toString().replaceAll("[^a-zA-Z0-9_]", "_");
                } else if (nodeData.containsKey("title")) {
                    uniqueNodeId = nodeType.toLowerCase() + "_" + nodeData.get("title").toString().replaceAll("[^a-zA-Z0-9_]", "_").substring(0, Math.min(20, nodeData.get("title").toString().length()));
                } else {
                    uniqueNodeId = nodeType.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
                }
                
                // 存储节点ID映射，用于后续创建边
                nodeIdMap.put(nodeObj, uniqueNodeId);
                
                // 设置节点大小
                Double nodeSize = 10.0;
                if ("CONCEPT".equals(nodeType)) {
                    nodeSize = 15.0;
                } else if ("ARTICLE".equals(nodeType)) {
                    nodeSize = 18.0;
                } else if ("NOTE".equals(nodeType)) {
                    nodeSize = 12.0;
                }
                
                // 创建节点DTO
                GraphDataDTO.NodeDTO nodeDTO = GraphDataDTO.NodeDTO.builder()
                        .id(uniqueNodeId)
                        .label(nodeLabel)
                        .type(nodeType)
                        .size(nodeSize)
                        .color(getNodeColor(nodeType))
                        .importance(nodeSize / 25.0)
                        .properties(nodeData)
                        .build();
                
                nodes.add(nodeDTO);
                log.debug("【知识图谱】备选处理添加节点: {}, 类型: {}", nodeLabel, nodeType);
            } catch (Exception e) {
                log.warn("【知识图谱】备选处理节点 #{} 时出错: {}", i, e.getMessage());
            }
        }
        
        // 然后处理关系，创建边
        for (int i = 0; i < queryResults.size(); i++) {
            try {
                Map<String, Object> row = queryResults.get(i);
                Object nodeObj = row.get("n");
                
                if (nodeObj == null || !nodeIdMap.containsKey(nodeObj)) {
                    continue;
                }
                
                String sourceNodeId = nodeIdMap.get(nodeObj);
                
                // 处理关系
                Object relsObj = row.get("outRelationships");
                if (relsObj instanceof List) {
                    List<Map<String, Object>> relationships = (List<Map<String, Object>>) relsObj;
                    
                    for (Map<String, Object> relData : relationships) {
                        try {
                            Object relObj = relData.get("rel");
                            Object targetObj = relData.get("target");
                            
                            if (relObj == null || targetObj == null || !nodeIdMap.containsKey(targetObj)) {
                                continue;
                            }
                            
                            String targetNodeId = nodeIdMap.get(targetObj);
                            Map<String, Object> rel = (Map<String, Object>) relObj;
                            
                            // 获取关系类型
                            String relType = rel.containsKey("type") ? rel.get("type").toString() : "RELATED";
                            
                            // 生成唯一边ID
                            String edgeId = "edge_" + UUID.randomUUID().toString().substring(0, 8);
                            
                            // 边标签处理
                            String edgeLabel = relType;
                            if (edgeLabel.contains("_")) {
                                // 美化边标签，将"MENTIONS_CONCEPT"转换为"mentions"
                                edgeLabel = edgeLabel.toLowerCase().split("_")[0];
                            }
                            
                            // 创建边DTO
                            GraphDataDTO.EdgeDTO edgeDTO = GraphDataDTO.EdgeDTO.builder()
                                    .id(edgeId)
                                    .source(sourceNodeId)
                                    .target(targetNodeId)
                                    .label(edgeLabel)
                                    .type(relType)
                                    .weight(0.8)
                                    .color(getEdgeColor(relType))
                                    .properties(rel)
                                    .build();
                            
                            edges.add(edgeDTO);
                            log.debug("【知识图谱】备选处理添加边: {} -> {}, 类型: {}", sourceNodeId, targetNodeId, relType);
                        } catch (Exception e) {
                            log.debug("【知识图谱】备选处理关系时出错: {}", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("【知识图谱】备选处理节点关系 #{} 时出错: {}", i, e.getMessage());
            }
        }
        
        // 创建统计信息
        GraphDataDTO.StatisticsDTO statistics = GraphDataDTO.StatisticsDTO.builder()
                .totalNodes(nodes.size())
                .totalEdges(edges.size())
                .articleCount(articleCount)
                .noteCount(noteCount)
                .conceptCount(conceptCount)
                .avgConnectivity(nodes.isEmpty() ? 0.0 : (double) edges.size() / nodes.size())
                .build();
        
        log.info("【知识图谱】备选处理完成: 节点={}, 边={}, 文章={}, 概念={}, 笔记={}", 
                nodes.size(), edges.size(), articleCount, conceptCount, noteCount);
        
        return GraphDataDTO.builder()
                .nodes(nodes)
                .edges(edges)
                .statistics(statistics)
                .build();
    }
} 