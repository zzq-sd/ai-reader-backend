package com.aireader.backend.config;

import com.aireader.backend.security.JwtAuthenticationFilter;
import com.aireader.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 使用allowedOriginPatterns替代allowedOrigins
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 禁用CSRF，因为使用JWT进行认证
            .csrf(csrf -> csrf.disable())
            // 无状态会话
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 授权请求
            .authorizeHttpRequests(authorize -> authorize
                // 公开访问的API端点
                .requestMatchers("/auth/login", "/auth/register", "/auth/refresh", "/auth/check-username", "/auth/check-email").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/error").permitAll()
                
                // 用户管理相关API
                .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                
                // RSS源相关API
                .requestMatchers(HttpMethod.GET, "/api/rss/**").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/rss").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/rss/**").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/rss/**").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers("/api/admin/rss/**").hasRole("ADMIN")
                
                // 文章相关API - 部分公开访问用于演示
                .requestMatchers(HttpMethod.GET, "/articles/*/content").permitAll() // 允许查看文章内容
                .requestMatchers(HttpMethod.GET, "/articles/*").permitAll() // 允许查看文章详情
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/*/content").permitAll() // 带前缀的API
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/all").permitAll() // 允许获取文章列表
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/*").permitAll() // 带前缀的API
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/**").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/articles/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/articles/**").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/articles/**").hasAnyRole("ADMIN")
                
                // 笔记相关API
                .requestMatchers("/api/notes/**").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers("/api/admin/notes/**").hasRole("ADMIN")
                
                // 收藏相关API
                .requestMatchers("/api/favorites/**").hasAnyRole("USER", "PREMIUM", "ADMIN")
                
                // 知识图谱相关API
                .requestMatchers("/api/knowledge/**").hasAnyRole("PREMIUM", "ADMIN")
                .requestMatchers("/api/admin/knowledge/**").hasRole("ADMIN")
                
                // 所有其他请求需要认证
                .anyRequest().authenticated()
            )
            // 添加JWT过滤器在UsernamePasswordAuthenticationFilter之前
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
} 