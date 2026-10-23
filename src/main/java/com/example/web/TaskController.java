package com.example.web;

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
        model.addAttribute("tasks", taskRepository.findByUserId(user.getId()));
        return "tasks";
    }

    @PostMapping("/tasks")
    public String create(@RequestParam String title, HttpServletRequest request) {
        User user = currentUser(request);
        String trimmed = title == null ? "" : title.trim();
        if (!trimmed.isEmpty()) {
            taskRepository.save(user.getId(), trimmed);
        }
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/toggle")
    public String toggle(@PathVariable Long id, HttpServletRequest request) {
        User user = currentUser(request);
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        task.toggle();
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

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long userId = session == null ? null : (Long) session.getAttribute(AuthInterceptor.USER_ID);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
