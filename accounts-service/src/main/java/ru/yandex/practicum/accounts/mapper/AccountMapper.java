package ru.yandex.practicum.accounts.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.entity.AccountEntity;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "name", expression = "java(entity.getSurename() + \" \" + entity.getName())")
    @Mapping(target = "birthdate", expression = "java(entity.getDateOfBirth().toString())")
    @Mapping(target = "sum", source = "amount")
    @Mapping(target = "accounts", expression = "java(java.util.List.of())")
    AccountResponse toResponse(AccountEntity entity);

    @Mapping(target = "name", expression = "java(entity.getSurename() + \" \" + entity.getName())")
    AccountDto toDto(AccountEntity entity);
}
