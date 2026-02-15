package com.bank.statementprocessor.testutil;

import com.bank.statementprocessor.core.model.TransactionRecord;

import java.math.BigDecimal;

/**
 * Test data builder for TransactionRecord.
 * Provides a fluent API for creating test data with sensible defaults.
 */
public class TransactionRecordBuilder {
    
    private Long reference = 100001L;
    private String accountNumber = "NL91RABO0315273637";
    private String description = "Test Transaction";
    private BigDecimal startBalance = new BigDecimal("100.00");
    private BigDecimal mutation = new BigDecimal("50.00");
    private BigDecimal endBalance = new BigDecimal("150.00");
    
    public static TransactionRecordBuilder aTransactionRecord() {
        return new TransactionRecordBuilder();
    }
    
    public TransactionRecordBuilder withReference(Long reference) {
        this.reference = reference;
        return this;
    }
    
    public TransactionRecordBuilder withAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }
    
    public TransactionRecordBuilder withDescription(String description) {
        this.description = description;
        return this;
    }
    
    public TransactionRecordBuilder withStartBalance(String startBalance) {
        this.startBalance = new BigDecimal(startBalance);
        return this;
    }
    
    public TransactionRecordBuilder withStartBalance(BigDecimal startBalance) {
        this.startBalance = startBalance;
        return this;
    }
    
    public TransactionRecordBuilder withMutation(String mutation) {
        this.mutation = new BigDecimal(mutation);
        return this;
    }
    
    public TransactionRecordBuilder withMutation(BigDecimal mutation) {
        this.mutation = mutation;
        return this;
    }
    
    public TransactionRecordBuilder withEndBalance(String endBalance) {
        this.endBalance = new BigDecimal(endBalance);
        return this;
    }
    
    public TransactionRecordBuilder withEndBalance(BigDecimal endBalance) {
        this.endBalance = endBalance;
        return this;
    }
    
    /**
     * Sets balances that are mathematically correct (startBalance + mutation = endBalance).
     */
    public TransactionRecordBuilder withCorrectBalances(String start, String mutation) {
        this.startBalance = new BigDecimal(start);
        this.mutation = new BigDecimal(mutation);
        this.endBalance = this.startBalance.add(this.mutation);
        return this;
    }
    
    /**
     * Sets balances that are mathematically incorrect (for testing validation failures).
     */
    public TransactionRecordBuilder withIncorrectBalances(String start, String mutation, String end) {
        this.startBalance = new BigDecimal(start);
        this.mutation = new BigDecimal(mutation);
        this.endBalance = new BigDecimal(end);
        return this;
    }
    
    public TransactionRecord build() {
        return TransactionRecord.builder()
                .reference(reference)
                .accountNumber(accountNumber)
                .description(description)
                .startBalance(startBalance)
                .mutation(mutation)
                .endBalance(endBalance)
                .build();
    }
}
