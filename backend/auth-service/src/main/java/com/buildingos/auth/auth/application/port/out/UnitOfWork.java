package com.buildingos.auth.auth.application.port.out;

import java.util.function.Supplier;

/** Runs a use case's role change and its audit row atomically. */
public interface UnitOfWork {
    <T> T inTransaction(Supplier<T> work);
}
