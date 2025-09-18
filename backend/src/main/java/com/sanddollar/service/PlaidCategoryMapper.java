package com.sanddollar.service;

import com.plaid.client.model.PersonalFinanceCategory;
import org.springframework.stereotype.Component;

/**
 * Maps Plaid personal finance categories to Sand Dollar internal categories.
 */
@Component
public class PlaidCategoryMapper {

    public CategoryMapping mapCategory(PersonalFinanceCategory category) {
        if (category == null) {
            return new CategoryMapping("Misc", "Misc");
        }

        String primary = category.getPrimary();
        String detailed = category.getDetailed();
        String mappedPrimary;

        if (primary == null) {
            mappedPrimary = "Misc";
        } else {
            switch (primary.toUpperCase()) {
                case "GROCERIES" -> mappedPrimary = "Groceries";
                case "RESTAURANT" -> mappedPrimary = "Dining";
                case "RENT" -> mappedPrimary = "Rent";
                case "UTILITIES" -> mappedPrimary = "Utilities";
                case "SUBSCRIPTIONS" -> mappedPrimary = "Subscriptions";
                case "TRANSPORTATION" -> mappedPrimary = "Transport";
                case "HEALTHCARE" -> {
                    if (detailed != null && detailed.toUpperCase().contains("FITNESS")) {
                        mappedPrimary = "Gym";
                    } else {
                        mappedPrimary = "Healthcare";
                    }
                }
                default -> mappedPrimary = "Misc";
            }
        }

        String mappedSecondary;
        if (mappedPrimary.equals("Gym")) {
            mappedSecondary = "Fitness";
        } else if (detailed != null && !detailed.isBlank()) {
            mappedSecondary = formatDetailed(detailed);
        } else {
            mappedSecondary = mappedPrimary;
        }

        return new CategoryMapping(mappedPrimary, mappedSecondary);
    }

    private String formatDetailed(String detailed) {
        String normalised = detailed.replace(':', '_').replace('-', '_');
        String[] parts = normalised.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                   .append(part.substring(1).toLowerCase());
        }
        return builder.length() == 0 ? "Misc" : builder.toString();
    }

    public record CategoryMapping(String primary, String secondary) {}
}
