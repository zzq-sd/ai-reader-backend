# Spring AI 开发文档

## 1. 简介

Spring AI 是Spring生态系统的一部分，旨在简化将人工智能功能集成到应用程序中的过程。它提供了丰富的抽象和接口，使开发人员能够轻松地与各种AI模型和服务进行交互。

Spring AI 主要解决了AI集成的基本挑战：**将企业数据和API与AI模型连接起来**。

## 2. 版本与依赖

本项目使用Spring AI 1.0.0版本，该版本是首个正式发布版本，提供了稳定的API和丰富的功能。

### 2.1 Maven依赖配置

在`pom.xml`中添加以下依赖：

```xml
<!-- Spring AI BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Spring AI OpenAI依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Spring AI向量存储依赖 - Neo4j -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-neo4j-store</artifactId>
</dependency>
```

### 2.2 应用程序属性配置

在`application.properties`或`application.yml`中配置Spring AI:

```properties
# OpenAI配置
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.model=gpt-3.5-turbo
spring.ai.openai.temperature=0.7

# 向量存储配置 - Neo4j
spring.neo4j.uri=bolt://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=password
```

## 3. 核心功能与组件

Spring AI 1.0.0提供的主要功能包括：

### 3.1 ChatClient API

ChatClient是与AI模型交互的主要接口，提供了流畅的API，类似于WebClient和RestClient。

```java
@Autowired
private OpenAiChatClient chatClient;

public String getResponse(String prompt) {
    return chatClient.call(prompt);
}

// 带系统提示的调用
public String getResponseWithSystemPrompt(String prompt) {
    Prompt systemPrompt = new Prompt(
        List.of(
            new SystemMessage("你是一个文章内容分析助手，擅长提取关键概念和实体。"),
            new UserMessage(prompt)
        )
    );
    return chatClient.call(systemPrompt);
}
```

### 3.2 结构化输出

可以将AI模型的输出直接映射到Java对象：

```java
public ArticleAnalysis analyzeArticle(String articleContent) {
    Prompt prompt = new Prompt(
        "分析以下文章内容，提取关键概念、实体和主题：" + articleContent
    );
    
    return chatClient.call(prompt, ArticleAnalysis.class);
}

// 结构化输出类
@Data
public class ArticleAnalysis {
    private List<String> concepts;
    private List<String> entities;
    private List<String> keywords;
    private String mainTopic;
}
```

### 3.3 检索增强生成 (RAG)

RAG是一种结合检索系统和生成AI的方法，可以使AI模型基于特定数据集回答问题：

```java
@Autowired
private Neo4jVectorStore vectorStore;

@Autowired
private OpenAiChatClient chatClient;

public String answerQuestionWithRAG(String question) {
    // 创建向量嵌入
    List<Document> relevantDocs = vectorStore.similaritySearch(question);
    
    // 构建提示，包含检索到的相关文档
    StringBuilder context = new StringBuilder();
    for (Document doc : relevantDocs) {
        context.append(doc.getContent()).append("\n\n");
    }
    
    Prompt prompt = new Prompt(
        List.of(
            new SystemMessage("基于以下信息回答用户问题。如果无法从提供的信息中找到答案，请说'我无法从提供的信息中找到答案'。\n\n信息: " + context.toString()),
            new UserMessage(question)
        )
    );
    
    return chatClient.call(prompt);
}
```

### 3.4 工具调用 (Tool Calling)

Spring AI支持工具调用，允许AI模型调用应用程序中的工具和函数：

```java
@Autowired
private OpenAiChatClient chatClient;

public String processWithTools(String query) {
    // 定义工具
    ToolSpecification searchTool = ToolSpecification.builder()
            .name("search_articles")
            .description("搜索相关文章")
            .parameter("query", String.class, "搜索关键词")
            .build();
    
    // 创建工具处理器
    ToolCallbackFactory toolCallbackFactory = 
        ToolCallbackFactory.builder()
            .register("search_articles", params -> {
                String searchQuery = params.get("query").toString();
                // 实际的搜索逻辑
                return searchArticlesByKeyword(searchQuery);
            })
            .build();
    
    // 创建支持工具调用的ChatClient
    OpenAiChatOptions options = OpenAiChatOptions.builder()
            .withTools(List.of(searchTool))
            .build();
    
    // 带工具的提示执行
    Prompt prompt = new Prompt("我想找关于机器学习的最新文章");
    return chatClient.call(prompt, toolCallbackFactory, options);
}
```

