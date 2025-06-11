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
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "name"})
})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private boolean isCustom;

    // Constructors
    public Category() {
    }

    public Category(User user, String name, TransactionType type, boolean isCustom) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return isCustom == category.isCustom &&
               Objects.equals(id, category.id) &&
               Objects.equals(user != null ? user.getId() : null, category.user != null ? category.user.getId() : null) &&
               Objects.equals(name, category.name) &&
               type == category.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user != null ? user.getId() : null, name, type, isCustom);
    }

    // toString
    @Override
    public String toString() {
        return "Category{" +
               "id=" + id +
               ", userId=" + (user != null ? user.getId() : null) +
               ", name='" + name + '\'' +
               ", type=" + type +
               ", isCustom=" + isCustom +
               '}';
    }
}
