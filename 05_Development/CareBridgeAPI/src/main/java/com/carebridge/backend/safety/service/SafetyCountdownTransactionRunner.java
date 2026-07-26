package com.carebridge.backend.safety.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Executes one countdown event in an isolated transaction. */
@Component
public class SafetyCountdownTransactionRunner {

    private final TransactionTemplate requiresNew;

    public SafetyCountdownTransactionRunner(PlatformTransactionManager transactionManager) {
        requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void run(Runnable work) {
        requiresNew.executeWithoutResult(status -> work.run());
    }
}
