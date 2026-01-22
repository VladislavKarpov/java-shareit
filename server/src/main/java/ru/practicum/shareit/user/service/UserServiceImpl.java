package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.EmailAlreadyExistsException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        log.info("UserService.create: email={}, name={}", userDto.getEmail(), userDto.getName());

        repository.findByEmailIgnoreCase(userDto.getEmail()).ifPresent(u -> {
            log.warn("UserService.create: email already exists email={}", userDto.getEmail());
            throw new EmailAlreadyExistsException("Email already exists: " + userDto.getEmail());
        });

        var user = UserMapper.toUser(userDto);
        user = repository.save(user);

        log.info("UserService.create: created userId={}", user.getId());
        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserDto update(Long userId, UserDto userDto) {
        log.info("UserService.update: userId={}, patch(name={}, email={})", userId, userDto.getName(), userDto.getEmail());

        var user = repository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("UserService.update: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        if (userDto.getEmail() != null && !userDto.getEmail().equalsIgnoreCase(user.getEmail())) {
            repository.findByEmailIgnoreCase(userDto.getEmail()).ifPresent(u -> {
                log.warn("UserService.update: email already exists email={}", userDto.getEmail());
                throw new EmailAlreadyExistsException("Email already exists: " + userDto.getEmail());
            });
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }

        user = repository.save(user);

        log.info("UserService.update: updated userId={}", user.getId());
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto getById(Long userId) {
        log.info("UserService.getById: userId={}", userId);

        var user = repository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("UserService.getById: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        log.info("UserService.getAll");

        var result = repository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();

        log.info("UserService.getAll: resultSize={}", result.size());
        return result;
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        log.info("UserService.delete: userId={}", userId);
        repository.deleteById(userId);
        log.info("UserService.delete: deleted userId={}", userId);
    }
}