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
            // 授权请求 - 特别注意顺序，更具体的规则应该在前面
            .authorizeHttpRequests(authorize -> authorize
                // 公开访问的API端点
                .requestMatchers("/auth/login", "/auth/register", "/auth/refresh", "/auth/check-username", "/auth/check-email").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/error").permitAll()
                
                // AI测试端点 - 临时开放用于调试
                .requestMatchers("/api/v1/test/ai/**").permitAll()
                .requestMatchers("/test/knowledge/**").permitAll()
                
                // 知识图谱相关API - 完全公开访问用于调试
                .requestMatchers("/api/v1/knowledge/graph-data").permitAll() // 特别放行图谱数据接口
                .requestMatchers("/api/v1/knowledge/test").permitAll() // 放行测试接口
                .requestMatchers("/api/v1/knowledge/test-neo4j").permitAll() // 放行Neo4j连接测试接口
                .requestMatchers("/api/v1/knowledge/health").permitAll() // 放行健康检查接口
                .requestMatchers("/api/v1/knowledge/**").permitAll() // 允许访问知识图谱数据
                .requestMatchers("/api/knowledge/**").permitAll() // 允许访问知识图谱数据（旧版API路径）
                .requestMatchers("/api/admin/knowledge/**").permitAll() // 临时放行管理员API用于调试
                
                // 用户管理相关API
                .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                
                // RSS源相关API
                .requestMatchers(HttpMethod.GET, "/api/rss/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/rss").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/rss/**").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/rss/**").hasAnyRole("USER", "PREMIUM", "ADMIN")
                .requestMatchers("/api/admin/rss/**").permitAll() // 临时放行
                
                // 文章相关API - 部分公开访问用于演示
                .requestMatchers(HttpMethod.GET, "/articles/*/content").permitAll() // 允许查看文章内容
                .requestMatchers(HttpMethod.GET, "/articles/*").permitAll() // 允许查看文章详情
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/*/content").permitAll() // 带前缀的API
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/all").permitAll() // 允许获取文章列表
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/*").permitAll() // 带前缀的API
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/articles/**").permitAll() // 临时放行
                .requestMatchers(HttpMethod.PUT, "/api/v1/articles/**").permitAll() // 临时放行
                .requestMatchers(HttpMethod.DELETE, "/api/v1/articles/**").permitAll() // 临时放行
                
                // 笔记相关API
                .requestMatchers("/api/notes/**").permitAll() // 临时放行
                .requestMatchers("/api/admin/notes/**").permitAll() // 临时放行
                
                // 收藏相关API
                .requestMatchers("/api/favorites/**").permitAll() // 临时放行
                
                // 允许公开访问的路径
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                // 知识图谱API已在上面配置
                .requestMatchers("/api/v1/test/**").permitAll() // 临时放行测试API
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                
                // 所有其他请求需要认证
                .anyRequest().permitAll()  // 临时全部放行以便测试
            )
            // 添加JWT过滤器在UsernamePasswordAuthenticationFilter之前
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
} 