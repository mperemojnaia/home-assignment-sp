package com.bank.statementprocessor.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * This entity is used to validate transaction data from uploaded CSV or JSON files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {
    
    /**
     * Unique numeric identifier for the transaction
     */
    private Long reference;
    
    /**
     * International Bank Account Number (IBAN) for the account
     */
    private String accountNumber;
    
    /**
     * Description of the transaction
     */
    private String description;
    
    /**
     * Starting balance before the transaction
     */
    private BigDecimal startBalance;
    
    /**
     * Transaction amount (positive for credits, negative for debits)
     */
    private BigDecimal mutation;
    
    /**
     * Ending balance after the transaction
     */
    private BigDecimal endBalance;
    
    /**
     * Validates that the balance calculation is exactly correct.
     * The calculation verifies: startBalance + mutation = endBalance
     *
     * @return true if the balance calculation is exactly correct, false otherwise
     */
    public boolean isBalanceCorrect() {
        if (startBalance == null || mutation == null || endBalance == null) {
            return false;
        }
        
        BigDecimal calculated = startBalance.add(mutation);
        return calculated.compareTo(endBalance) == 0;
    }
}
