package com.sanddollar.budgeting;

import org.springframework.stereotype.Component;

@Component
public class CategoryNormalizer {

    public String normalizeCategory(String categoryTop, String categorySub) {
        String category = selectCategory(categoryTop, categorySub);
        return normalizeAndCapitalize(category);
    }

    private String selectCategory(String categoryTop, String categorySub) {
        if (categoryTop != null && !categoryTop.trim().isEmpty() && !categoryTop.trim().equalsIgnoreCase("Misc")) {
            return categoryTop;
        }

        if (categorySub != null && !categorySub.trim().isEmpty()) {
            return categorySub;
        }

        return "Misc";
    }

    private String normalizeAndCapitalize(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Misc";
        }

        String normalized = category.trim()
            .replaceAll("\\s+", " ")
            .toLowerCase();

        return capitalizeWords(normalized);
    }

    private String capitalizeWords(String text) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}