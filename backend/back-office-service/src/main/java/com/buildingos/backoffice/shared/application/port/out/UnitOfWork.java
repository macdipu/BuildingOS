package com.buildingos.backoffice.shared.application.port.out;

import java.util.function.Supplier;

/** Runs a use case's reads, writes and transition rows atomically. */
public interface UnitOfWork {
    <T> T inTransaction(Supplier<T> work);
}
