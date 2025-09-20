package com.sanddollar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class BudgetConfig {

    @Bean("incomeWhitelistNames")
    public Set<String> incomeWhitelistNames() {
        return Set.of("Cozy Eart Dir Dep", "Cozy Earth", "Mastercard Stipend");
    }

    @Bean("incomeWhitelistCategories")
    public Set<String> incomeWhitelistCategories() {
        return Set.of("Payroll", "Salary", "Income Wages", "Paycheck");
    }

    @Bean("issuerSet")
    public Set<String> issuerSet() {
        return Set.of("American Express", "Amex", "Chase", "JPMorgan", "Capital One",
                     "Citi", "Citibank", "Discover", "Bank of America", "Wells Fargo",
                     "US Bank", "Barclays");
    }
}