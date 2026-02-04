package com.ampznetwork.herobrine.web;

import com.ampznetwork.herobrine.config.model.Config;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;

@Log
@Configuration
public class SecurityConfig {
    @Bean
    @ConditionalOnExpression("#{!(systemEnvironment['DEBUG']?:'false').equals('true')}")
    public @Nullable ClientRegistrationRepository clientRegistrationRepository(@Autowired Config config) {
        var registrations = config.getOAuth2().stream()
                .filter(oAuth -> !oAuth.getName().isBlank())
                .map(info -> ClientRegistration.withRegistrationId(info.getName())
                        .clientId(info.getClientId())
                        .clientSecret(info.getSecret())
                        .scope(info.getScope())
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri(info.getRedirectUrl())
                        .authorizationUri(info.getAuthorizationUrl())
                        .tokenUri(info.getTokenUrl())
                        .userInfoUri(info.getUserInfoUrl())
                        .userNameAttributeName(info.getUserNameAttributeName())
                        .build())
                .toArray(ClientRegistration[]::new);
        return registrations.length == 0 ? null : new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    //@ConditionalOnMissingBean(type = "org.springframework.boot.test.mock.mockito.MockitoPostProcessor")
    public SecurityFilterChain configureSecure(HttpSecurity http) throws Exception {
        log.info("Using OAuth2-based SecurityFilterChain");
        return http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/haste/**").permitAll()
                        //.anyRequest().authenticated()
                )
                //.oauth2Login(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable).build();
    }

    @Bean
    @Order
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    public SecurityFilterChain configureInsecure(HttpSecurity http) throws Exception {
        log.warning("Using insecure SecurityFilterChain; consider configuring OAuth2 providers!");
        return http.authorizeHttpRequests(auth -> auth.requestMatchers("/haste/**")
                .permitAll()
                .anyRequest()
                .permitAll()).httpBasic(Customizer.withDefaults()).userDetailsService(username -> new UserDetails() {
            // token for dev: ZGV2Og==

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public String getPassword() {
                return "{noop}";
            }

            @Override
            public String getUsername() {
                return username;
            }
        }).csrf(AbstractHttpConfigurer::disable).build();
    }
}
