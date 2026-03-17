package kcs.funding.fundingboost.payment.infra;

import java.util.UUID;
import kcs.funding.fundingboost.payment.application.GeneralPaymentGateway;
import kcs.funding.fundingboost.payment.application.PgAuthorizeRequest;
import kcs.funding.fundingboost.payment.application.PgAuthorizeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockPgGateway implements GeneralPaymentGateway {

    @Value("${app.payment.mock-pg.provider:MOCK-PG}")
    private String provider;

    @Value("${app.payment.mock-pg.force-fail:false}")
    private boolean forceFail;

    @Override
    public PgAuthorizeResult authorize(PgAuthorizeRequest request) {
        if (forceFail) {
            return PgAuthorizeResult.rejected(provider, "MOCK_DECLINED", "mock pg forced fail");
        }

        String transactionId = "mock_tx_" + UUID.randomUUID().toString().replace("-", "");
        return PgAuthorizeResult.approved(provider, transactionId);
    }
}

