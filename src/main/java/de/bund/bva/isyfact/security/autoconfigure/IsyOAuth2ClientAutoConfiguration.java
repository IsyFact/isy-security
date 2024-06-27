package de.bund.bva.isyfact.security.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import de.bund.bva.isyfact.security.oauth2.client.Authentifizierungsmanager;
import de.bund.bva.isyfact.security.oauth2.client.authentication.ClientCredentialsAuthenticationProvider;
import de.bund.bva.isyfact.security.oauth2.client.authentication.ManualClientCredentialsAuthenticationProvider;
import de.bund.bva.isyfact.security.oauth2.client.authentication.PasswordAuthenticationProvider;
import de.bund.bva.isyfact.security.config.IsyOAuth2ClientConfigurationProperties;
import de.bund.bva.isyfact.security.oauth2.client.IsyOAuth2Authentifizierungsmanager;

/**
 * Autoconfiguration for beans related to OAuth 2.0 client authentication.
 * Most of the beans will only be created if clients are configuration in the application properties.
 * If there is no need for {@link Authentifizierungsmanager} it is safe to exclude this autoconfiguration.
 * <p>
 * Since it creates beans that use {@link ClientRegistration}s the library {@code spring-boot-oauth2-client} is required.
 */
@ConditionalOnClass({ EnableWebSecurity.class, ClientRegistration.class })
@AutoConfiguration
@EnableConfigurationProperties
public class IsyOAuth2ClientAutoConfiguration {

    // does not have a dependency on ClientRegistrations and should always be created
    @Bean
    @ConditionalOnMissingBean
    ManualClientCredentialsAuthenticationProvider isyOAuth2ManualClientCredentialsAuthenticationProvider(
            JwtAuthenticationConverter jwtAuthenticationConverter) {
        return new ManualClientCredentialsAuthenticationProvider(jwtAuthenticationConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProviderManager providerManager(List<AuthenticationProvider> authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public Authentifizierungsmanager authentifizierungsmanager(
            ProviderManager providerManager,
            @Nullable ClientRegistrationRepository clientRegistrationRepository) {
        return new IsyOAuth2Authentifizierungsmanager(providerManager, clientRegistrationRepository);
    }

    /**
     * Beans defined in this class are only required if any ClientRegistrations are configured.
     */
    @Configuration
    @Conditional(ClientsConfiguredCondition.class)
    public static class ClientsConfiguredDependentBeans {

        @Bean
        @ConfigurationProperties(prefix = "isy.security.oauth2.client")
        public IsyOAuth2ClientConfigurationProperties isyOAuth2ClientProperties(OAuth2ClientProperties oAuth2ClientProperties) {
            return new IsyOAuth2ClientConfigurationProperties(oAuth2ClientProperties);
        }

        /**
         * Provides an AuthorizedClientManager configured with the OAuth 2.0 Client Credentials flow that can be used to obtain
         * access tokens in the service-tier.
         */
        @Bean("isyAuthorizedClientManager")
        public OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
                ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService authorizedClientService) {
            OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                    .clientCredentials()
                    .build();

            AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                    new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
            authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

            return authorizedClientManager;
        }

        @Bean
        public ClientCredentialsAuthenticationProvider isyOAuth2ClientCredentialsAuthenticationProvider(
                @Qualifier("isyAuthorizedClientManager") OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
                JwtAuthenticationConverter jwtAuthenticationConverter) {
            return new ClientCredentialsAuthenticationProvider(oAuth2AuthorizedClientManager, jwtAuthenticationConverter);
        }

        @Bean
        @Conditional(ClientsConfiguredCondition.class)
        public PasswordAuthenticationProvider passwordAuthenticationProvider(
                ClientRegistrationRepository clientRegistrationRepository, JwtAuthenticationConverter jwtAuthenticationConverter,
                IsyOAuth2ClientConfigurationProperties isyOAuth2ClientConfigurationProperties) {
            return new PasswordAuthenticationProvider(clientRegistrationRepository, jwtAuthenticationConverter,
                    isyOAuth2ClientConfigurationProperties);
        }

    }

}
