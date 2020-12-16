package de.inw.serpent.serpback.user.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler

@Configuration
@EnableWebSecurity
@Order(50)
class UserWebSecurityConfig(
    private val userDetailsService: UserDetailsService,
    private val authProvider: DaoAuthenticationProvider
) : WebSecurityConfigurerAdapter() {
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
         http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers(
                "/api/user/login",
                "/api/user/logout",
                "/api/user/register",
                "/api/user/register/**",
                "/api/user/reset/**"
            )
            .permitAll()
            .anyRequest().authenticated()
            .and()
            .logout()
            .logoutUrl("/api/user/logout")
            .logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler (HttpStatus.OK))
            .and()
            .httpBasic().disable()
    }

    @Autowired
    fun initialize(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userDetailsService)
        auth.authenticationProvider(authProvider)
    }
}
