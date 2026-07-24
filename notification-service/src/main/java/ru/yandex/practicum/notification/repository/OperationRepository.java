package ru.yandex.practicum.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.notification.entity.OperationEntity;

import java.util.List;

public interface OperationRepository extends JpaRepository<OperationEntity, Long> {

    List<OperationEntity> findByLoginOrderByCreatedAtDesc(String login);
}
