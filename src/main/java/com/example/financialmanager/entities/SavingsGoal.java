package com.example.financialmanager.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal targetAmount;

    // @Column(nullable = false) // Removed currentAmount
    // private BigDecimal currentAmount;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false) // Added startDate
    private LocalDate startDate;

    // Constructors
    public SavingsGoal() {
    }

    public SavingsGoal(User user, String name, BigDecimal targetAmount, /*BigDecimal currentAmount,*/ LocalDate targetDate, LocalDate startDate) {
        this.user = user;
        this.name = name;
        this.targetAmount = targetAmount;
        // this.currentAmount = currentAmount; // Removed
        this.targetDate = targetDate;
        this.startDate = startDate; // Added
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    // public BigDecimal getCurrentAmount() { // Removed
    // return currentAmount;
    // }

    // public void setCurrentAmount(BigDecimal currentAmount) { // Removed
    // this.currentAmount = currentAmount;
    // }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public LocalDate getStartDate() { // Added
        return startDate;
    }

    public void setStartDate(LocalDate startDate) { // Added
        this.startDate = startDate;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavingsGoal that = (SavingsGoal) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(user != null ? user.getId() : null, that.user != null ? that.user.getId() : null) &&
               Objects.equals(name, that.name) &&
               Objects.equals(targetAmount, that.targetAmount) &&
               Objects.equals(targetDate, that.targetDate) &&
               Objects.equals(startDate, that.startDate); // Added
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user != null ? user.getId() : null, name, targetAmount, targetDate, startDate); // Added startDate
    }

    // toString
    @Override
    public String toString() {
        return "SavingsGoal{" +
               "id=" + id +
               ", userId=" + (user != null ? user.getId() : null) +
               ", name='" + name + '\'' +
               ", targetAmount=" + targetAmount +
               // ", currentAmount=" + currentAmount + // Removed
               ", targetDate=" + targetDate +
               ", startDate=" + startDate + // Added
               '}';
    }
}
