package com.yuno.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class PaymentOrchestratorApplication {

    public static void main(String[] args) {

        ApplicationContext context =
                SpringApplication.run(PaymentOrchestratorApplication.class, args);
        PaymentOrchestrator orchestrator =
                context.getBean(PaymentOrchestrator.class);

        orchestrator.run();
    }

}
