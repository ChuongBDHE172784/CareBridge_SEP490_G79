package com.carebridge.backend.checklist.distribution;

public record ChecklistDistributionResult(
        int createdInstances,
        int existingInstances,
        int createdTasks,
        int existingTasks,
        int cancelledInstances,
        int deniedRecipients,
        int conflicts,
        int failures) {

    public static ChecklistDistributionResult created(int instances, int tasks) {
        return new ChecklistDistributionResult(instances, 0, tasks, 0, 0, 0, 0, 0);
    }

    public static ChecklistDistributionResult existing(int instances, int tasks) {
        return new ChecklistDistributionResult(0, instances, 0, tasks, 0, 0, 0, 0);
    }
}
