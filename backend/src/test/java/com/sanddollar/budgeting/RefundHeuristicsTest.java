package com.sanddollar.budgeting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefundHeuristicsTest {

    private RefundHeuristics refundHeuristics;

    @BeforeEach
    void setUp() {
        refundHeuristics = new RefundHeuristics();
    }

    @Test
    void testIsRefundWithPositiveAmountAndRefundKeywords() {
        assertTrue(refundHeuristics.isRefund("Refund from Amazon", 5000L));
        assertTrue(refundHeuristics.isRefund("Product return", 2500L));
        assertTrue(refundHeuristics.isRefund("Credit adjustment", 1000L));
        assertTrue(refundHeuristics.isRefund("Cashback reward", 500L));
        assertTrue(refundHeuristics.isRefund("Chargeback approved", 10000L));
        assertTrue(refundHeuristics.isRefund("Dispute resolution", 3000L));
        assertTrue(refundHeuristics.isRefund("Rebate from manufacturer", 1500L));
        assertTrue(refundHeuristics.isRefund("Reimbursement for travel", 25000L));
    }

    @Test
    void testIsRefundWithNegativeAmount() {
        assertFalse(refundHeuristics.isRefund("Refund from Amazon", -5000L));
        assertFalse(refundHeuristics.isRefund("Product return", -2500L));
    }

    @Test
    void testIsRefundWithZeroAmount() {
        assertFalse(refundHeuristics.isRefund("Refund from Amazon", 0L));
    }

    @Test
    void testIsRefundWithRefundPattern() {
        assertTrue(refundHeuristics.isRefund("Amazon return processed", 5000L));
        assertTrue(refundHeuristics.isRefund("Credit for overcharge", 1000L));
        assertTrue(refundHeuristics.isRefund("Reversal of previous charge", 2000L));
        assertTrue(refundHeuristics.isRefund("Chargeback successful", 3000L));
    }

    @Test
    void testIsRefundWithMerchantReturnPattern() {
        assertTrue(refundHeuristics.isRefund("Return to Target", 4000L));
        assertTrue(refundHeuristics.isRefund("Refund from Best Buy", 50000L));
        assertTrue(refundHeuristics.isRefund("Return from Walmart", 2500L));
    }

    @Test
    void testIsNotRefund() {
        assertFalse(refundHeuristics.isRefund("Starbucks Coffee", 500L));
        assertFalse(refundHeuristics.isRefund("Gas Station Purchase", 3000L));
        assertFalse(refundHeuristics.isRefund("Amazon Purchase", 2500L));
        assertFalse(refundHeuristics.isRefund("Grocery Store", 5000L));
    }

    @Test
    void testIsRefundWithNullTransactionName() {
        assertFalse(refundHeuristics.isRefund(null, 5000L));
    }

    @Test
    void testIsRefundCaseInsensitive() {
        assertTrue(refundHeuristics.isRefund("REFUND FROM AMAZON", 5000L));
        assertTrue(refundHeuristics.isRefund("refund from amazon", 5000L));
        assertTrue(refundHeuristics.isRefund("Refund From Amazon", 5000L));
        assertTrue(refundHeuristics.isRefund("RETURN TO TARGET", 3000L));
        assertTrue(refundHeuristics.isRefund("credit adjustment", 1000L));
    }

    @Test
    void testIsRefundPartialMatch() {
        assertTrue(refundHeuristics.isRefund("Partial refund for damaged item", 2500L));
        assertTrue(refundHeuristics.isRefund("Store credit for return", 1500L));
        assertTrue(refundHeuristics.isRefund("Cashback from credit card", 500L));
        assertTrue(refundHeuristics.isRefund("Adjustment for billing error", 1000L));
    }

    @Test
    void testIsRefundWithWhitespace() {
        assertTrue(refundHeuristics.isRefund("  Refund from Amazon  ", 5000L));
        assertTrue(refundHeuristics.isRefund("Return\tto\nTarget", 3000L));
    }
}