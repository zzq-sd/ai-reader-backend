package com.aireader.backend.service.impl;

import com.aireader.backend.ai.AiService;
import com.aireader.backend.ai.ArticleAnalysisResult;
import com.aireader.backend.ai.NoteAnalysisResult;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            // 执行Cypher查询，通过共享概念找到相关内容
            String cypher = 
                "MATCH (note:NoteNode {id: $noteId})-[:CONTAINS_CONCEPT]->(concept:ConceptNode)" +
                "<-[r]-(related) " + 
                "WHERE type(r) IN ['MENTIONS_CONCEPT', 'CONTAINS_CONCEPT'] " +
                "AND NOT related:NoteNode OR related.id <> $noteId " +
                "WITH related, count(concept) AS commonConcepts, collect(concept.name) AS conceptNames " +
                "ORDER BY commonConcepts DESC " +
                "LIMIT $limit " +
                "RETURN related, commonConcepts, conceptNames";
            
            Map<String, Object> params = Map.of(
                "noteId", noteId,
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
            log.error("获取笔记相关项失败，笔记ID: " + noteId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<RelatedItemDto> getRelatedItemsForConcept(String conceptId, int limit) {
        log.info("获取概念相关项，概念ID: {}，限制数量: {}", conceptId, limit);
        
        try {
            // 执行Cypher查询，找到与概念相关的内容
            String cypher = 
                "MATCH (concept:ConceptNode)-[r]-(related) " +
                "WHERE id(concept) = $conceptId " +
                "AND type(r) IN ['MENTIONS_CONCEPT', 'CONTAINS_CONCEPT'] " +
                "WITH related, type(r) AS relationType " +
                "ORDER BY relationType " +
                "LIMIT $limit " +
                "RETURN related, relationType";
            
            Map<String, Object> params = Map.of(
                "conceptId", Long.parseLong(conceptId),
                "limit", limit
            );
            
            Collection<Map<String, Object>> resultCollection = neo4jClient.query(cypher)
                .bindAll(params)
                .fetch()
                .all();
                
            List<Map<String, Object>> results = new ArrayList<>(resultCollection);
            
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
                    
                    if (labels.contains("ArticleNode")) {
                        itemType = "ARTICLE";
                        id = (String) node.get("id");
                        title = (String) node.get("title");
                    } else if (labels.contains("NoteNode")) {
                        itemType = "NOTE";
                        id = (String) node.get("id");
                        title = (String) node.get("title");
                    } else if (labels.contains("ConceptNode")) {
                        itemType = "CONCEPT";
                        id = node.get("id").toString();
                        title = (String) node.get("name");
                    }
                    
                    if (!id.isEmpty() && !title.isEmpty()) {
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
                startMatch = "MATCH (center:ArticleNode {id: $nodeId})";
            } else if ("note".equalsIgnoreCase(nodeType)) {
                startMatch = "MATCH (center:NoteNode {id: $nodeId})";
            } else if ("concept".equalsIgnoreCase(nodeType)) {
                startMatch = "MATCH (center:ConceptNode) WHERE id(center) = $nodeId";
            } else {
                throw new IllegalArgumentException("不支持的节点类型: " + nodeType);
            }
            
            // 节点查询
            String nodeQuery = startMatch +
                " CALL apoc.path.subgraphNodes(center, {maxLevel: $depth}) YIELD node" +
                " RETURN id(node) as id, labels(node) as labels, properties(node) as props";
            
            // 关系查询
            String relationshipQuery = startMatch +
                " CALL apoc.path.spanningTree(center, {maxLevel: $depth})" +
                " YIELD path" +
                " WITH relationships(path) as rels" +
                " UNWIND rels as rel" +
                " RETURN id(rel) as id, id(startNode(rel)) as source, id(endNode(rel)) as target," +
                " type(rel) as type, properties(rel) as props";
            
            Map<String, Object> params = Map.of(
                "nodeId", "concept".equalsIgnoreCase(nodeType) ? Long.parseLong(centerNodeId) : centerNodeId,
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
                    
                    if (nodeType2.equals("ArticleNode")) {
                        nodeLabel = (String) props.getOrDefault("title", "未知文章");
                    } else if (nodeType2.equals("NoteNode")) {
                        nodeLabel = (String) props.getOrDefault("title", "未知笔记");
                    } else if (nodeType2.equals("ConceptNode")) {
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
            String cypher = "MATCH (c:ConceptNode) WHERE c.name CONTAINS $name RETURN c ORDER BY c.name LIMIT $limit";
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
            String cypher = "MATCH (c:ConceptNode) WHERE c.name = $name RETURN c";
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
                        "MATCH (a:ArticleNode {id: $articleId}) " +
                        "MATCH (c:ConceptNode) WHERE id(c) = $conceptId " +
                        "MERGE (a)-[r:MENTIONS_CONCEPT]->(c) " +
                        "ON CREATE SET r.relevance = 0.8, r.manuallyAdded = true, r.createdAt = datetime() " +
                        "ON MATCH SET r.relevance = 0.8, r.manuallyAdded = true, r.updatedAt = datetime() " +
                        "RETURN r";
                    
                    Map<String, Object> params = Map.of(
                        "articleId", articleId,
                        "conceptId", conceptNode.getId()
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
                        "MATCH (n:NoteNode {id: $noteId}) " +
                        "MATCH (c:ConceptNode) WHERE id(c) = $conceptId " +
                        "MERGE (n)-[r:CONTAINS_CONCEPT]->(c) " +
                        "ON CREATE SET r.relevance = 0.8, r.manuallyAdded = true, r.createdAt = datetime() " +
                        "ON MATCH SET r.relevance = 0.8, r.manuallyAdded = true, r.updatedAt = datetime() " +
                        "RETURN r";
                    
                    Map<String, Object> params = Map.of(
                        "noteId", noteId,
                        "conceptId", conceptNode.getId()
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
        String cypher = "MATCH (a:ArticleNode) WHERE a.id = $id RETURN a";
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
        // 查找现有节点
        String cypher = "MATCH (n:NoteNode) WHERE n.id = $id RETURN n";
        Map<String, Object> params = Map.of("id", note.getId());
        
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
        } else {
            // 创建新节点
            node = new NoteNode();
            node.setId(note.getId());
            node.setTitle(note.getTitle());
            node.setUserId(note.getUserId());
            node.setCreatedAt(LocalDateTime.now());
            node.setUpdatedAt(LocalDateTime.now());
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
        for (Map<String, String> conceptMap : analysisResult.getConcepts()) {
            String name = conceptMap.get("name");
            String description = conceptMap.get("description");
            
            if (name != null && !name.isBlank()) {
                ConceptNode concept = saveOrUpdateConcept(name, description);
                
                ArticleConceptRelationship relationship = new ArticleConceptRelationship();
                relationship.setArticle(articleNode);
                relationship.setConcept(concept);
                relationship.setRelationType("CONCEPT");
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
            
            NoteConceptRelationship relationship = new NoteConceptRelationship();
            relationship.setNote(noteNode);
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
                
                NoteConceptRelationship relationship = new NoteConceptRelationship();
                relationship.setNote(noteNode);
                relationship.setConcept(concept);
                relationship.setRelationType("ENTITY");
                relationship.setEntityType(type);
                relationship.setCreatedAt(LocalDateTime.now());
                
                neo4jTemplate.save(relationship);
            }
        }
        
        // 处理笔记中的主题
        if (analysisResult.getTopic() != null && !analysisResult.getTopic().isBlank()) {
            ConceptNode concept = saveOrUpdateConcept(analysisResult.getTopic(), "笔记主题");
                
            NoteConceptRelationship relationship = new NoteConceptRelationship();
            relationship.setNote(noteNode);
            relationship.setConcept(concept);
            relationship.setRelationType("TOPIC");
            relationship.setCreatedAt(LocalDateTime.now());
            
            neo4jTemplate.save(relationship);
        }
        
        // 处理关键观点
        for (String keyPoint : analysisResult.getKeyPoints()) {
            if (keyPoint != null && !keyPoint.isBlank()) {
                // 关键观点可能较长，作为概念处理时可能需要截断
                String conceptName = keyPoint.length() > 100 ? keyPoint.substring(0, 100) + "..." : keyPoint;
                ConceptNode concept = saveOrUpdateConcept(conceptName, "笔记观点: " + keyPoint);
                
                NoteConceptRelationship relationship = new NoteConceptRelationship();
                relationship.setNote(noteNode);
                relationship.setConcept(concept);
                relationship.setRelationType("KEY_POINT");
                relationship.setCreatedAt(LocalDateTime.now());
                
                neo4jTemplate.save(relationship);
            }
        }
        
        // 处理相关概念
        for (String relatedConcept : analysisResult.getRelatedConcepts()) {
            if (relatedConcept != null && !relatedConcept.isBlank()) {
                ConceptNode concept = saveOrUpdateConcept(relatedConcept, "相关概念");
                
                NoteConceptRelationship relationship = new NoteConceptRelationship();
                relationship.setNote(noteNode);
                relationship.setConcept(concept);
                relationship.setRelationType("RELATED_CONCEPT");
                relationship.setCreatedAt(LocalDateTime.now());
                
                neo4jTemplate.save(relationship);
            }
        }
        
        log.info("笔记知识图谱关系创建完成，笔记ID: {}", noteNode.getMysqlId());
    }
    
    // 辅助方法：转换查询结果为RelatedItemDto
    private List<RelatedItemDto> transformToRelatedItemDto(List<Map<String, Object>> results) {
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
                
                if (labels.contains("ArticleNode")) {
                    itemType = "ARTICLE";
                    id = (String) node.get("id");
                    title = (String) node.get("title");
                    
                    // 尝试获取摘要，使用String类型的ID
                    Optional<ArticleMetadata> metadata = articleMetadataRepository.findById(id);
                    if (metadata.isPresent()) {
                        summary = metadata.get().getSummary();
                    }
                } else if (labels.contains("NoteNode")) {
                    itemType = "NOTE";
                    id = (String) node.get("id");
                    title = (String) node.get("title");
                    
                    // 尝试获取摘要，使用String类型的ID
                    Optional<Note> note = noteRepository.findById(id);
                    if (note.isPresent()) {
                        String content = note.get().getContent();
                        summary = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                    }
                } else if (labels.contains("ConceptNode")) {
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
} 