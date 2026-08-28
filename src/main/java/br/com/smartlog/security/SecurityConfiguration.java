package br.com.smartlog.security;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import com.google.firebase.auth.FirebaseAuth;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    FirebaseAuthenticationFilter firebaseAuthenticationFilter(ObjectProvider<FirebaseAuth> auth) {
        return new FirebaseAuthenticationFilter(auth);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            FirebaseAuthenticationFilter firebaseFilter) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Autenticação necessária\",\"timestamp\":\"" + java.time.Instant.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"Acesso não permitido\",\"timestamp\":\"" + java.time.Instant.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}");
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1", "/actuator/health", "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/entregas").hasAnyRole("ADMIN", "MOTORISTA", "CLIENTE")
                        .requestMatchers("/api/v1/entregas/**").hasAnyRole("ADMIN", "MOTORISTA", "CLIENTE")
                        .anyRequest().authenticated())
                .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
