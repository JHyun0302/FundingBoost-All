package kcs.funding.fundingboost.catalog.infra;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.catalog")
public class CatalogClientProperties {

    private String reader = "local";
    private Remote remote = new Remote();

    @Getter
    @Setter
    public static class Remote {
        private String baseUrl = "http://funding-crawler:8090";
        private int connectTimeoutMs = 1000;
        private int readTimeoutMs = 3000;
        private boolean fallbackToLocal = true;
    }
}
