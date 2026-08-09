package com.backend.userservice.auth.config;

import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.commondataaccess.security.JwtAuthenticationFilter;
import com.backend.commondataaccess.security.jwt.JwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 역할: UserService의 보안 정책(허용 URL, role 매칭, 세션 정책)과 공통 JWT 필터를 필터체인에 연결하는 구성 클래스. <p> 책임 <p> - JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 추가 <p> - endpoint별 접근 제어 <p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 1. HTTP 보안 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 람다식 사용
                .headers(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(configurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/heartbeat",
                                         "/user/v1/admins",
                                         "/auth/v1/auth/login",
                                         "/auth/v1/oauth/google/callback",
                                         "/users/swagger-ui/**",
                                         "/users/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/v1/users").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(UserType.ADMIN.name()) // 운영진
                        .requestMatchers("/user/v1/**").hasAnyRole(UserType.ADMIN.name(), UserType.USER.name()) // 인증된 사용자만 사용가능
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 필터 위치 지정
                .addFilterBefore(new JwtAuthenticationFilter(jwtAuthenticationConverter),
                                 UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 2. WebSecurity 설정 (정적 리소스 제외 등)
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                           .requestMatchers("/swagger-resources/**", "/webjars/**", "/static/**", "/templates/**", "/h2-console/**");
    }

    // 3. CORS 설정
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
