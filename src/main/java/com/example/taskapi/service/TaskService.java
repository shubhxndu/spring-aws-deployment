package com.example.taskapi.service;

import com.example.taskapi.dto.TaskRequest;
import com.example.taskapi.dto.TaskResponse;
import com.example.taskapi.entity.Task;
import com.example.taskapi.exception.ResourceNotFoundException;
import com.example.taskapi.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id) {
        return toResponse(findTask(id));
    }

    public TaskResponse createTask(TaskRequest request) {
        boolean completed = request.getCompleted() != null && request.getCompleted();
        Task task = new Task(request.getTitle().trim(), request.getDescription(), completed);
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTask(id);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setCompleted(request.getCompleted() != null && request.getCompleted());
        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        taskRepository.delete(findTask(id));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getCreatedAt());
    }
}
