package com.example.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String list(@RequestParam(required = false) String tab,
                       @RequestParam(required = false) Long undoId,
                       @RequestParam(required = false) String undoStatus,
                       @RequestParam(required = false) String updatedStatus,
                       HttpServletRequest request,
                       Model model) {
        User user = currentUser(request);
        boolean completeTab = "complete".equalsIgnoreCase(tab);
        model.addAttribute("email", user.getEmail());
        model.addAttribute("tab", completeTab ? "complete" : "active");
        model.addAttribute("undoId", undoId);
        model.addAttribute("undoStatus", undoStatus);
        model.addAttribute("updatedStatus", updatedStatus);
        addTasks(user, model, completeTab);
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
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        User user = currentUser(request);
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus != null && task.canTransitionTo(normalizedStatus)) {
            String previousStatus = task.getStatus();
            task.setStatus(normalizedStatus);
            redirectAttributes.addAttribute("undoId", id);
            redirectAttributes.addAttribute("undoStatus", previousStatus);
            redirectAttributes.addAttribute("updatedStatus", normalizedStatus);
        }
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/status/undo")
    public String undoStatus(@PathVariable Long id,
                             @RequestParam(required = false) String status,
                             HttpServletRequest request) {
        User user = currentUser(request);
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus != null) {
            task.setStatus(normalizedStatus);
        }
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/bulk-status")
    public String bulkUpdateStatus(@RequestParam(required = false) List<Long> ids,
                                   @RequestParam(required = false) String status,
                                   HttpServletRequest request) {
        User user = currentUser(request);
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus != null && ids != null) {
            for (Long id : ids) {
                taskRepository.findByIdAndUserId(id, user.getId())
                        .ifPresent(task -> task.setStatus(normalizedStatus));
            }
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
        model.addAttribute("tab", "active");
        model.addAttribute("error", error);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("dueDate", dueDate);
        model.addAttribute("priority", priority);
        addTasks(user, model);
        return "tasks";
    }

    private void addTasks(User user, Model model) {
        addTasks(user, model, false);
    }

    private void addTasks(User user, Model model, boolean completeTab) {
        List<Task> allTasks = taskRepository.findByUserId(user.getId());
        allTasks.sort(Comparator.comparing(Task::getDueDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        long activeCount = allTasks.stream().filter(task -> !task.isCompleted()).count();
        long completeCount = allTasks.stream().filter(Task::isCompleted).count();

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

        List<Task> visible = topLevel.stream()
                .filter(task -> completeTab == task.isCompleted())
                .collect(Collectors.toList());

        model.addAttribute("tasks", visible);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("completeCount", completeCount);
        model.addAttribute("statuses", Task.statuses());
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String trimmed = status.trim();
        if (Task.STATUS_TODO.equalsIgnoreCase(trimmed)) {
            return Task.STATUS_TODO;
        }
        if (Task.STATUS_PROCESSING.equalsIgnoreCase(trimmed)) {
            return Task.STATUS_PROCESSING;
        }
        if (Task.STATUS_COMPLETE.equalsIgnoreCase(trimmed)) {
            return Task.STATUS_COMPLETE;
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
