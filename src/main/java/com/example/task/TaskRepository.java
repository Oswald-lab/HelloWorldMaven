package com.example.task;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {

    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();

    public Task save(Long userId, Long parentId, String title, String description,
                     LocalDate dueDate, String priority, String category) {
        Long id = ids.incrementAndGet();
        Task task = new Task(id, userId, parentId, title, description, dueDate, priority, category);
        tasks.put(id, task);
        return task;
    }

    public List<Task> findByUserId(Long userId) {
        return tasks.values().stream()
                .filter(task -> task.getUserId().equals(userId))
                .sorted(Comparator.comparing(Task::getId).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Task> findByIdAndUserId(Long id, Long userId) {
        Task task = tasks.get(id);
        if (task == null || !task.getUserId().equals(userId)) {
            return Optional.empty();
        }
        return Optional.of(task);
    }

    public void delete(Task task) {
        tasks.remove(task.getId());
        tasks.values().removeIf(candidate -> task.getId().equals(candidate.getParentId()));
    }
}
