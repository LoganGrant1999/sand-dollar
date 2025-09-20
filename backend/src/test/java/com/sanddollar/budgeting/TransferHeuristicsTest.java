package com.sanddollar.budgeting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransferHeuristicsTest {

    private TransferHeuristics transferHeuristics;

    @BeforeEach
    void setUp() {
        transferHeuristics = new TransferHeuristics();
    }

    @Test
    void testIsTransferWithTransferKeywords() {
        assertTrue(transferHeuristics.isTransfer("Online Transfer", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("ACH TRANSFER", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Wire Transfer", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Mobile Transfer", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("External Transfer", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Deposit", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Withdrawal", "Misc", "Misc"));
    }

    @Test
    void testIsTransferWithTransferCategories() {
        assertTrue(transferHeuristics.isTransfer("Some Transaction", "transfer", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Some Transaction", "Bank Fees", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Some Transaction", "Deposit", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Some Transaction", "Misc", "transfer"));
        assertTrue(transferHeuristics.isTransfer("Some Transaction", "Misc", "bank fees"));
    }

    @Test
    void testIsTransferWithAccountPattern() {
        assertTrue(transferHeuristics.isTransfer("Transfer to account 1234", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("From acct #5678", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Account *9012", "Misc", "Misc"));
    }

    @Test
    void testIsTransferWithConfirmationPattern() {
        assertTrue(transferHeuristics.isTransfer("Transfer ref#ABC123DEF", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Confirmation 987654321", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Conf #XYZ789", "Misc", "Misc"));
    }

    @Test
    void testIsNotTransfer() {
        assertFalse(transferHeuristics.isTransfer("Starbucks Coffee", "Food and Drink", "Coffee Shops"));
        assertFalse(transferHeuristics.isTransfer("Gas Station", "Transportation", "Gas"));
        assertFalse(transferHeuristics.isTransfer("Grocery Store", "Food and Drink", "Groceries"));
        assertFalse(transferHeuristics.isTransfer("Amazon Purchase", "Shopping", "General Merchandise"));
    }

    @Test
    void testIsTransferWithNullTransactionName() {
        assertFalse(transferHeuristics.isTransfer(null, "transfer", "Misc"));
    }

    @Test
    void testIsTransferCaseInsensitive() {
        assertTrue(transferHeuristics.isTransfer("ONLINE TRANSFER", "misc", "misc"));
        assertTrue(transferHeuristics.isTransfer("online transfer", "MISC", "MISC"));
        assertTrue(transferHeuristics.isTransfer("Online Transfer", "Transfer", "transfer"));
    }

    @Test
    void testIsTransferPartialMatch() {
        assertTrue(transferHeuristics.isTransfer("Some transfer to savings", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Direct deposit from employer", "Misc", "Misc"));
        assertTrue(transferHeuristics.isTransfer("Mobile xfer confirmation", "Misc", "Misc"));
    }
}