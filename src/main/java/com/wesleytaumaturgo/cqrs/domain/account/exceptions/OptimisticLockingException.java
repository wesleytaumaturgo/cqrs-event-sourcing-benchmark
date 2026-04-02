package com.wesleytaumaturgo.cqrs.domain.account.exceptions;

import java.util.UUID;

/**
 * Lançada quando dois processos tentam gravar na mesma versão de um aggregate simultaneamente.
 * A versão esperada já foi ocupada por outro append concorrente.
 */
public class OptimisticLockingException extends RuntimeException {

    public OptimisticLockingException(UUID aggregateId, long expectedVersion) {
        super("Concurrent modification on aggregate " + aggregateId
              + " at expected version " + expectedVersion);
    }
}
