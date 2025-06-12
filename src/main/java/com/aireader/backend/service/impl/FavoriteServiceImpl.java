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
import com.aireader.backend.service.FavoriteService;
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
import java.util.stream.Collectors;

/**
 * 收藏管理服务实现类
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

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
    public void addFavorite(String articleId, String userId) {
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
            if (!interaction.getIsFavorite()) {
                interaction.setIsFavorite(true);
                interaction.setFavoritedAt(LocalDateTime.now());
                userArticleInteractionRepository.save(interaction);
            }
        } else {
            // 创建新的交互记录
            UserArticleInteraction newInteraction = new UserArticleInteraction();
            newInteraction.setUser(user);
            newInteraction.setArticleMetadata(article);
            newInteraction.setIsFavorite(true);
            newInteraction.setFavoritedAt(LocalDateTime.now());
            newInteraction.setIsRead(false);
            userArticleInteractionRepository.save(newInteraction);
        }
    }

    @Override
    @Transactional
    public void removeFavorite(String articleId, String userId) {
        ArticleMetadata article = articleMetadataRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("文章不存在: " + articleId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        Optional<UserArticleInteraction> existingInteraction = userArticleInteractionRepository
                .findByUserAndArticleMetadata(user, article);

        if (existingInteraction.isPresent()) {
            UserArticleInteraction interaction = existingInteraction.get();
            if (interaction.getIsFavorite()) {
                interaction.setIsFavorite(false);
                interaction.setFavoritedAt(null);
                userArticleInteractionRepository.save(interaction);
            }
        }
        // 如果记录不存在，无需操作
    }

    @Override
    public boolean isFavorite(String articleId, String userId) {
        ArticleMetadata article = articleMetadataRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("文章不存在: " + articleId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        Optional<UserArticleInteraction> interaction = userArticleInteractionRepository
                .findByUserAndArticleMetadata(user, article);

        return interaction.isPresent() && interaction.get().getIsFavorite();
    }

    @Override
    public Page<ArticleDTO> getFavorites(String userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "favoritedAt"));
        Page<UserArticleInteraction> interactions = userArticleInteractionRepository
                .findByUserAndIsFavoriteTrue(user, pageRequest);

        // 将交互记录转换为ArticleDTO
        List<ArticleDTO> articleDTOs = new ArrayList<>();
        for (UserArticleInteraction interaction : interactions.getContent()) {
            ArticleMetadata metadata = interaction.getArticleMetadata();
            Optional<ArticleContent> contentOpt = articleContentRepository.findById(metadata.getMongodbContentId());

            ArticleDTO dto = ArticleDTO.fromEntity(metadata, contentOpt.orElse(null));
            if (dto != null) {
                dto.setFavorited(true);
                dto.setFavoritedAt(interaction.getFavoritedAt());
                dto.setRead(interaction.getIsRead());
                dto.setLastReadAt(interaction.getLastReadAt());
                articleDTOs.add(dto);
            }
        }

        return new PageImpl<>(articleDTOs, pageRequest, interactions.getTotalElements());
    }

    @Override
    public List<ArticleDTO> getRecentFavorites(String userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));

        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "favoritedAt"));
        Page<UserArticleInteraction> interactions = userArticleInteractionRepository
                .findByUserAndIsFavoriteTrue(user, pageRequest);

        return interactions.getContent().stream()
                .map(interaction -> {
                    ArticleMetadata metadata = interaction.getArticleMetadata();
                    Optional<ArticleContent> contentOpt = articleContentRepository.findById(metadata.getMongodbContentId());
                    
                    ArticleDTO dto = ArticleDTO.fromEntity(metadata, contentOpt.orElse(null));
                    if (dto != null) {
                        dto.setFavorited(true);
                        dto.setFavoritedAt(interaction.getFavoritedAt());
                        dto.setRead(interaction.getIsRead());
                        dto.setLastReadAt(interaction.getLastReadAt());
                    }
                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ArticleDTO> getFavoritesByTag(String userId, String tagId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        
        Page<ArticleMetadata> articles = articleMetadataRepository
                .findFavoriteArticlesByUserAndTag(userId, tagId, pageRequest);
        
        List<ArticleDTO> dtos = articles.getContent().stream()
                .map(metadata -> {
                    Optional<ArticleContent> contentOpt = articleContentRepository.findById(metadata.getMongodbContentId());
                    ArticleDTO dto = ArticleDTO.fromEntity(metadata, contentOpt.orElse(null));
                    
                    // 获取交互信息
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
                    Optional<UserArticleInteraction> interaction = userArticleInteractionRepository
                            .findByUserAndArticleMetadata(user, metadata);
                    
                    if (interaction.isPresent() && dto != null) {
                        dto.setFavorited(interaction.get().getIsFavorite());
                        dto.setFavoritedAt(interaction.get().getFavoritedAt());
                        dto.setRead(interaction.get().getIsRead());
                        dto.setLastReadAt(interaction.get().getLastReadAt());
                    }
                    
                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageRequest, articles.getTotalElements());
    }

    @Override
    public Page<ArticleDTO> searchFavorites(String userId, String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        
        // 转换关键词为模糊匹配格式
        String searchPattern = "%" + keyword.toLowerCase() + "%";
        
        Page<ArticleMetadata> articles = articleMetadataRepository
                .searchFavoriteArticlesByUser(userId, searchPattern, pageRequest);
        
        List<ArticleDTO> dtos = articles.getContent().stream()
                .map(metadata -> {
                    Optional<ArticleContent> contentOpt = articleContentRepository.findById(metadata.getMongodbContentId());
                    ArticleDTO dto = ArticleDTO.fromEntity(metadata, contentOpt.orElse(null));
                    
                    // 获取交互信息
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
                    Optional<UserArticleInteraction> interaction = userArticleInteractionRepository
                            .findByUserAndArticleMetadata(user, metadata);
                    
                    if (interaction.isPresent() && dto != null) {
                        dto.setFavorited(interaction.get().getIsFavorite());
                        dto.setFavoritedAt(interaction.get().getFavoritedAt());
                        dto.setRead(interaction.get().getIsRead());
                        dto.setLastReadAt(interaction.get().getLastReadAt());
                    }
                    
                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageRequest, articles.getTotalElements());
    }

    @Override
    public long getFavoritesCount(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在: " + userId));
        
        return userArticleInteractionRepository.countByUserAndIsFavoriteTrue(user);
    }
} 