package com.example.user;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    public User save(String email, String passwordHash) {
        Long id = ids.incrementAndGet();
        User user = new User(id, email, passwordHash);
        usersById.put(id, user);
        usersByEmail.put(email, user);
        return user;
    }

    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersById.get(id));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByEmail.get(email));
    }

    public boolean existsByEmail(String email) {
        return email != null && usersByEmail.containsKey(email);
    }
}
