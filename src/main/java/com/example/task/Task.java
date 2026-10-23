package com.example.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {

    public static final String PRIORITY_HIGH = "High";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_LOW = "Low";
    public static final String DEFAULT_PRIORITY = PRIORITY_MEDIUM;

    private final Long id;
    private final Long userId;
    private final Long parentId;
    private final String title;
    private final String description;
    private final LocalDate dueDate;
    private final String priority;
    private boolean completed;
    private final List<Task> subtasks = new ArrayList<>();

    public Task(Long id, Long userId, Long parentId, String title, String description,
                LocalDate dueDate, String priority) {
        this.id = id;
        this.userId = userId;
        this.parentId = parentId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.completed = false;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getStatus() {
        return completed ? "Completed" : "Pending";
    }

    public List<Task> getSubtasks() {
        return subtasks;
    }

    public void addSubtask(Task task) {
        subtasks.add(task);
    }

    public boolean isSubtask() {
        return parentId != null;
    }

    public void toggle() {
        this.completed = !this.completed;
    }
}
