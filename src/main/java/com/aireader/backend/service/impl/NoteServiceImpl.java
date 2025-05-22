package com.aireader.backend.service.impl;

import com.aireader.backend.dto.NoteRequestDto;
import com.aireader.backend.dto.NoteResponseDto;
import com.aireader.backend.dto.TagDto;
import com.aireader.backend.model.jpa.ArticleMetadata;
import com.aireader.backend.model.jpa.Note;
import com.aireader.backend.model.jpa.Tag;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.repository.ArticleMetadataRepository;
import com.aireader.backend.repository.NoteRepository;
import com.aireader.backend.repository.TagRepository;
import com.aireader.backend.repository.UserRepository;
import com.aireader.backend.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 笔记服务实现类
 */
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ArticleMetadataRepository articleMetadataRepository;

    /**
     * 创建笔记
     * @param noteRequest 笔记请求
     * @param userId 用户ID
     * @return 创建的笔记
     */
    @Override
    @Transactional
    public NoteResponseDto createNote(NoteRequestDto noteRequest, String userId) {
        User user = getUserById(userId);
        
        // 构建Note对象
        Note note = Note.builder()
                .title(noteRequest.getTitle())
                .contentRichText(noteRequest.getContent())
                .user(user)
                .aiProcessingStatus("PENDING")
                .build();
        
        // 如果有关联文章，设置文章元数据
        if (noteRequest.getArticleId() != null && !noteRequest.getArticleId().isEmpty()) {
            ArticleMetadata articleMetadata = articleMetadataRepository.findById(noteRequest.getArticleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到关联的文章"));
            note.setArticleMetadata(articleMetadata);
        }
        
        // 处理标签
        if (noteRequest.getTags() != null && !noteRequest.getTags().isEmpty()) {
            Set<Tag> tags = processTagsForNote(noteRequest.getTags(), user);
            note.setTags(tags);
        }
        
        // 保存笔记
        Note savedNote = noteRepository.save(note);
        
        // 转换为DTO并返回
        return convertToResponseDto(savedNote);
    }

    /**
     * 更新笔记
     * @param noteId 笔记ID
     * @param noteRequest 笔记请求
     * @param userId 用户ID
     * @return 更新后的笔记
     */
    @Override
    @Transactional
    public NoteResponseDto updateNote(String noteId, NoteRequestDto noteRequest, String userId) {
        // 获取用户
        User user = getUserById(userId);
        
        // 获取要更新的笔记
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到笔记或无权操作"));
        
        // 更新笔记内容
        note.setTitle(noteRequest.getTitle());
        note.setContentRichText(noteRequest.getContent());
        
        // 如果文章ID发生变更，更新关联的文章元数据
        if (noteRequest.getArticleId() != null) {
            if (!noteRequest.getArticleId().isEmpty()) {
                ArticleMetadata articleMetadata = articleMetadataRepository.findById(noteRequest.getArticleId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到关联的文章"));
                note.setArticleMetadata(articleMetadata);
            } else {
                note.setArticleMetadata(null); // 移除文章关联
            }
        }
        
        // 如果标签列表存在，更新标签
        if (noteRequest.getTags() != null) {
            Set<Tag> tags = processTagsForNote(noteRequest.getTags(), user);
            note.setTags(tags);
        }
        
        // 保存更新后的笔记
        Note updatedNote = noteRepository.save(note);
        
        // 转换为DTO并返回
        return convertToResponseDto(updatedNote);
    }

    /**
     * 删除笔记
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteNote(String noteId, String userId) {
        // 检查笔记是否存在且属于该用户
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到笔记或无权操作"));
        
        // 删除笔记
        noteRepository.delete(note);
        
        // 返回删除成功
        return true;
    }

    /**
     * 根据ID获取笔记
     * @param noteId 笔记ID
     * @return 笔记
     */
    @Override
    public Optional<NoteResponseDto> getNoteById(String noteId) {
        return noteRepository.findById(noteId)
                .map(this::convertToResponseDto);
    }

    /**
     * 获取用户的所有笔记
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    @Override
    public List<NoteResponseDto> getUserNotes(String userId, int page, int size) {
        User user = getUserById(userId);
        
        // 创建分页请求，按更新时间降序排序
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        
        // 查询用户的笔记
        Page<Note> notesPage = noteRepository.findByUser(user, pageable);
        
        // 转换为DTO列表并返回
        return notesPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取文章的所有笔记
     * @param articleId 文章ID
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    @Override
    public List<NoteResponseDto> getArticleNotes(String articleId, String userId, int page, int size) {
        User user = getUserById(userId);
        
        // 查找文章
        ArticleMetadata articleMetadata = articleMetadataRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到文章"));
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        
        // 使用分页方法查询用户对该文章的笔记
        Page<Note> notesPage = noteRepository.findByUserAndArticleMetadata(user, articleMetadata, pageable);
        
        // 转换为DTO列表并返回
        return notesPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取标签下的所有笔记
     * @param tagId 标签ID
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    @Override
    public List<NoteResponseDto> getNotesByTag(String tagId, String userId, int page, int size) {
        User user = getUserById(userId);
        
        // 查找标签
        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到标签或无权访问"));
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        
        // 使用JPQL查询带有指定标签的笔记
        Page<Note> notesPage = noteRepository.findByUserAndTagName(user, tag.getName(), pageable);
        
        // 转换为DTO列表并返回
        return notesPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 搜索笔记
     * @param keyword 关键词
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 笔记列表
     */
    @Override
    public List<NoteResponseDto> searchNotes(String keyword, String userId, int page, int size) {
        User user = getUserById(userId);
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size);
        
        // 搜索笔记
        Page<Note> notesPage = noteRepository.searchByUserAndKeyword(user, keyword, pageable);
        
        // 转换为DTO列表并返回
        return notesPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的所有标签
     * @param userId 用户ID
     * @return 标签列表
     */
    @Override
    public List<TagDto> getUserTags(String userId) {
        User user = getUserById(userId);
        
        // 查询用户的所有标签
        List<Tag> tags = tagRepository.findByUser(user);
        
        // 转换为DTO列表并返回
        return tags.stream()
                .map(this::convertToTagDto)
                .collect(Collectors.toList());
    }

    /**
     * 创建标签
     * @param tagDto 标签DTO
     * @param userId 用户ID
     * @return 创建的标签
     */
    @Override
    @Transactional
    public TagDto createTag(TagDto tagDto, String userId) {
        User user = getUserById(userId);
        
        // 检查标签名是否已存在
        Optional<Tag> existingTag = tagRepository.findByNameAndUser(tagDto.getName(), user);
        if (existingTag.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "标签已存在");
        }
        
        // 创建新标签
        Tag tag = Tag.builder()
                .name(tagDto.getName())
                .user(user)
                .build();
        
        // 保存标签
        Tag savedTag = tagRepository.save(tag);
        
        // 转换为DTO并返回
        return convertToTagDto(savedTag);
    }

    /**
     * 更新标签
     * @param tagId 标签ID
     * @param tagDto 标签DTO
     * @param userId 用户ID
     * @return 更新后的标签
     */
    @Override
    @Transactional
    public TagDto updateTag(String tagId, TagDto tagDto, String userId) {
        // 检查标签是否存在且属于该用户
        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到标签或无权操作"));
        
        // 检查新名称是否与其他标签冲突
        if (!tag.getName().equals(tagDto.getName())) {
            Optional<Tag> existingTag = tagRepository.findByNameAndUser(tagDto.getName(), tag.getUser());
            if (existingTag.isPresent() && !existingTag.get().getId().equals(tagId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "标签名称已被使用");
            }
        }
        
        // 更新标签名称
        tag.setName(tagDto.getName());
        
        // 保存更新后的标签
        Tag updatedTag = tagRepository.save(tag);
        
        // 转换为DTO并返回
        return convertToTagDto(updatedTag);
    }

    /**
     * 删除标签
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteTag(String tagId, String userId) {
        // 检查标签是否存在且属于该用户
        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到标签或无权操作"));
        
        // 删除标签
        tagRepository.delete(tag);
        
        // 返回删除成功
        return true;
    }

    /**
     * 为笔记添加标签
     * @param noteId 笔记ID
     * @param tagIds 标签ID集合
     * @param userId 用户ID
     * @return 更新后的笔记
     */
    @Override
    @Transactional
    public NoteResponseDto addTagsToNote(String noteId, Set<String> tagIds, String userId) {
        // 检查笔记是否存在且属于该用户
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到笔记或无权操作"));
        
        // 查询所有标签
        List<Tag> tags = tagRepository.findAllById(tagIds);
        
        // 验证所有标签都属于当前用户
        tags.forEach(tag -> {
            if (!tag.getUser().getId().toString().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用此标签");
            }
        });
        
        // 将新标签添加到笔记的标签集合中
        note.getTags().addAll(tags);
        
        // 保存更新后的笔记
        Note updatedNote = noteRepository.save(note);
        
        // 转换为DTO并返回
        return convertToResponseDto(updatedNote);
    }

    /**
     * 从笔记中移除标签
     * @param noteId 笔记ID
     * @param tagIds 标签ID集合
     * @param userId 用户ID
     * @return 更新后的笔记
     */
    @Override
    @Transactional
    public NoteResponseDto removeTagsFromNote(String noteId, Set<String> tagIds, String userId) {
        // 检查笔记是否存在且属于该用户
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到笔记或无权操作"));
        
        // 从笔记的标签集合中移除指定标签
        note.setTags(note.getTags().stream()
                .filter(tag -> !tagIds.contains(tag.getId()))
                .collect(Collectors.toSet()));
        
        // 保存更新后的笔记
        Note updatedNote = noteRepository.save(note);
        
        // 转换为DTO并返回
        return convertToResponseDto(updatedNote);
    }

    /**
     * 处理笔记的标签
     * @param tagNames 标签名称集合
     * @param user 用户
     * @return 标签集合
     */
    private Set<Tag> processTagsForNote(Set<String> tagNames, User user) {
        Set<Tag> tags = new HashSet<>();
        
        for (String tagName : tagNames) {
            // 查找标签是否存在
            Optional<Tag> existingTag = tagRepository.findByNameAndUser(tagName, user);
            
            if (existingTag.isPresent()) {
                // 如果标签已存在，直接使用
                tags.add(existingTag.get());
            } else {
                // 如果标签不存在，创建新标签
                Tag newTag = Tag.builder()
                        .name(tagName)
                        .user(user)
                        .build();
                tags.add(tagRepository.save(newTag));
            }
        }
        
        return tags;
    }

    /**
     * 将Note实体转换为NoteResponseDto
     * @param note Note实体
     * @return NoteResponseDto
     */
    private NoteResponseDto convertToResponseDto(Note note) {
        NoteResponseDto dto = new NoteResponseDto();
        dto.setId(note.getId());
        dto.setTitle(note.getTitle());
        dto.setContent(note.getContentRichText());
        dto.setUserId(note.getUser().getId().toString());
        dto.setUsername(note.getUser().getUsername());
        
        if (note.getArticleMetadata() != null) {
            dto.setArticleId(note.getArticleMetadata().getId());
            dto.setArticleTitle(note.getArticleMetadata().getTitle());
        }
        
        // 提取标签名称
        dto.setTags(note.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet()));
        
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());
        
        return dto;
    }

    /**
     * 将Tag实体转换为TagDto
     * @param tag Tag实体
     * @return TagDto
     */
    private TagDto convertToTagDto(Tag tag) {
        return TagDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .userId(tag.getUser().getId().toString())
                .build();
    }

    /**
     * 根据ID获取用户
     * @param userId 用户ID
     * @return 用户实体
     */
    private User getUserById(String userId) {
        try {
            UUID uuid = UUID.fromString(userId);
            return userRepository.findById(uuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到用户"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户ID格式无效");
        }
    }
} 