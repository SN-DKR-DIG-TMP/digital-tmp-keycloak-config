package net.atos.digital.keycloak.configurations;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.client.ClientBuilder;
import net.atos.digital.keycloak.properties.KeycloakProperties;
import net.atos.digital.keycloak.properties.UserManagerConfiguration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@FieldDefaults(level =  AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class KeycloakConfiguration {

    public static final String KEYCLOAK_BEAN_NAME = "keycloakAdminClient";

    final BeanFactory beanFactory;

    final UserManagerConfiguration userManagerConfiguration;

    final KeycloakProperties keycloakProperties;

    @Bean(name="keycloak")
    public Keycloak keycloak(UserManagerConfiguration userManagerConfiguration) {

        return initKeycloak(userManagerConfiguration, keycloakProperties.master());
    }

    @PostConstruct
    public void onPostConstruct() {
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;

        String realm = keycloakProperties.admin().realm();
        if (StringUtils.isNotEmpty(keycloakProperties.admin().clientSecret())) {
            ResteasyClient client = createResteasyClient(userManagerConfiguration);

            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(userManagerConfiguration.keycloakServerUrl)
                    .realm(realm)
                    .clientId(keycloakProperties.admin().clientId())
                    .clientSecret(keycloakProperties.admin().clientSecret())
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .resteasyClient(client)
                    .build();
            configurableBeanFactory.registerSingleton(KEYCLOAK_BEAN_NAME, keycloak);
        }
    }

    private Keycloak initKeycloak(UserManagerConfiguration userManagerConfiguration, KeycloakProperties.Realm realm) {
        ResteasyClient client = createResteasyClient(userManagerConfiguration);

        return KeycloakBuilder.builder()
                .serverUrl(userManagerConfiguration.keycloakServerUrl)
                .realm(realm.realm())
                .clientId(realm.clientId())
                .username(realm.user())
                .password(realm.password())
                .resteasyClient(client)
                .build();
    }

    private ResteasyClient createResteasyClient(UserManagerConfiguration userManagerConfiguration) {
        log.info("Load Keycloak behind proxy configuration: http.proxy.enable[{}], http.proxy.host[{}], http.proxy.port[{}], http.poolSize[{}], http.sockettimeout[{}], http.establishConnectionTimeout[{}], http.connectionCheckoutTimeout[{}]",
                userManagerConfiguration.keycloakAdminclientHttpProxyEnable,
                userManagerConfiguration.keycloakAdminclientHttpProxyHost,
                userManagerConfiguration.keycloakAdminclientHttpProxyPort,
                userManagerConfiguration.keycloakAdminclientProxyHttpPoolSize,
                userManagerConfiguration.keycloakAdminclientHttpSocketTimeout,
                userManagerConfiguration.keycloakAdminclientHttpEstablishConnectionTimeout,
                userManagerConfiguration.keycloakAdminclientHttpConnectionCheckoutTimeout);

        ResteasyClientBuilder resteasyClientBuilder = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                .connectionPoolSize(userManagerConfiguration.keycloakAdminclientProxyHttpPoolSize)
                .readTimeout(userManagerConfiguration.keycloakAdminclientHttpSocketTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(userManagerConfiguration.keycloakAdminclientHttpEstablishConnectionTimeout, TimeUnit.MILLISECONDS)
                .connectionCheckoutTimeout(userManagerConfiguration.keycloakAdminclientHttpConnectionCheckoutTimeout, TimeUnit.MILLISECONDS);

        if (userManagerConfiguration.keycloakAdminclientHttpProxyEnable) {
            resteasyClientBuilder.defaultProxy(userManagerConfiguration.keycloakAdminclientHttpProxyHost, userManagerConfiguration.keycloakAdminclientHttpProxyPort);
        }
        return resteasyClientBuilder.build();
    }
}
