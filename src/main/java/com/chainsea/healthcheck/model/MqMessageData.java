package com.chainsea.healthcheck.model;


import java.io.Serializable;
import java.util.List;

public class MqMessageData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskId;
    private List<String> serviceNames;
    private TaskStatus status;

    public MqMessageData(String taskId, List<String> serviceNames, TaskStatus status) {
        this.taskId = taskId;
        this.serviceNames = serviceNames;
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

    public void setServiceNames(List<String> serviceNames) {
        this.serviceNames = serviceNames;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
