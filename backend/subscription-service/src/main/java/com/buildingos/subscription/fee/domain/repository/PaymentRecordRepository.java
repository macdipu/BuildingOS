package com.buildingos.subscription.fee.domain.repository;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.PaymentRecord;
import com.buildingos.subscription.fee.domain.model.PaymentReference;
import java.util.List;

public interface PaymentRecordRepository {
    void insert(PaymentRecord payment);
    List<PaymentRecord> find(FeeCode feeCode, PaymentReference reference);
}
