package com.example.financialmanager.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    // @Column(nullable = false) // Replaced by ManyToOne relationship
    // private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Constructors
    public Transaction() {
    }

    // Constructor updated for Category entity
    public Transaction(User user, BigDecimal amount, LocalDate date, Category category, String description, TransactionType type) {
        this.user = user;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
        this.type = type;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    // Getter and Setter for Category entity
    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(user, that.user) && // Be careful with LAZY fetched fields in equals/hashCode if not loaded
               Objects.equals(amount, that.amount) &&
               Objects.equals(date, that.date) &&
               Objects.equals(category != null ? category.getId() : null, that.category != null ? that.category.getId() : null) && // Compare category IDs
               type == that.type;
    }

    @Override
    public int hashCode() {
        // Be careful with LAZY fetched fields in equals/hashCode if not loaded
        return Objects.hash(id, user != null ? user.getId() : null, amount, date, category != null ? category.getId() : null, type); // Use category ID
    }

    // toString
    @Override
    public String toString() {
        return "Transaction{" +
               "id=" + id +
               ", userId=" + (user != null ? user.getId() : null) +
               ", categoryId=" + (category != null ? category.getId() : null) + // Avoid loading category for toString
               ", amount=" + amount +
               ", date=" + date +
               ", description='" + description + '\'' +
               ", type=" + type +
               '}';
    }
}
