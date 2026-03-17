package kcs.funding.fundingboost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FundingBoostApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundingBoostApplication.class, args);
    }

}
