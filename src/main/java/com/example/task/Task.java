package com.example.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {

    public static final String PRIORITY_HIGH = "High";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_LOW = "Low";
    public static final String DEFAULT_PRIORITY = PRIORITY_MEDIUM;

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_DONE = "Done";

    private final Long id;
    private final Long userId;
    private final Long parentId;
    private final String title;
    private final String description;
    private final LocalDate dueDate;
    private final String priority;
    private String status;
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
        this.status = STATUS_PENDING;
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
        return STATUS_DONE.equals(status);
    }

    public String getStatus() {
        return status;
    }

    public boolean canTransitionTo(String target) {
        if (STATUS_PENDING.equals(status)) {
            return STATUS_IN_PROGRESS.equals(target);
        }
        if (STATUS_IN_PROGRESS.equals(status)) {
            return STATUS_DONE.equals(target);
        }
        if (STATUS_DONE.equals(status)) {
            return STATUS_IN_PROGRESS.equals(target);
        }
        return false;
    }

    public void setStatus(String newStatus) {
        this.status = newStatus;
    }

    public static List<String> statuses() {
        return List.of(STATUS_PENDING, STATUS_IN_PROGRESS, STATUS_DONE);
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
}
