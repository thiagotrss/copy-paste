package com.rready.copypaste.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health", "/error").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login {
                it.defaultSuccessUrl("/", true)
            }
            .logout {
                it.logoutUrl("/logout")
                it.logoutSuccessUrl("/")
                it.invalidateHttpSession(true)
                it.deleteCookies("JSESSIONID")
            }
        return http.build()
    }
}
