package ru.practicum.shareit.user.service;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.EmailAlreadyExistsException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }


    @Override
    public UserDto create(UserDto userDto) {
        repository.findByEmailIgnoreCase(userDto.getEmail()).ifPresent(user -> {
            throw new EmailAlreadyExistsException("Email already exists: " + userDto.getEmail());
        });

        var user = UserMapper.toUser(userDto);
        user = repository.save(user);
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (userDto.getEmail() != null && !userDto.getEmail().equalsIgnoreCase(user.getEmail())) {
            repository.findByEmailIgnoreCase(userDto.getEmail()).ifPresent(userMail -> {
                throw new EmailAlreadyExistsException("Email already exists: " + userDto.getEmail());
            });
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getName() != null) user.setName(userDto.getName());

        user = repository.save(user);
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto getById(Long userId) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        return repository.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long userId) {
        repository.deleteById(userId);
    }
}
