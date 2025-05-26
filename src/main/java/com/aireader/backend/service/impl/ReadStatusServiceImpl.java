package com.aireader.backend.service.impl;

import com.aireader.backend.dto.ArticleDTO;
import com.aireader.backend.exception.NotFoundException;
import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.model.jpa.UserArticleInteraction;
import com.aireader.backend.model.mongo.ArticleContent;
import com.aireader.backend.repository.mongo.ArticleContentRepository;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.UserArticleInteractionRepository;
import com.aireader.backend.repository.UserRepository;
import com.aireader.backend.service.ReadStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 阅读状态管理服务实现类
 */
@Service
public class ReadStatusServiceImpl implements ReadStatusService {

    @Autowired
    private UserArticleInteractionRepository userArticleInteractionRepository;

    @Autowired
    private ArticleMetadataRepository articleMetadataRepository;

    @Autowired
    private ArticleContentRepository articleContentRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public void markAsRead(String articleId, String userId) {
        // 获取文章和用户
        ArticleMetadata article = articleMetadataRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("文章不存在: " + articleId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        // 查找现有的交互记录
        Optional<UserArticleInteraction> existingInteraction = userArticleInteractionRepository
                .findByUserAndArticleMetadata(user, article);

        if (existingInteraction.isPresent()) {
            UserArticleInteraction interaction = existingInteraction.get();
            if (!interaction.getIsRead()) {
                interaction.setIsRead(true);
                interaction.setLastReadAt(LocalDateTime.now());
                userArticleInteractionRepository.save(interaction);
            }
        } else {
            // 创建新的交互记录
            UserArticleInteraction.UserArticleInteractionId id = 
                new UserArticleInteraction.UserArticleInteractionId(userId, articleId);
            
            UserArticleInteraction newInteraction = UserArticleInteraction.builder()
                    .id(id)
                    .user(user)
                    .articleMetadata(article)
                    .isRead(true)
                    .isFavorite(false)
                    .lastReadAt(LocalDateTime.now())
                    .build();
            
            userArticleInteractionRepository.save(newInteraction);
        }
    }

    @Override
    @Transactional
    public void markAsUnread(String articleId, String userId) {
        ArticleMetadata article = articleMetadataRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("文章不存在: " + articleId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        Optional<UserArticleInteraction> existingInteraction = userArticleInteractionRepository
                .findByUserAndArticleMetadata(user, article);

        if (existingInteraction.isPresent()) {
            UserArticleInteraction interaction = existingInteraction.get();
            if (interaction.getIsRead()) {
                interaction.setIsRead(false);
                interaction.setLastReadAt(null);
                userArticleInteractionRepository.save(interaction);
            }
        }
        // 如果记录不存在，不需要做任何操作（默认就是未读状态）
    }

    @Override
    @Transactional
    public void markMultipleAsRead(List<String> articleIds, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        for (String articleId : articleIds) {
            try {
                markAsRead(articleId, userId);
            } catch (NotFoundException e) {
                // 忽略不存在的文章，继续处理其他文章
                continue;
            }
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        // 获取用户订阅的所有RSS源的文章
        List<ArticleMetadata> articles = articleMetadataRepository.findByRssSourceUserId(userId);
        
        for (ArticleMetadata article : articles) {
            markAsRead(article.getId(), userId);
        }
    }

    @Override
    public boolean isRead(String articleId, String userId) {
        ArticleMetadata article = articleMetadataRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("文章不存在: " + articleId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        Optional<UserArticleInteraction> interaction = userArticleInteractionRepository
                .findByUserAndArticleMetadata(user, article);

        return interaction.isPresent() && interaction.get().getIsRead();
    }

    @Override
    public Page<ArticleDTO> getReadArticles(String userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastReadAt"));
        Page<UserArticleInteraction> interactions = userArticleInteractionRepository
                .findByUserAndIsReadTrue(user, pageRequest);

        return convertInteractionsToArticleDTOs(interactions, pageRequest);
    }

    @Override
    public Page<ArticleDTO> getUnreadArticles(String userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        // 获取用户订阅的RSS源的所有文章，排除已读的
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publicationDate"));
        
        // 这里需要一个复杂的查询来获取未读文章
        // 暂时使用简化的实现
        List<ArticleMetadata> allArticles = articleMetadataRepository.findByRssSourceUserId(userId);
        List<ArticleDTO> unreadArticles = new ArrayList<>();
        
        for (ArticleMetadata article : allArticles) {
            if (!isRead(article.getId(), userId)) {
                Optional<ArticleContent> contentOpt = articleContentRepository.findById(article.getMongodbContentId());
                ArticleDTO dto = ArticleDTO.fromEntity(article, contentOpt.orElse(null));
                if (dto != null) {
                    dto.setRead(false);
                    unreadArticles.add(dto);
                }
            }
        }
        
        // 手动分页
        int start = page * size;
        int end = Math.min(start + size, unreadArticles.size());
        List<ArticleDTO> pageContent = start < unreadArticles.size() ? 
            unreadArticles.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageRequest, unreadArticles.size());
    }

    @Override
    public long getReadCount(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        
        return userArticleInteractionRepository.countByUserAndIsReadTrue(user);
    }

    @Override
    public long getUnreadCount(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        
        // 获取用户订阅的所有文章数量
        long totalArticles = articleMetadataRepository.countByRssSourceUserId(userId);
        
        // 减去已读文章数量
        long readCount = getReadCount(userId);
        
        return totalArticles - readCount;
    }

    /**
     * 将交互记录转换为ArticleDTO列表
     */
    private Page<ArticleDTO> convertInteractionsToArticleDTOs(Page<UserArticleInteraction> interactions, PageRequest pageRequest) {
        List<ArticleDTO> articleDTOs = new ArrayList<>();
        
        for (UserArticleInteraction interaction : interactions.getContent()) {
            ArticleMetadata metadata = interaction.getArticleMetadata();
            Optional<ArticleContent> contentOpt = articleContentRepository.findById(metadata.getMongodbContentId());

            ArticleDTO dto = ArticleDTO.fromEntity(metadata, contentOpt.orElse(null));
            if (dto != null) {
                dto.setRead(interaction.getIsRead());
                dto.setLastReadAt(interaction.getLastReadAt());
                dto.setFavorited(interaction.getIsFavorite());
                dto.setFavoritedAt(interaction.getFavoritedAt());
                articleDTOs.add(dto);
            }
        }

        return new PageImpl<>(articleDTOs, pageRequest, interactions.getTotalElements());
    }
} 