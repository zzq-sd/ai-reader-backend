package com.aireader.backend.service.impl;

import com.aireader.backend.config.RabbitMQConfig;
import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.RssSourceDTO;
import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.RssSource;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.model.jpa.Note;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.RssSourceRepository;
import com.aireader.backend.repository.UserRepository;
import com.aireader.backend.repository.mongo.ArticleContentRepository;
import com.aireader.backend.repository.UserArticleInteractionRepository;
import com.aireader.backend.repository.ArticleTagRepository;
import com.aireader.backend.repository.NoteRepository;
import com.aireader.backend.service.ArticleFetchService;
import com.aireader.backend.service.RssFeedService;
import com.aireader.backend.service.ReadStatusService;
import com.aireader.backend.service.FavoriteService;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RSS源管理服务实现类
 */
@Service
@Slf4j
public class RssFeedServiceImpl implements RssFeedService {

    private final RssSourceRepository rssSourceRepository;
    private final UserRepository userRepository;
    private final ArticleMetadataRepository articleMetadataRepository;
    private final ArticleFetchService articleFetchService;
    private final RabbitTemplate rabbitTemplate;
    private final ArticleContentRepository articleContentRepository;
    private final ReadStatusService readStatusService;
    private final FavoriteService favoriteService;
    private final UserArticleInteractionRepository userArticleInteractionRepository;
    private final ArticleTagRepository articleTagRepository;
    private final NoteRepository noteRepository;
    
    @Value("${rss.fetch.timeout}")
    private int fetchTimeout;
    
    @Autowired
    public RssFeedServiceImpl(RssSourceRepository rssSourceRepository,
                             UserRepository userRepository,
                             ArticleMetadataRepository articleMetadataRepository,
                             ArticleFetchService articleFetchService,
                             RabbitTemplate rabbitTemplate,
                             ArticleContentRepository articleContentRepository,
                             ReadStatusService readStatusService,
                             FavoriteService favoriteService,
                             UserArticleInteractionRepository userArticleInteractionRepository,
                             ArticleTagRepository articleTagRepository,
                             NoteRepository noteRepository) {
        this.rssSourceRepository = rssSourceRepository;
        this.userRepository = userRepository;
        this.articleMetadataRepository = articleMetadataRepository;
        this.articleFetchService = articleFetchService;
        this.rabbitTemplate = rabbitTemplate;
        this.articleContentRepository = articleContentRepository;
        this.readStatusService = readStatusService;
        this.favoriteService = favoriteService;
        this.userArticleInteractionRepository = userArticleInteractionRepository;
        this.articleTagRepository = articleTagRepository;
        this.noteRepository = noteRepository;
    }
    
    /**
     * 添加新的RSS源
     * 
     * @param rssSourceDTO RSS源信息
     * @param userId 用户ID
     * @return 保存后的RSS源信息
     */
    @Override
    @Transactional
    public RssSourceDTO addRssSource(RssSourceDTO rssSourceDTO, String userId) {
        // 验证RSS源URL
        if (!validateRssUrl(rssSourceDTO.getUrl())) {
            throw new RuntimeException("无效的RSS源URL");
        }
        
        // 查找用户 (userId 是真正的用户ID/UUID)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查是否已存在相同URL的RSS源
        Optional<RssSource> existingSource = rssSourceRepository.findByUrl(rssSourceDTO.getUrl());
        
        if (existingSource.isPresent()) {
            // 如果已存在，检查是否是公共源或用户自己的源
            RssSource source = existingSource.get();
            if (source.isPublic() || (source.getUser() != null && source.getUser().getId().equals(user.getId()))) {
                return convertToDto(source);
            }
        }

        // 创建新的RSS源
        RssSource rssSource = new RssSource();
        rssSource.setName(rssSourceDTO.getName());
        rssSource.setUrl(rssSourceDTO.getUrl());
        rssSource.setDescription(rssSourceDTO.getDescription());
        rssSource.setCategory(rssSourceDTO.getCategory());
        rssSource.setPublic(rssSourceDTO.isPublic());
        rssSource.setUser(user);
        rssSource.setCreatedAt(LocalDateTime.now());
        rssSource.setUpdatedAt(LocalDateTime.now());
        rssSource.setLastFetchedAt(null);
        
        // 设置RSSHub特定字段
        rssSource.setIsRsshub(rssSourceDTO.isRsshub());
        rssSource.setRsshubRoute(rssSourceDTO.getRsshubRoute());
        rssSource.setRsshubInstance(rssSourceDTO.getRsshubInstance());
        
        // 保存RSS源
        RssSource savedSource = rssSourceRepository.save(rssSource);
        
        // 触发文章抓取（异步）
        scheduleRssFetch(savedSource.getId());
        
        return convertToDto(savedSource);
    }
    