### 3.5 Advisor API

Advisors是一种拦截器链，允许修改发送到模型的提示，注入检索数据和会话记忆：

```java
@Autowired
private Neo4jVectorStore vectorStore;

@Autowired
private OpenAiChatClient chatClient;

public ChatClient createAdvisorChatClient() {
    // 创建检索增强Advisor
    VectorStoreRetriever retriever = new VectorStoreRetriever(vectorStore);
    retriever.setMaxResults(3);
    
    RetrievalAugmentor augmentor = new RetrievalAugmentor(retriever);
    augmentor.setThreshold(0.7);
    
    // 创建会话记忆Advisor
    WindowBufferMemory memory = new WindowBufferMemory();
    
    // 组合Advisors
    List<ChatClientAdvisor> advisors = List.of(
        new RetrievalAugmentationAdvisor(augmentor),
        new MemoryAdvisor(memory)
    );
    
    // 创建带Advisors的ChatClient
    return AdvisedChatClient.builder(chatClient)
        .withAdvisors(advisors)
        .build();
}
```

### 3.6 向量存储抽象

Spring AI提供了对20多种向量数据库的支持，包括Neo4j、MongoDB等：

```java
@Autowired
private Neo4jVectorStore vectorStore;

@Autowired
private EmbeddingClient embeddingClient;

// 将文档存储到向量库
public void storeDocuments(List<Document> documents) {
    vectorStore.add(documents);
}

// 相似度搜索
public List<Document> findSimilarDocuments(String query) {
    return vectorStore.similaritySearch(query);
}

// 使用过滤条件进行搜索
public List<Document> findSimilarDocumentsWithFilter(String query, String type) {
    String filter = "metadata.type = '" + type + "'";
    return vectorStore.similaritySearch(query, filter);
}
```

## 4. 在AI阅读器系统中的应用

### 4.1 文章内容分析服务

我们将使用Spring AI实现文章的智能分析，包括关键词提取、实体识别和主题分类：

```java
@Service
public class ArticleAiAnalysisService {
    
    @Autowired
    private OpenAiChatClient chatClient;
    
    @Autowired
    private Neo4jVectorStore vectorStore;
    
    @Autowired
    private EmbeddingClient embeddingClient;
    
    /**
     * 分析文章内容，提取关键信息
     */
    public ArticleAnalysisResult analyzeArticle(String content) {
        Prompt prompt = new Prompt(
            List.of(
                new SystemMessage("你是一个专业的文章分析助手。请分析提供的文章内容，提取以下信息：\n" +
                                 "1. 主要关键词（最多10个）\n" +
                                 "2. 关键实体（人物、组织、地点等）\n" +
                                 "3. 文章的主要主题或分类\n" +
                                 "4. 情感倾向（积极、消极或中性）"),
                new UserMessage(content)
            )
        );
        
        return chatClient.call(prompt, ArticleAnalysisResult.class);
    }
    
    /**
     * 将文章向量化并存储到向量数据库
     */
    public void vectorizeAndStoreArticle(Long articleId, String title, String content) {
        // 创建文档对象
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", articleId.toString());
        metadata.put("title", title);
        metadata.put("type", "article");
        
        Document document = new Document(content, metadata);
        
        // 存储到向量数据库
        vectorStore.add(List.of(document));
    }
    
    /**
     * 获取与查询相似的文章
     */
    public List<RelatedItemDto> findSimilarArticles(String query) {
        List<Document> similarDocs = vectorStore.similaritySearch(query, "metadata.type = 'article'");
        
        return similarDocs.stream()
            .map(doc -> {
                Map<String, Object> metadata = doc.getMetadata();
                return RelatedItemDto.builder()
                    .itemId((String) metadata.get("id"))
                    .title((String) metadata.get("title"))
                    .type(RelatedItemDto.ItemType.ARTICLE)
                    .summary(doc.getContent().substring(0, Math.min(200, doc.getContent().length())) + "...")
                    .build();
            })
            .collect(Collectors.toList());
    }
}
```

