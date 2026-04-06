package com.backend.userservice.auth.config;

import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.commondataaccess.security.JwtAuthenticationFilter;
import com.backend.commondataaccess.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * SpringSecurity, 인증 인가 관련 설정 및 관련 Bean 모음
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    // 1. HTTP 보안 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 람다식 사용
                .headers(headers -> headers.disable())
                .cors(cors -> cors.configurationSource(configurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .exceptionHandling(exception -> exception
//                        .accessDeniedHandler(accessDeniedHandler)
//                        .authenticationEntryPoint(unauthorizedHandler)
//                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/heartbeat", "/user/v1/users/**", "/auth/v1/auth/login", "/users/swagger-ui/**", "/users/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(UserType.ADMIN.name()) // 운영진
                        .requestMatchers("/user/v1/**").hasRole(UserType.USER.name())    // 일반 회원
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 필터 위치 지정
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                                 UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 2. WebSecurity 설정 (정적 리소스 제외 등)
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                           .requestMatchers("/swagger-resources/**", "/webjars/**", "/static/**", "/templates/**", "/h2-console/**");
    }

    // 3. AuthenticationManager 설정 (Provider 등록)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 4. CORS 설정
    @Bean
    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