    /**
     * 获取用户的所有RSS源
     * 
     * @param userId 用户ID
     * @return RSS源列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<RssSourceDTO> getUserRssSources(String userId) {
        // 查找用户 (userId 是真正的用户ID/UUID)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

        List<RssSource> userSources = rssSourceRepository.findByUserId(user.getId()); // 使用 user.getId() (UUID)
        List<RssSource> publicSources = rssSourceRepository.findByIsPublicTrue();
        
        // 合并用户自己的源和公共源，去重
        Set<RssSource> allSources = new HashSet<>(userSources);
        allSources.addAll(publicSources);
        
        return allSources.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有公共RSS源
     * 
     * @return 公共RSS源列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<RssSourceDTO> getPublicRssSources() {
        return rssSourceRepository.findByIsPublicTrue().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取RSS源
     * 
     * @param sourceId RSS源ID
     * @return 可选的RSS源
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<RssSourceDTO> getRssSourceById(String sourceId) {
        return rssSourceRepository.findById(sourceId)
                .map(this::convertToDto);
    }
    
    /**
     * 更新RSS源信息
     * 
     * @param sourceId RSS源ID
     * @param rssSourceDTO 更新的RSS源信息
     * @param userId 用户ID
     * @return 更新后的RSS源信息
     */
    @Override
    @Transactional
    public RssSourceDTO updateRssSource(String sourceId, RssSourceDTO rssSourceDTO, String userId) {
        RssSource rssSource = rssSourceRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("RSS源不存在"));
        
        // 检查权限 (userId 是真正的用户ID/UUID)
        if (rssSource.getUser() == null || !rssSource.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权修改此RSS源");
        }
        
        // 保存原始URL，用于后续比较
        String originalUrl = rssSource.getUrl();
        
        // 如果URL发生变化，需要验证新URL
        if (!originalUrl.equals(rssSourceDTO.getUrl()) && !validateRssUrl(rssSourceDTO.getUrl())) {
            throw new RuntimeException("无效的RSS源URL");
        }
        
        // 更新RSS源信息
        rssSource.setName(rssSourceDTO.getName());
        rssSource.setUrl(rssSourceDTO.getUrl());
        rssSource.setDescription(rssSourceDTO.getDescription());
        rssSource.setCategory(rssSourceDTO.getCategory());
        rssSource.setPublic(rssSourceDTO.isPublic());
        rssSource.setActive(rssSourceDTO.isActive());
        rssSource.setUpdatedAt(LocalDateTime.now());
        
        // 保存更新后的RSS源
        RssSource updatedSource = rssSourceRepository.save(rssSource);
        
        // 如果URL发生变化，触发文章抓取
        if (!originalUrl.equals(rssSourceDTO.getUrl())) {
            new Thread(() -> articleFetchService.fetchArticlesFromSource(updatedSource)).start();
        }
        
