package com.example.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.example.task.Task;
import com.example.task.TaskRepository;
import com.example.user.User;
import com.example.user.UserRepository;

@Controller
public class TaskController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskController(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/tasks")
    public String list(HttpServletRequest request, Model model) {
        User user = currentUser(request);
        model.addAttribute("email", user.getEmail());
        addTasks(user, model);
        return "tasks";
    }

    @PostMapping("/tasks")
    public String create(@RequestParam(required = false) String title,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String dueDate,
                         @RequestParam(required = false) String priority,
                         @RequestParam(required = false) List<String> subtasks,
                         HttpServletRequest request,
                         Model model) {
        User user = currentUser(request);
        String trimmedTitle = title == null ? "" : title.trim();

        if (trimmedTitle.isEmpty()) {
            return renderError(user, model, "Title is required.", title, description, dueDate, priority);
        }

        LocalDate parsedDueDate;
        try {
            if (dueDate == null || dueDate.trim().isEmpty()) {
                throw new DateTimeParseException("missing", "", 0);
            }
            parsedDueDate = LocalDate.parse(dueDate.trim());
        } catch (DateTimeParseException ex) {
            return renderError(user, model, "Due date is required.", title, description, dueDate, priority);
        }

        String normalizedPriority = normalizePriority(priority);
        String trimmedDescription = description == null || description.trim().isEmpty()
                ? null : description.trim();

        Task task = taskRepository.save(user.getId(), null, trimmedTitle, trimmedDescription,
                parsedDueDate, normalizedPriority);

        if (subtasks != null) {
            for (String subtaskTitle : subtasks) {
                if (subtaskTitle != null && !subtaskTitle.trim().isEmpty()) {
                    taskRepository.save(user.getId(), task.getId(), subtaskTitle.trim(), null,
                            parsedDueDate, normalizedPriority);
                }
            }
        }

        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam(required = false) String status,
                               HttpServletRequest request) {
        User user = currentUser(request);
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus != null && task.canTransitionTo(normalizedStatus)) {
            task.setStatus(normalizedStatus);
        }
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/delete")
    public String delete(@PathVariable Long id, HttpServletRequest request) {
        User user = currentUser(request);
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        taskRepository.delete(task);
        return "redirect:/tasks";
    }

    private String renderError(User user, Model model, String error, String title,
                               String description, String dueDate, String priority) {
        model.addAttribute("email", user.getEmail());
        model.addAttribute("error", error);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("dueDate", dueDate);
        model.addAttribute("priority", priority);
        addTasks(user, model);
        return "tasks";
    }

    private void addTasks(User user, Model model) {
        List<Task> allTasks = taskRepository.findByUserId(user.getId());
        List<Task> topLevel = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.isSubtask()) {
                allTasks.stream()
                        .filter(parent -> parent.getId().equals(task.getParentId()))
                        .findFirst()
                        .ifPresent(parent -> parent.addSubtask(task));
            } else {
                topLevel.add(task);
            }
        }
        model.addAttribute("tasks", topLevel);
        model.addAttribute("statuses", Task.statuses());
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String trimmed = status.trim();
        if (Task.STATUS_PENDING.equalsIgnoreCase(trimmed)) {
            return Task.STATUS_PENDING;
        }
        if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(trimmed)) {
            return Task.STATUS_IN_PROGRESS;
        }
        if (Task.STATUS_DONE.equalsIgnoreCase(trimmed)) {
            return Task.STATUS_DONE;
        }
        return null;
    }

    private String normalizePriority(String priority) {
        if (priority == null) {
            return Task.DEFAULT_PRIORITY;
        }
        String trimmed = priority.trim();
        if (Task.PRIORITY_HIGH.equalsIgnoreCase(trimmed)) {
            return Task.PRIORITY_HIGH;
        }
        if (Task.PRIORITY_LOW.equalsIgnoreCase(trimmed)) {
            return Task.PRIORITY_LOW;
        }
        return Task.DEFAULT_PRIORITY;
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long userId = session == null ? null : (Long) session.getAttribute(AuthInterceptor.USER_ID);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