### 4.2 知识图谱构建服务

使用Spring AI分析文章和笔记，并将提取的实体、概念和关系构建到Neo4j知识图谱中：

```java
@Service
public class KnowledgeGraphService {
    
    @Autowired
    private OpenAiChatClient chatClient;
    
    @Autowired
    private Neo4jOperations neo4jOperations;
    
    /**
     * 从文本中提取知识图谱元素
     */
    public KnowledgeGraphElements extractKnowledgeElements(String content) {
        Prompt prompt = new Prompt(
            List.of(
                new SystemMessage("你是一个专业的知识图谱构建助手。请从以下文本中提取实体、概念和它们之间的关系："),
                new UserMessage(content)
            )
        );
        
        return chatClient.call(prompt, KnowledgeGraphElements.class);
    }
    
    /**
     * 将知识元素添加到图谱中
     */
    public void updateKnowledgeGraph(KnowledgeGraphElements elements, String sourceId, String sourceType) {
        // 添加概念节点
        for (Concept concept : elements.getConcepts()) {
            Map<String, Object> params = Map.of(
                "name", concept.getName(),
                "description", concept.getDescription()
            );
            
            neo4jOperations.query(
                "MERGE (c:Concept {name: $name}) " +
                "ON CREATE SET c.description = $description " +
                "ON MATCH SET c.description = CASE WHEN c.description IS NULL THEN $description ELSE c.description END " +
                "RETURN c",
                params
            );
        }
        
        // 添加实体节点
        for (Entity entity : elements.getEntities()) {
            Map<String, Object> params = Map.of(
                "name", entity.getName(),
                "type", entity.getType()
            );
            
            neo4jOperations.query(
                "MERGE (e:Entity {name: $name, type: $type}) " +
                "RETURN e",
                params
            );
        }
        
        // 添加关系
        for (Relationship rel : elements.getRelationships()) {
            Map<String, Object> params = Map.of(
                "source", rel.getSource(),
                "target", rel.getTarget(),
                "type", rel.getType(),
                "sourceId", sourceId,
                "sourceType", sourceType
            );
            
            neo4jOperations.query(
                "MATCH (s {name: $source}), (t {name: $target}) " +
                "MERGE (s)-[r:" + rel.getType() + " {sourceId: $sourceId, sourceType: $sourceType}]->(t) " +
                "RETURN r",
                params
            );
        }
        
        // 与源内容建立关系
        if ("article".equals(sourceType)) {
            // 连接文章与概念
            for (Concept concept : elements.getConcepts()) {
                Map<String, Object> params = Map.of(
                    "articleId", sourceId,
                    "conceptName", concept.getName()
                );
                
                neo4jOperations.query(
                    "MATCH (a:ArticleNode {id: $articleId}), (c:Concept {name: $conceptName}) " +
                    "MERGE (a)-[r:MENTIONS_CONCEPT]->(c) " +
                    "RETURN r",
                    params
                );
            }
        } else if ("note".equals(sourceType)) {
            // 连接笔记与概念
            for (Concept concept : elements.getConcepts()) {
                Map<String, Object> params = Map.of(
                    "noteId", sourceId,
                    "conceptName", concept.getName()
                );
                
                neo4jOperations.query(
                    "MATCH (n:NoteNode {id: $noteId}), (c:Concept {name: $conceptName}) " +
                    "MERGE (n)-[r:CONTAINS_CONCEPT]->(c) " +
                    "RETURN r",
                    params
                );
            }
        }
    }
}

@Data
public class KnowledgeGraphElements {
    private List<Concept> concepts;
    private List<Entity> entities;
    private List<Relationship> relationships;
}

@Data
public class Concept {
    private String name;
    private String description;
}

@Data
public class Entity {
    private String name;
    private String type; // 例如：Person, Organization, Location等
}

@Data
public class Relationship {
    private String source;
    private String target;
    private String type;
}
```

