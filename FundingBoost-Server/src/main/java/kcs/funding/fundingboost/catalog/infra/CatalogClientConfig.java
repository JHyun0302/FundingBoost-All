package kcs.funding.fundingboost.catalog.infra;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CatalogClientProperties.class)
public class CatalogClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.catalog", name = "reader", havingValue = "remote")
    public RestClient catalogRestClient(CatalogClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getRemote().getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getRemote().getReadTimeoutMs());

        return RestClient.builder()
                .baseUrl(properties.getRemote().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
