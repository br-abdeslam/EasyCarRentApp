package be.condorcet.easycarrent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP Basic security for the course application.
 *
 * <p>Read access to categories, vehicles, customers, rentals and payments
 * requires an authenticated USER or ADMIN. Category, vehicle and customer writes
 * require ADMIN. Rental booking, updates and lifecycle transitions are allowed
 * for USER or ADMIN, while deleting a rental requires ADMIN. Payment creation and
 * the normal lifecycle (pay/fail/retry) are allowed for USER or ADMIN, while
 * refunding and deleting a payment require ADMIN. Maintenance records may be read
 * by USER or ADMIN, while creating, starting, completing and deleting a
 * maintenance record require ADMIN. {@code /api/ping} stays public.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/ping").permitAll()
                        // Rentals: any authenticated user may read, book, update and run
                        // lifecycle transitions; only ADMIN may delete a rental.
                        // GET /api/vehicles/available is a read under /api/vehicles/** and is
                        // already granted to USER and ADMIN by the vehicle GET rule below.
                        .requestMatchers(HttpMethod.GET, "/api/rentals/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/rentals/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/rentals/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/rentals/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/rentals/**").hasRole("ADMIN")
                        // Payments: any authenticated user may read, create and run the normal
                        // lifecycle (pay/fail/retry); refunding and deleting a payment require
                        // ADMIN. The ADMIN-only refund matcher is declared before the general
                        // PATCH rule so it is not shadowed by it.
                        .requestMatchers(HttpMethod.GET, "/api/payments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/payments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/payments/*/refund").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/payments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/payments/**").hasRole("ADMIN")
                        // Maintenance records: any authenticated user may read; creating,
                        // starting, completing and deleting a maintenance record all require
                        // ADMIN. Maintenance management is an administrative operation.
                        .requestMatchers(HttpMethod.GET, "/api/maintenance-records/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/maintenance-records/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/maintenance-records/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/maintenance-records/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/vehicles/**", "/api/customers/**")
                        .hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/vehicles/**", "/api/customers/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/vehicles/**", "/api/customers/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/vehicles/**", "/api/customers/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> {})
                .build();
    }

    /**
     * Development-only in-memory accounts. These credentials are for local and
     * course use only and must never be used in production.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername("user")
                .password(passwordEncoder.encode("user123"))
                .roles("USER")
                .build();
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