### 4.3 知识图谱查询和可视化

使用Neo4j查询知识图谱，并生成可视化数据：

```java
@Service
public class KnowledgeVisualizationService {
    
    @Autowired
    private Neo4jOperations neo4jOperations;
    
    /**
     * 获取以特定节点为中心的子图
     */
    public GraphVisualizationDto getGraphVisualizationData(String centerNodeId, int depth) {
        Map<String, Object> params = Map.of(
            "nodeId", centerNodeId,
            "depth", depth
        );
        
        // 查询节点
        List<Map<String, Object>> nodeResults = neo4jOperations.query(
            "MATCH (n) " +
            "WHERE id(n) = $nodeId " +
            "CALL apoc.path.subgraphNodes(n, {maxLevel: $depth}) YIELD node " +
            "RETURN id(node) as id, labels(node) as labels, node.name as name, " +
            "node.type as type, properties(node) as properties",
            params,
            Map.class
        );
        
        // 查询关系
        List<Map<String, Object>> edgeResults = neo4jOperations.query(
            "MATCH (n) " +
            "WHERE id(n) = $nodeId " +
            "CALL apoc.path.subgraphAll(n, {maxLevel: $depth}) YIELD nodes, relationships " +
            "UNWIND relationships AS r " +
            "RETURN id(r) as id, id(startNode(r)) as source, id(endNode(r)) as target, " +
            "type(r) as label, properties(r) as properties",
            params,
            Map.class
        );
        
        // 转换为DTO
        List<GraphVisualizationDto.Node> nodes = nodeResults.stream()
            .map(row -> {
                List<String> labels = (List<String>) row.get("labels");
                String type = labels != null && !labels.isEmpty() ? labels.get(0) : "Unknown";
                
                return GraphVisualizationDto.Node.builder()
                    .id(row.get("id").toString())
                    .label((String) row.get("name"))
                    .type(type)
                    .properties((Map<String, Object>) row.get("properties"))
                    .build();
            })
            .collect(Collectors.toList());
        
        List<GraphVisualizationDto.Edge> edges = edgeResults.stream()
            .map(row -> GraphVisualizationDto.Edge.builder()
                .id(row.get("id").toString())
                .source(row.get("source").toString())
                .target(row.get("target").toString())
                .label((String) row.get("label"))
                .properties((Map<String, Object>) row.get("properties"))
                .build()
            )
            .collect(Collectors.toList());
        
        return GraphVisualizationDto.builder()
            .nodes(nodes)
            .edges(edges)
            .build();
    }
}
```

## 5. 最佳实践与注意事项

### 5.1 提示工程 (Prompt Engineering)

- 使用明确、具体的指令
- 提供良好的示例
- 分解复杂任务为简单步骤
- 使用系统消息定义角色和行为
- 避免太模糊或太复杂的提示

### 5.2 模型选择

- 对于简单的文本生成和分析任务，可使用 gpt-3.5-turbo
- 对于更复杂的理解和推理任务，考虑使用 gpt-4
- 对于嵌入向量生成，使用适当的嵌入模型

### 5.3 性能优化

- 合理设置向量检索的结果数量
- 使用缓存机制减少重复计算
- 批处理大量文档的向量化过程
- 使用异步处理大型文档

### 5.4 安全性考虑

- 安全存储API密钥
- 实施速率限制以控制API调用
- 对用户输入进行验证和清理
- 使用审核模型检查有害内容

## 6. 参考资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/index.html)
- [Spring AI GitHub 仓库](https://github.com/spring-projects/spring-ai)
- [Spring Initializr](https://start.spring.io)
- [Spring AI 示例项目](https://github.com/spring-projects/spring-ai-examples) 