package com.yuno.payment.factory;

import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.provider.PaymentProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderFactory {

    private final Map<PaymentMethod, List<PaymentProvider>> providerMap = new HashMap<>();

    @Autowired
    public ProviderFactory(List<PaymentProvider> providers) {

        for (PaymentProvider provider : providers) {

            if (provider.getProvider() == Provider.PROVIDER_A) {
                providerMap.put(PaymentMethod.CARD, List.of(provider));
            }

            if (provider.getProvider() == Provider.PROVIDER_B) {
                providerMap.put(PaymentMethod.UPI, List.of(provider));
            }
        }
    }

    public List<PaymentProvider> getProviders(PaymentMethod method) {
        return providerMap.getOrDefault(method, List.of());
    }
}
