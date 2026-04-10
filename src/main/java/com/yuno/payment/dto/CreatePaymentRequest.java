package com.yuno.payment.dto;

import com.yuno.payment.model.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Long amount;

    @NotNull(message = "Currency is required")
    private String currency;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    @NotNull(message = "From account is required")
    private UUID fromAccountId;

    @NotNull(message = "To account is required")
    private UUID toAccountId;

    @NotNull(message = "Idempotency key is required")
    private String idempotencyKey;
}