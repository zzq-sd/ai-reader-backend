package com.aireader.backend.security;

import com.aireader.backend.model.jpa.Note;
import com.aireader.backend.model.jpa.RssSource;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.repository.NoteRepository;
import com.aireader.backend.repository.RssSourceRepository;
import com.aireader.backend.repository.UserArticleInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 自定义权限评估器
 * 用于处理更复杂的权限检查，例如资源所有权验证
 */
@Component
public class PermissionEvaluator {

    private final NoteRepository noteRepository;
    private final RssSourceRepository rssSourceRepository;
    private final UserArticleInteractionRepository userArticleInteractionRepository;
    private final AuthenticationTrustResolver trustResolver;

    @Autowired
    public PermissionEvaluator(NoteRepository noteRepository, 
                              RssSourceRepository rssSourceRepository,
                              UserArticleInteractionRepository userArticleInteractionRepository) {
        this.noteRepository = noteRepository;
        this.rssSourceRepository = rssSourceRepository;
        this.userArticleInteractionRepository = userArticleInteractionRepository;
        this.trustResolver = new AuthenticationTrustResolverImpl();
    }

    /**
     * 检查当前用户是否有权限访问指定的笔记
     *
     * @param authentication 当前认证
     * @param noteId 笔记ID
     * @return 是否有权限
     */
    public boolean canAccessNote(Authentication authentication, String noteId) {
        // 如果是管理员，直接允许访问
        if (hasAdminRole(authentication)) {
            return true;
        }

        try {
            String userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return false;
            }

            // 获取笔记信息
            Optional<Note> noteOpt = noteRepository.findById(noteId);
            if (!noteOpt.isPresent()) {
                return false; // 笔记不存在
            }

            // 检查笔记是否属于当前用户
            Note note = noteOpt.get();
            return note.getUser() != null && note.getUser().getId().equals(userId);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查当前用户是否有权限访问指定的RSS源
     *
     * @param authentication 当前认证
     * @param sourceId RSS源ID
     * @return 是否有权限
     */
    public boolean canAccessRssSource(Authentication authentication, String sourceId) {
        // 如果是管理员，直接允许访问
        if (hasAdminRole(authentication)) {
            return true;
        }

        try {
            String userId = getUserIdFromAuthentication(authentication);

            // 获取RSS源信息
            Optional<RssSource> sourceOpt = rssSourceRepository.findById(sourceId);
            if (!sourceOpt.isPresent()) {
                return false; // RSS源不存在
            }

            // 检查RSS源是否是公共的或属于当前用户
            RssSource rssSource = sourceOpt.get();
            boolean isOwner = rssSource.getUser() != null && rssSource.getUser().getId().equals(userId);
            return rssSource.isPublic() || (userId != null && isOwner);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查当前用户是否有权限修改指定的RSS源
     *
     * @param authentication 当前认证
     * @param sourceId RSS源ID
     * @return 是否有权限
     */
    public boolean canModifyRssSource(Authentication authentication, String sourceId) {
        // 如果是管理员，直接允许访问
        if (hasAdminRole(authentication)) {
            return true;
        }

        try {
            String userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return false;
            }

            // 获取RSS源信息
            Optional<RssSource> sourceOpt = rssSourceRepository.findById(sourceId);
            if (!sourceOpt.isPresent()) {
                return false; // RSS源不存在
            }

            // 检查RSS源是否属于当前用户
            RssSource rssSource = sourceOpt.get();
            return rssSource.getUser() != null && rssSource.getUser().getId().equals(userId);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从认证对象中获取用户ID
     * 
     * @param authentication 认证对象
     * @return 用户ID (String), or null if not authenticated or principal is not User
     */
    private String getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || 
                trustResolver.isAnonymous(authentication)) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }

        return null;
    }

    /**
     * 检查是否有管理员权限
     *
     * @param authentication 认证对象
     * @return 是否是管理员
     */
    private boolean hasAdminRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || 
                trustResolver.isAnonymous(authentication)) {
            return false;
        }
        
        return AuthorityUtils.authorityListToSet(authentication.getAuthorities())
                .contains("ROLE_ADMIN");
    }
} 