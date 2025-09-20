package com.sanddollar.budgeting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryNormalizerTest {

    private CategoryNormalizer categoryNormalizer;

    @BeforeEach
    void setUp() {
        categoryNormalizer = new CategoryNormalizer();
    }

    @Test
    void testNormalizeCategoryWithValidTopCategory() {
        String result = categoryNormalizer.normalizeCategory("Food and Drink", "Restaurants");
        assertEquals("Food And Drink", result);
    }

    @Test
    void testNormalizeCategoryFallsBackToSubCategory() {
        String result = categoryNormalizer.normalizeCategory("Misc", "Groceries");
        assertEquals("Groceries", result);
    }

    @Test
    void testNormalizeCategoryWithNullTopCategory() {
        String result = categoryNormalizer.normalizeCategory(null, "Gas Stations");
        assertEquals("Gas Stations", result);
    }

    @Test
    void testNormalizeCategoryWithEmptyTopCategory() {
        String result = categoryNormalizer.normalizeCategory("", "Shopping");
        assertEquals("Shopping", result);
    }

    @Test
    void testNormalizeCategoryWithWhitespaceTopCategory() {
        String result = categoryNormalizer.normalizeCategory("   ", "Entertainment");
        assertEquals("Entertainment", result);
    }

    @Test
    void testNormalizeCategoryWithMiscTopCategory() {
        String result = categoryNormalizer.normalizeCategory("misc", "Travel");
        assertEquals("Travel", result);
    }

    @Test
    void testNormalizeCategoryWithBothNullCategories() {
        String result = categoryNormalizer.normalizeCategory(null, null);
        assertEquals("Misc", result);
    }

    @Test
    void testNormalizeCategoryWithBothEmptyCategories() {
        String result = categoryNormalizer.normalizeCategory("", "");
        assertEquals("Misc", result);
    }

    @Test
    void testNormalizeCategoryCapitalization() {
        String result = categoryNormalizer.normalizeCategory("food and drink", null);
        assertEquals("Food And Drink", result);
    }

    @Test
    void testNormalizeCategoryTrimsWhitespace() {
        String result = categoryNormalizer.normalizeCategory("  Food and Drink  ", null);
        assertEquals("Food And Drink", result);
    }

    @Test
    void testNormalizeCategoryNormalizesSpaces() {
        String result = categoryNormalizer.normalizeCategory("Food    and     Drink", null);
        assertEquals("Food And Drink", result);
    }

    @Test
    void testNormalizeCategoryWithSpecialCharacters() {
        String result = categoryNormalizer.normalizeCategory("food & drink", null);
        assertEquals("Food & Drink", result);
    }
}