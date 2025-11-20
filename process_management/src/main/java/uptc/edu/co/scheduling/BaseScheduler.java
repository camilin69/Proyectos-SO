package uptc.edu.co.scheduling;

import uptc.edu.co.model.Process;
import uptc.edu.co.model.SimulationMetrics;
import java.util.*;

public abstract class BaseScheduler implements ProcessScheduler {
    protected List<Process> readyQueue;
    protected List<Process> completedProcesses;
    protected Process currentProcess;
    protected int currentTime;
    protected SimulationMetrics metrics;
    protected List<String> executionHistory;
    
    public BaseScheduler() {
        this.readyQueue = new ArrayList<>();
        this.completedProcesses = new ArrayList<>();
        this.currentProcess = null;
        this.currentTime = 0;
        this.metrics = new SimulationMetrics();
        this.executionHistory = new ArrayList<>();
    }
    
    @Override
    public void addProcess(Process process) {
        readyQueue.add(process);
        process.setState(Process.ProcessState.READY);
        executionHistory.add(String.format("Tiempo %d: Proceso %d agregado a cola lista", 
            currentTime, process.getId()));
    }
    
    @Override
    public boolean hasPendingProcesses() {
        boolean hasPending = !readyQueue.isEmpty() || 
                           (currentProcess != null && !currentProcess.isFinished());
        return hasPending;
    }
    
    @Override
    public List<Process> getReadyQueue() {
        return Collections.unmodifiableList(readyQueue);
    }
    
    @Override
    public Process getCurrentProcess() {
        return currentProcess;
    }
    
    protected void completeProcess(Process process) {
        if (process != null) {
            process.setState(Process.ProcessState.TERMINATED);
            process.setEndTime(currentTime);
            completedProcesses.add(process);
            
            if (readyQueue.contains(process)) {
                readyQueue.remove(process);
            }
            executionHistory.add(String.format("Tiempo %d: Proceso %d TERMINADO", 
                currentTime, process.getId()));
                
        }
    }

    public void tick() {
        currentTime++;
    }
    
    protected void calculateMetrics() {
        if (completedProcesses.isEmpty()) {
            metrics.setAverageWaitingTime(0);
            metrics.setAverageTurnaroundTime(0);
            metrics.setThroughput(0);
            return;
        }
        
        double totalWaitingTime = 0;
        double totalTurnaroundTime = 0;
        
        for (Process process : completedProcesses) {
            totalWaitingTime += process.getWaitingTime(process.getEndTime());
            totalTurnaroundTime += process.getTurnaroundTime();
        }
        
        metrics.setAverageWaitingTime(totalWaitingTime / completedProcesses.size());
        metrics.setAverageTurnaroundTime(totalTurnaroundTime / completedProcesses.size());
        
        int lastCompletionTime = completedProcesses.stream()
                .mapToInt(Process::getEndTime)
                .max()
                .orElse(1);
        metrics.setThroughput((double) completedProcesses.size() / lastCompletionTime);
        
        for (String entry : executionHistory) {
            metrics.addLogEntry(entry);
        }
    }
    
    public abstract Process getNextProcess();
    public abstract SimulationMetrics executeScheduling();
    public abstract String getAlgorithmName();
    public List<Process> getCompletedProcesses() {
        return Collections.unmodifiableList(completedProcesses);
    }
    
    public List<String> getExecutionHistory() {
        return Collections.unmodifiableList(executionHistory);
    }
}