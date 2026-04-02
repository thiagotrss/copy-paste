package com.rready.copypaste.config

import com.rready.copypaste.service.UserPreferencesService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import java.util.Locale

@Configuration
class WebConfig(private val userPreferencesService: UserPreferencesService) : WebMvcConfigurer {

    @Bean
    fun localeResolver(): LocaleResolver {
        val resolver = CookieLocaleResolver("lang")
        resolver.setDefaultLocale(Locale.ENGLISH)
        return resolver
    }

    @Bean
    fun localeChangeInterceptor(): LocaleChangeInterceptor {
        val interceptor = LocaleChangeInterceptor()
        interceptor.paramName = "lang"
        return interceptor
    }

    @Bean
    fun localePreferenceInterceptor(): LocalePreferenceInterceptor =
        LocalePreferenceInterceptor(userPreferencesService)

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
        registry.addInterceptor(localePreferenceInterceptor())
    }
}
