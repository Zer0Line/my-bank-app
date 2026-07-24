package ru.yandex.practicum.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(schema = "notifications", name = "operations")
public class OperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
