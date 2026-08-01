package ru.yandex.practicum.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.accounts.entity.AccountEntity;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {

    Optional<AccountEntity> findByLogin(String login);

    @Query("SELECT a FROM AccountEntity a WHERE a.login <> :login")
    List<AccountEntity> findAllByLoginIsNot(@Param("login") String login);
}
