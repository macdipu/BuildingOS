package com.buildingos.subscription.shared.application.port.out;

import java.util.function.Supplier;

/** Runs a use case's reads, writes and audit entry atomically. */
public interface UnitOfWork {
    <T> T inTransaction(Supplier<T> work);
}
