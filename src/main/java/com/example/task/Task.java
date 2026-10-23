package com.example.task;

public class Task {

    private final Long id;
    private final Long userId;
    private final String title;
    private boolean completed;

    public Task(Long id, Long userId, String title) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.completed = false;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void toggle() {
        this.completed = !this.completed;
    }
}
