package com.example.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {

    public static final String PRIORITY_HIGH = "High";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_LOW = "Low";
    public static final String DEFAULT_PRIORITY = PRIORITY_MEDIUM;

    public static final String STATUS_TODO = "Todo";
    public static final String STATUS_PROCESSING = "Processing";
    public static final String STATUS_COMPLETE = "Complete";

    public static final String CATEGORY_WORK = "Work";
    public static final String CATEGORY_PERSONAL = "Personal";
    public static final String CATEGORY_OTHER = "Other";
    public static final String DEFAULT_CATEGORY = CATEGORY_OTHER;

    private final Long id;
    private final Long userId;
    private final Long parentId;
    private final String title;
    private final String description;
    private final LocalDate dueDate;
    private final String priority;
    private final String category;
    private String status;
    private final List<Task> subtasks = new ArrayList<>();

    public Task(Long id, Long userId, Long parentId, String title, String description,
                LocalDate dueDate, String priority, String category) {
        this.id = id;
        this.userId = userId;
        this.parentId = parentId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.status = STATUS_TODO;
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

    public String getCategory() {
        return category;
    }

    public boolean isCompleted() {
        return STATUS_COMPLETE.equals(status);
    }

    public String getStatus() {
        return status;
    }

    public boolean canTransitionTo(String target) {
        if (STATUS_TODO.equals(status)) {
            return STATUS_PROCESSING.equals(target);
        }
        if (STATUS_PROCESSING.equals(status)) {
            return STATUS_COMPLETE.equals(target);
        }
        if (STATUS_COMPLETE.equals(status)) {
            return STATUS_PROCESSING.equals(target);
        }
        return false;
    }

    public void setStatus(String newStatus) {
        this.status = newStatus;
    }

    public static List<String> statuses() {
        return List.of(STATUS_TODO, STATUS_PROCESSING, STATUS_COMPLETE);
    }

    public static List<String> categories() {
        return List.of(CATEGORY_WORK, CATEGORY_PERSONAL, CATEGORY_OTHER);
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
