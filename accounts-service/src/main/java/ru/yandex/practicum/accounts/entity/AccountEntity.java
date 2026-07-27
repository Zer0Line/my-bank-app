package ru.yandex.practicum.accounts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(schema = "accounts", name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "login", nullable = false)
    @EqualsAndHashCode.Include
    private String login;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "surename", nullable = false)
    private String surename;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
}
