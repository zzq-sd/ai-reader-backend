// ===================================================================
// Neo4j 知识图谱性能优化索引脚本
// 阶段六：系统集成与优化
// ===================================================================

// === 基础节点索引 ===

// 概念节点索引
CREATE INDEX concept_name_index IF NOT EXISTS FOR (c:Concept) ON (c.name);
CREATE INDEX concept_type_index IF NOT EXISTS FOR (c:Concept) ON (c.type);
CREATE INDEX concept_frequency_index IF NOT EXISTS FOR (c:Concept) ON (c.totalFrequency);
CREATE INDEX concept_created_index IF NOT EXISTS FOR (c:Concept) ON (c.createdAt);

// 文章节点索引
CREATE INDEX article_id_index IF NOT EXISTS FOR (a:Article) ON (a.articleId);
CREATE INDEX article_title_index IF NOT EXISTS FOR (a:Article) ON (a.title);
CREATE INDEX article_publish_date_index IF NOT EXISTS FOR (a:Article) ON (a.publishDate);
CREATE INDEX article_source_index IF NOT EXISTS FOR (a:Article) ON (a.source);

// 笔记节点索引
CREATE INDEX note_id_index IF NOT EXISTS FOR (n:Note) ON (n.noteId);
CREATE INDEX note_user_index IF NOT EXISTS FOR (n:Note) ON (n.userId);
CREATE INDEX note_created_index IF NOT EXISTS FOR (n:Note) ON (n.createdAt);

// 作者节点索引
CREATE INDEX author_name_index IF NOT EXISTS FOR (au:Author) ON (au.name);

// === 复合索引（提升复杂查询性能）===

// 概念类型和频次复合索引
CREATE INDEX concept_type_freq_composite IF NOT EXISTS FOR (c:Concept) ON (c.type, c.totalFrequency);

// 概念名称和类型复合索引
CREATE INDEX concept_name_type_composite IF NOT EXISTS FOR (c:Concept) ON (c.name, c.type);

// 文章发布日期和来源复合索引
CREATE INDEX article_date_source_composite IF NOT EXISTS FOR (a:Article) ON (a.publishDate, a.source);

// 笔记用户和创建时间复合索引
CREATE INDEX note_user_created_composite IF NOT EXISTS FOR (n:Note) ON (n.userId, n.createdAt);

// === 全文搜索索引 ===

// 概念名称全文搜索
CREATE FULLTEXT INDEX concept_fulltext_index IF NOT EXISTS FOR (c:Concept) ON EACH [c.name, c.description];

// 文章标题和摘要全文搜索
CREATE FULLTEXT INDEX article_fulltext_index IF NOT EXISTS FOR (a:Article) ON EACH [a.title, a.summary];

// 笔记标题全文搜索
CREATE FULLTEXT INDEX note_fulltext_index IF NOT EXISTS FOR (n:Note) ON EACH [n.title];

// === 关系索引 ===

// CONTAINS关系索引（文章包含概念）
CREATE INDEX contains_confidence_index IF NOT EXISTS FOR ()-[r:CONTAINS]-() ON (r.confidence);
CREATE INDEX contains_frequency_index IF NOT EXISTS FOR ()-[r:CONTAINS]-() ON (r.frequency);

// DISCUSSES关系索引（笔记讨论概念）
CREATE INDEX discusses_confidence_index IF NOT EXISTS FOR ()-[r:DISCUSSES]-() ON (r.confidence);

// RELATED_TO关系索引（概念间关系）
CREATE INDEX related_strength_index IF NOT EXISTS FOR ()-[r:RELATED_TO]-() ON (r.strength);

// === 性能优化约束 ===

// 确保概念名称唯一性
CREATE CONSTRAINT concept_name_unique IF NOT EXISTS FOR (c:Concept) REQUIRE c.name IS UNIQUE;

// 确保文章ID唯一性
CREATE CONSTRAINT article_id_unique IF NOT EXISTS FOR (a:Article) REQUIRE a.articleId IS UNIQUE;

// 确保笔记ID唯一性
CREATE CONSTRAINT note_id_unique IF NOT EXISTS FOR (n:Note) REQUIRE n.noteId IS UNIQUE;

// === 查询性能验证脚本 ===

// 验证概念查询性能
// EXPLAIN MATCH (c:Concept {name: 'Spring Boot'}) RETURN c;

// 验证相关文章查询性能
// EXPLAIN MATCH (c:Concept {name: 'Spring Boot'})<-[r:CONTAINS]-(a:Article) 
// RETURN a ORDER BY r.confidence DESC LIMIT 10;

// 验证概念搜索性能
// EXPLAIN CALL db.index.fulltext.queryNodes('concept_fulltext_index', 'Spring*') 
// YIELD node RETURN node LIMIT 10;

// 验证复合查询性能
// EXPLAIN MATCH (c:Concept {type: 'TECHNOLOGY'}) 
// WHERE c.totalFrequency > 10 
// RETURN c ORDER BY c.totalFrequency DESC LIMIT 20;

// === 统计信息更新 ===

// 更新数据库统计信息以优化查询计划
CALL db.stats.retrieve('GRAPH COUNTS');

// === 索引状态检查 ===

// 查看所有索引状态
SHOW INDEXES;

// 查看约束状态
SHOW CONSTRAINTS;

// === 清理脚本（如需重建索引时使用）===

// 注意：以下命令会删除所有索引，请谨慎使用
// DROP INDEX concept_name_index IF EXISTS;
// DROP INDEX concept_type_index IF EXISTS;
// DROP INDEX concept_frequency_index IF EXISTS;
// DROP INDEX article_id_index IF EXISTS;
// DROP INDEX note_id_index IF EXISTS;

// === 性能监控查询 ===

// 查看最常用的概念
// MATCH (c:Concept) 
// RETURN c.name, c.type, c.totalFrequency 
// ORDER BY c.totalFrequency DESC 
// LIMIT 20;

// 查看概念分布统计
// MATCH (c:Concept) 
// RETURN c.type, count(*) as count, avg(c.totalFrequency) as avgFreq
// ORDER BY count DESC;

// 查看文章-概念关系统计
// MATCH ()-[r:CONTAINS]->() 
// RETURN count(r) as totalRelations, 
//        avg(r.confidence) as avgConfidence,
//        avg(r.frequency) as avgFrequency;

// === 数据完整性检查 ===

// 检查孤立的概念节点
// MATCH (c:Concept) 
// WHERE NOT (c)<-[:CONTAINS]-() AND NOT (c)<-[:DISCUSSES]-()
// RETURN count(c) as orphanedConcepts;

// 检查没有概念的文章
// MATCH (a:Article) 
// WHERE NOT (a)-[:CONTAINS]->()
// RETURN count(a) as articlesWithoutConcepts;

// === 备份建议 ===

// 在执行索引优化前，建议备份数据库：
// CALL apoc.export.cypher.all("backup.cypher", {format: "cypher-shell"});

// === 执行完成提示 ===

// 索引创建完成后，建议重启Neo4j服务以确保最佳性能
// 监控查询执行计划的变化，确认索引被正确使用 