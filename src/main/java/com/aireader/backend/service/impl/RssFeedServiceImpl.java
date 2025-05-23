package com.aireader.backend.service.impl;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.dto.RssSourceDTO;
import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.RssSource;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.RssSourceRepository;
import com.aireader.backend.repository.UserRepository;
import com.aireader.backend.service.ArticleFetchService;
import com.aireader.backend.service.RssFeedService;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class RssFeedServiceImpl implements RssFeedService {

    @Autowired
    private RssSourceRepository rssSourceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ArticleMetadataRepository articleMetadataRepository;
    
    @Autowired
    private ArticleFetchService articleFetchService;
    
    @Value("${rss.fetch.timeout}")
    private int fetchTimeout;
    
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
        
        // 查找用户
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查是否已存在相同URL的RSS源
        Optional<RssSource> existingSource = rssSourceRepository.findByUrl(rssSourceDTO.getUrl());
        
        if (existingSource.isPresent()) {
            // 如果已存在，检查是否是公共源或用户自己的源
            RssSource source = existingSource.get();
            if (source.isPublic() || source.getUser().getId().equals(UUID.fromString(userId))) {
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
        
        // 保存RSS源
        RssSource savedSource = rssSourceRepository.save(rssSource);
        
        // 触发文章抓取（异步）
        new Thread(() -> articleFetchService.fetchArticlesFromSource(savedSource)).start();
        
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
        List<RssSource> userSources = rssSourceRepository.findByUserId(UUID.fromString(userId));
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
        return rssSourceRepository.findById(UUID.fromString(sourceId))
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
        RssSource rssSource = rssSourceRepository.findById(UUID.fromString(sourceId))
                .orElseThrow(() -> new RuntimeException("RSS源不存在"));
        
        // 检查权限
        if (!rssSource.getUser().getId().equals(UUID.fromString(userId))) {
            throw new RuntimeException("无权修改此RSS源");
        }
        
        // 如果URL发生变化，需要验证新URL
        if (!rssSource.getUrl().equals(rssSourceDTO.getUrl()) && !validateRssUrl(rssSourceDTO.getUrl())) {
            throw new RuntimeException("无效的RSS源URL");
        }
        
        // 更新RSS源信息
        rssSource.setName(rssSourceDTO.getName());
        rssSource.setUrl(rssSourceDTO.getUrl());
        rssSource.setDescription(rssSourceDTO.getDescription());
        rssSource.setCategory(rssSourceDTO.getCategory());
        rssSource.setPublic(rssSourceDTO.isPublic());
        rssSource.setUpdatedAt(LocalDateTime.now());
        
        // 保存更新后的RSS源
        RssSource updatedSource = rssSourceRepository.save(rssSource);
        
        // 如果URL发生变化，触发文章抓取
        if (!rssSource.getUrl().equals(rssSourceDTO.getUrl())) {
            new Thread(() -> articleFetchService.fetchArticlesFromSource(updatedSource)).start();
        }
        
        return convertToDto(updatedSource);
    }
    
    /**
     * 删除RSS源
     * 
     * @param sourceId RSS源ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteRssSource(String sourceId, String userId) {
        RssSource rssSource = rssSourceRepository.findById(UUID.fromString(sourceId))
                .orElseThrow(() -> new RuntimeException("RSS源不存在"));
        
        // 检查权限
        if (!rssSource.getUser().getId().equals(UUID.fromString(userId))) {
            throw new RuntimeException("无权删除此RSS源");
        }
        
        // 删除RSS源
        rssSourceRepository.delete(rssSource);
        
        return true;
    }
    
    /**
     * 获取RSS源的文章列表
     * 
     * @param sourceId RSS源ID
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<ArticleDTO> getArticlesByRssSource(String sourceId, int page, int size) {
        // 检查RSS源是否存在
        if (!rssSourceRepository.existsById(UUID.fromString(sourceId))) {
            throw new RuntimeException("RSS源不存在");
        }
        
        // 分页查询文章
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<ArticleMetadata> articles = articleMetadataRepository.findByRssSourceId(
                UUID.fromString(sourceId), pageable);
        
        // 转换为DTO
        return articles.getContent().stream()
                .map(this::convertToArticleDto)
                .collect(Collectors.toList());
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
        List<UUID> sourceIds = getUserRssSources(userId).stream()
                .map(dto -> UUID.fromString(dto.getId()))
                .collect(Collectors.toList());
        
        if (sourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 分页查询文章
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<ArticleMetadata> articles = articleMetadataRepository.findByRssSourceIdIn(sourceIds, pageable);
        
        // 转换为DTO
        return articles.getContent().stream()
                .map(this::convertToArticleDto)
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
        RssSource rssSource = rssSourceRepository.findById(UUID.fromString(sourceId))
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
            URL feedUrl = new URL(url);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));
            
            // 如果能成功解析，则认为URL有效
            return feed != null && feed.getEntries() != null;
        } catch (Exception e) {
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
        dto.setUserId(rssSource.getUser().getId().toString());
        dto.setCreatedAt(rssSource.getCreatedAt());
        dto.setUpdatedAt(rssSource.getUpdatedAt());
        dto.setLastFetchedAt(rssSource.getLastFetchedAt());
        
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
            entity.setId(UUID.fromString(rssSourceDTO.getId()));
        }
        
        entity.setName(rssSourceDTO.getName());
        entity.setUrl(rssSourceDTO.getUrl());
        entity.setDescription(rssSourceDTO.getDescription());
        entity.setCategory(rssSourceDTO.getCategory());
        entity.setPublic(rssSourceDTO.isPublic());
        
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
        ArticleDTO dto = new ArticleDTO();
        dto.setId(articleMetadata.getId().toString());
        dto.setTitle(articleMetadata.getTitle());
        dto.setAuthor(articleMetadata.getAuthor());
        dto.setSummary(articleMetadata.getSummary());
        dto.setUrl(articleMetadata.getUrl());
        dto.setImageUrl(articleMetadata.getImageUrl());
        dto.setPublishedAt(articleMetadata.getPublishedAt());
        dto.setRssSourceId(articleMetadata.getRssSource().getId().toString());
        dto.setRssSourceName(articleMetadata.getRssSource().getName());
        
        return dto;
    }
} 