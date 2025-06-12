package com.aireader.backend.test;

import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.mongo.ArticleContent;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.mongo.ArticleContentRepository;
import com.aireader.backend.service.ArticleFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/test")
public class EncodingTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(EncodingTestController.class);
    
    @Autowired
    private ArticleMetadataRepository articleMetadataRepository;
    
    @Autowired
    private ArticleContentRepository articleContentRepository;
    
    @Autowired
    private ArticleFetchService articleFetchService;
    
    /**
     * 重新抓取特定文章内容，测试编码修复
     */
    @PostMapping("/refetch-article/{articleId}")
    public String refetchArticleContent(@PathVariable String articleId) {
        try {
            logger.info("开始重新抓取文章内容: {}", articleId);
            
            // 查找文章元数据
            Optional<ArticleMetadata> optionalMetadata = articleMetadataRepository.findById(articleId);
            if (!optionalMetadata.isPresent()) {
                return "文章元数据不存在: " + articleId;
            }
            
            ArticleMetadata metadata = optionalMetadata.get();
            
            // 删除现有的MongoDB内容
            Optional<ArticleContent> existingContent = articleContentRepository.findByMysqlMetadataId(articleId);
            if (existingContent.isPresent()) {
                articleContentRepository.delete(existingContent.get());
                logger.info("已删除现有内容，准备重新抓取");
            }
            
            // 重新抓取内容 - 使用反射调用私有方法
            try {
                java.lang.reflect.Method method = articleFetchService.getClass().getDeclaredMethod("fetchAndSaveArticleContent", ArticleMetadata.class);
                method.setAccessible(true);
                method.invoke(articleFetchService, metadata);
                
                return "重新抓取成功！文章ID: " + articleId;
            } catch (Exception e) {
                logger.error("反射调用失败，尝试直接重新抓取", e);
                return "重新抓取失败: " + e.getMessage();
            }
            
        } catch (Exception e) {
            logger.error("重新抓取文章内容失败: {}", articleId, e);
            return "重新抓取失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取文章内容摘要，用于检查编码
     */
    @GetMapping("/check-encoding/{articleId}")
    public String checkArticleEncoding(@PathVariable String articleId) {
        try {
            Optional<ArticleContent> optionalContent = articleContentRepository.findByMysqlMetadataId(articleId);
            if (!optionalContent.isPresent()) {
                return "文章内容不存在: " + articleId;
            }
            
            ArticleContent content = optionalContent.get();
            String title = content.getTitleFromContent();
            String htmlContent = content.getFullHtmlContent();
            
            StringBuilder result = new StringBuilder();
            result.append("文章ID: ").append(articleId).append("\n");
            result.append("标题: ").append(title).append("\n");
            result.append("HTML内容长度: ").append(htmlContent != null ? htmlContent.length() : 0).append("\n");
            result.append("HTML内容前200字符: ").append(htmlContent != null ? htmlContent.substring(0, Math.min(200, htmlContent.length())) : "无内容").append("\n");
            
            return result.toString();
            
        } catch (Exception e) {
            logger.error("检查文章编码失败: {}", articleId, e);
            return "检查失败: " + e.getMessage();
        }
    }
} 