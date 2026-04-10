package com.yuno.payment.factory;

import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.provider.PaymentProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderFactory {

    private final Map<PaymentMethod, List<PaymentProvider>> providerMap;

    @Autowired
    public ProviderFactory(List<PaymentProvider> providers) {
        Map<Provider, PaymentProvider> providersByName = providers.stream()
                .collect(Collectors.toMap(PaymentProvider::getProvider, Function.identity()));

        this.providerMap = Map.of(
                PaymentMethod.CARD, orderedProviders(providersByName, Provider.PROVIDER_A, Provider.PROVIDER_B),
                PaymentMethod.UPI, orderedProviders(providersByName, Provider.PROVIDER_B, Provider.PROVIDER_A)
        );
    }

    public List<PaymentProvider> getProviders(PaymentMethod method) {
        if (method == null) {
            return List.of();
        }
        return providerMap.getOrDefault(method, List.of());
    }

    private List<PaymentProvider> orderedProviders(
            Map<Provider, PaymentProvider> providersByName,
            Provider primary,
            Provider fallback) {

        List<PaymentProvider> orderedProviders = new ArrayList<>();
        addIfPresent(orderedProviders, providersByName, primary);
        addIfPresent(orderedProviders, providersByName, fallback);
        return List.copyOf(orderedProviders);
    }

    private void addIfPresent(
            List<PaymentProvider> orderedProviders,
            Map<Provider, PaymentProvider> providersByName,
            Provider provider) {

        PaymentProvider paymentProvider = providersByName.get(provider);
        if (paymentProvider != null) {
            orderedProviders.add(paymentProvider);
        }
    }
}