        return convertToDto(updatedSource);
    }
    
    /**
     * 删除RSS源及其关联的所有数据
     * 
     * @param sourceId RSS源ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteRssSource(String sourceId, String userId) {
        try {
            // 1. 检查RSS源是否存在
            Optional<RssSource> rssSourceOpt = rssSourceRepository.findById(sourceId);
            if (rssSourceOpt.isEmpty()) {
                log.warn("尝试删除不存在的RSS源: {}", sourceId);
                return false;
            }
            
            RssSource rssSource = rssSourceOpt.get();
            
            // 2. 检查权限 - 确保用户只能删除自己的RSS源
            if (rssSource.getUser() == null || !rssSource.getUser().getId().equals(userId)) {
                log.warn("用户 {} 尝试删除不属于自己的RSS源: {}", userId, sourceId);
                throw new RuntimeException("无权删除此RSS源");
            }
            
            // 3. 调用内部删除方法
            return deleteRssSource(sourceId);
            
        } catch (Exception e) {
            log.error("删除RSS源时发生错误: sourceId={}, userId={}, error={}", sourceId, userId, e.getMessage(), e);
            throw new RuntimeException("删除RSS源失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除RSS源及其关联的所有数据（内部方法）
     * 
     * @param rssSourceId RSS源ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteRssSource(String rssSourceId) {
        try {
            // 1. 检查RSS源是否存在
            Optional<RssSource> rssSourceOpt = rssSourceRepository.findById(rssSourceId);
            if (rssSourceOpt.isEmpty()) {
                log.warn("尝试删除不存在的RSS源: {}", rssSourceId);
                return false;
            }
            
            RssSource rssSource = rssSourceOpt.get();
            log.info("开始删除RSS源: {} ({})", rssSource.getName(), rssSourceId);
            
            // 2. 查找该RSS源下的所有文章元数据
            List<ArticleMetadata> articles = articleMetadataRepository.findByRssSourceId(rssSourceId);
            log.info("找到 {} 篇文章需要删除", articles.size());
            
            if (!articles.isEmpty()) {
                List<String> articleIds = articles.stream()
                    .map(ArticleMetadata::getId)
                    .collect(Collectors.toList());
                
                // 3. 删除用户文章交互记录（收藏、已读等）
                try {
                    log.info("开始删除用户文章交互记录...");
                    userArticleInteractionRepository.deleteByArticleMetadataIdIn(articleIds);
                    log.info("已删除用户文章交互记录");
                } catch (Exception e) {
                    log.error("删除用户文章交互记录时出现错误: {}", e.getMessage(), e);
                    throw new RuntimeException("删除用户文章交互记录失败: " + e.getMessage(), e);
                }
                
                // 4. 删除文章标签关联记录
                try {
                    log.info("开始删除文章标签关联记录...");
                    articleTagRepository.deleteByArticleMetadataIdIn(articleIds);
                    log.info("已删除文章标签关联记录");
                } catch (Exception e) {
                    log.error("删除文章标签关联记录时出现错误: {}", e.getMessage(), e);
                    throw new RuntimeException("删除文章标签关联记录失败: " + e.getMessage(), e);
                }
                
                // 5. 删除关联到文章的笔记
                try {
                    log.info("开始删除关联到文章的笔记...");
                    for (ArticleMetadata article : articles) {
                        List<Note> notes = noteRepository.findByArticleMetadata(article);
                        if (!notes.isEmpty()) {
                            noteRepository.deleteAll(notes);
                            log.info("已删除文章 {} 的 {} 条笔记", article.getId(), notes.size());
                        }
                    }
                    log.info("已删除所有关联笔记");
                } catch (Exception e) {
                    log.error("删除关联笔记时出现错误: {}", e.getMessage(), e);
                    throw new RuntimeException("删除关联笔记失败: " + e.getMessage(), e);
                }
                
                // 6. 删除MongoDB中的文章内容
                try {
                    log.info("开始删除MongoDB中的文章内容...");
                    articleContentRepository.deleteByMysqlMetadataIdIn(articleIds);
                    log.info("已删除MongoDB中的 {} 篇文章内容", articleIds.size());
                } catch (Exception e) {
                    log.warn("删除MongoDB文章内容时出现错误: {}", e.getMessage());
                    // 继续执行，不因为MongoDB删除失败而中断整个流程
                }
                
                // 7. 删除MySQL中的文章元数据
                try {
                    log.info("开始删除MySQL中的文章元数据...");
                    articleMetadataRepository.deleteAll(articles);
                    log.info("已删除MySQL中的 {} 篇文章元数据", articles.size());
                } catch (Exception e) {
                    log.error("删除MySQL文章元数据时出现错误: {}", e.getMessage(), e);
                    throw new RuntimeException("删除文章元数据失败: " + e.getMessage(), e);
                }
            }
            
            // 8. 最后删除RSS源
            try {
                log.info("开始删除RSS源...");
                rssSourceRepository.delete(rssSource);
                log.info("已删除RSS源: {} ({})", rssSource.getName(), rssSourceId);
            } catch (Exception e) {
                log.error("删除RSS源时出现错误: {}", e.getMessage(), e);
                throw new RuntimeException("删除RSS源失败: " + e.getMessage(), e);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("删除RSS源时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("删除RSS源失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取RSS源的文章列表
     * 
     * @param sourceId RSS源ID
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID，用于获取已读状态
     * @return 分页的文章列表
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDTO> getArticlesByRssSource(String sourceId, int page, int size, String userId) {
        // 检查RSS源是否存在
        if (!rssSourceRepository.existsById(sourceId)) {
            throw new RuntimeException("RSS源不存在");
        }
        
        // 分页查询文章
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publicationDate"));
        Page<ArticleMetadata> articles = articleMetadataRepository.findByRssSourceId(
                sourceId, pageable);
        
        // 转换为DTO并保持分页信息，包含用户已读状态
        List<ArticleDTO> articleDTOs = articles.getContent().stream()
                .map(article -> convertToArticleDto(article, userId))
                .collect(Collectors.toList());
        
        // 创建包含分页信息的Page对象
        return new PageImpl<>(articleDTOs, pageable, articles.getTotalElements());
    }
    
    /**
     * 获取用户订阅的所有RSS源的最新文章
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<ArticleDTO> getLatestArticlesForUser(String userId, int page, int size) {
        // 获取用户订阅的RSS源ID列表
        List<String> sourceIds = getUserRssSources(userId).stream()
                .map(dto -> dto.getId())
                .collect(Collectors.toList());
        
        if (sourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 分页查询文章
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publicationDate"));
        Page<ArticleMetadata> articles = articleMetadataRepository.findByRssSourceIdIn(sourceIds, pageable);
        
        // 转换为DTO，包含用户已读状态
        return articles.getContent().stream()
                .map(article -> convertToArticleDto(article, userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 手动触发RSS源的抓取
     * 
     * @param sourceId RSS源ID
     * @return 抓取的文章数量
     */
    @Override
    @Transactional
    public int fetchRssSource(String sourceId) {
        RssSource rssSource = rssSourceRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("RSS源不存在"));
        
        List<ArticleMetadata> fetchedArticles = articleFetchService.fetchArticlesFromSource(rssSource);
        
        // 更新最后抓取时间
        rssSource.setLastFetchedAt(LocalDateTime.now());
        rssSourceRepository.save(rssSource);
        
        return fetchedArticles.size();
    }
    
    /**
     * 检查RSS源URL是否有效
     * 
     * @param url RSS源URL
     * @return 是否有效
     */
    @Override
    public boolean validateRssUrl(String url) {
        try {
            log.info("开始验证RSS源URL: {}", url);
            URL feedUrl = new URL(url);
            
            // 设置超时时间
            SyndFeedInput input = new SyndFeedInput();
            XmlReader reader = new XmlReader(feedUrl);
            
            // 尝试解析Feed
            SyndFeed feed = input.build(reader);
            
            // 判断Feed是否有效
            boolean isValid = feed != null && feed.getEntries() != null;
            
            if (isValid) {
                log.info("RSS源URL验证成功: {}, 条目数: {}", url, feed.getEntries().size());
                return true;
            } else {
                log.warn("RSS源URL验证失败: {} - Feed为空或没有条目", url);
                return false;
            }
            
        } catch (Exception e) {
            // RSSHub源可能需要一些特殊处理，暂时放宽验证
            if (url.contains("localhost:1200") || url.contains("rsshub.app")) {
                log.info("检测到RSSHub源，跳过严格验证: {}, 错误: {}", url, e.getMessage());
                return true;
            }
            
            log.warn("RSS URL验证失败: {}, 错误: {}", url, e.getMessage());
            return false;
        }
    }
    
    /**
     * 将实体转换为DTO
     * 
     * @param rssSource RSS源实体
     * @return RSS源DTO
     */
    @Override
    public RssSourceDTO convertToDto(RssSource rssSource) {
        RssSourceDTO dto = new RssSourceDTO();
        dto.setId(rssSource.getId().toString());
        dto.setName(rssSource.getName());
        dto.setUrl(rssSource.getUrl());
        dto.setDescription(rssSource.getDescription());
        dto.setCategory(rssSource.getCategory());
        dto.setPublic(rssSource.isPublic());
        dto.setActive(rssSource.isActive());
        dto.setUserId(rssSource.getUser().getId().toString());
        dto.setCreatedAt(rssSource.getCreatedAt());
        dto.setUpdatedAt(rssSource.getUpdatedAt());
        dto.setLastFetchedAt(rssSource.getLastFetchedAt());
        
        // 设置RSSHub特定字段
        dto.setRsshub(rssSource.getIsRsshub() != null && rssSource.getIsRsshub());
        dto.setRsshubRoute(rssSource.getRsshubRoute());
        dto.setRsshubInstance(rssSource.getRsshubInstance());
        
        return dto;
    }
    
    /**
     * 将DTO转换为实体
     * 
     * @param rssSourceDTO RSS源DTO
     * @return RSS源实体
     */
    @Override
    public RssSource convertToEntity(RssSourceDTO rssSourceDTO) {
        RssSource entity = new RssSource();
        
        // 如果有ID，则设置ID
        if (rssSourceDTO.getId() != null && !rssSourceDTO.getId().isEmpty()) {
            entity.setId(rssSourceDTO.getId());
        }
        
        entity.setName(rssSourceDTO.getName());
        entity.setUrl(rssSourceDTO.getUrl());
        entity.setDescription(rssSourceDTO.getDescription());
        entity.setCategory(rssSourceDTO.getCategory());
        entity.setPublic(rssSourceDTO.isPublic());
        
        // 设置RSSHub特定字段
        entity.setIsRsshub(rssSourceDTO.isRsshub());
        entity.setRsshubRoute(rssSourceDTO.getRsshubRoute());
        entity.setRsshubInstance(rssSourceDTO.getRsshubInstance());
        
        // 设置时间字段
        entity.setCreatedAt(rssSourceDTO.getCreatedAt() != null ? 
                rssSourceDTO.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(rssSourceDTO.getUpdatedAt() != null ? 
                rssSourceDTO.getUpdatedAt() : LocalDateTime.now());
        entity.setLastFetchedAt(rssSourceDTO.getLastFetchedAt());
        
        return entity;
    }
    
    /**
     * 将文章实体转换为DTO
     * 
     * @param articleMetadata 文章元数据实体
     * @return 文章DTO
     */
    private ArticleDTO convertToArticleDto(ArticleMetadata articleMetadata) {
        return convertToArticleDto(articleMetadata, null);
    }
    
    /**
     * 将文章实体转换为DTO，包含用户已读状态
     * 
     * @param articleMetadata 文章元数据实体
     * @param userId 用户ID，如果为null则不查询已读状态
     * @return 文章DTO
     */
    private ArticleDTO convertToArticleDto(ArticleMetadata articleMetadata, String userId) {
        ArticleDTO dto = new ArticleDTO();
        dto.setId(articleMetadata.getId());
        dto.setTitle(articleMetadata.getTitle());
        dto.setAuthor(articleMetadata.getAuthor());
        dto.setSummary(articleMetadata.getSummaryText());
        dto.setOriginalUrl(articleMetadata.getLinkToOriginal());
        dto.setImageUrl(null); // ArticleMetadata 类中没有 imageUrl 字段
        dto.setPublicationDate(articleMetadata.getPublicationDate());
        dto.setRssSourceId(articleMetadata.getRssSource().getId().toString());
        dto.setRssSourceName(articleMetadata.getRssSource().getName());
        
        // 如果提供了用户ID，查询已读状态和收藏状态
        if (userId != null) {
            try {
                boolean isRead = readStatusService.isRead(articleMetadata.getId(), userId);
                dto.setIsRead(isRead);
            } catch (Exception e) {
                log.warn("获取文章已读状态失败: articleId={}, userId={}, error={}", 
                        articleMetadata.getId(), userId, e.getMessage());
                dto.setIsRead(false); // 默认为未读
            }
            
            try {
                boolean isFavorited = favoriteService.isFavorite(articleMetadata.getId(), userId);
                dto.setIsFavorited(isFavorited);
            } catch (Exception e) {
                log.warn("获取文章收藏状态失败: articleId={}, userId={}, error={}", 
                        articleMetadata.getId(), userId, e.getMessage());
                dto.setIsFavorited(false); // 默认为未收藏
            }
        } else {
            dto.setIsRead(false); // 默认为未读
            dto.setIsFavorited(false); // 默认为未收藏
        }
        
        return dto;
    }

    /**
     * 异步执行RSS抓取
     * @param sourceId RSS源ID
     */
    @Override
    @Async("rssFetchExecutor")
    public void scheduleRssFetch(String sourceId) {
        log.info("异步任务：开始抓取RSS源，ID: {}", sourceId);
        try {
            Optional<RssSource> sourceOpt = rssSourceRepository.findById(sourceId);
            if (sourceOpt.isPresent()) {
                articleFetchService.fetchArticlesFromSource(sourceOpt.get());
            } else {
                log.error("RSS源不存在，ID: {}", sourceId);
            }
        } catch (Exception e) {
            log.error("RSS抓取任务执行异常，源ID: " + sourceId, e);
        }
    }
} 