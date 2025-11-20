package uptc.edu.co.scheduling;

import uptc.edu.co.model.Process;
import uptc.edu.co.model.SimulationMetrics;
import java.util.*;

public class SJFScheduler extends BaseScheduler {
    private boolean preemptive;
    
    public SJFScheduler(boolean preemptive) {
        super();
        this.preemptive = preemptive;
    }
    
    @Override
    public Process getNextProcess() {
        if (readyQueue.isEmpty()) {
            if (currentProcess != null && currentProcess.isFinished()) {
                completeProcess(currentProcess);
                currentProcess = null;
            }
            return currentProcess;
        }
        
        readyQueue.sort(Comparator.comparingInt(Process::getRemainingTime));
        Process shortestJob = readyQueue.get(0);
        
        if (currentProcess == null) {
            readyQueue.remove(shortestJob);
            currentProcess = shortestJob;
            currentProcess.setState(Process.ProcessState.RUNNING);
            
            if (currentProcess.getStartTime() == 0) {
                currentProcess.setStartTime(currentTime);
            }
            
        } else if (currentProcess.isFinished()) {
            completeProcess(currentProcess);
            readyQueue.remove(shortestJob);
            currentProcess = shortestJob;
            currentProcess.setState(Process.ProcessState.RUNNING);
            
            if (currentProcess.getStartTime() == 0) {
                currentProcess.setStartTime(currentTime);
            }
            
        } else if (preemptive && shortestJob.getRemainingTime() < currentProcess.getRemainingTime()) {
            readyQueue.add(currentProcess);
            currentProcess.setState(Process.ProcessState.READY);
            
            readyQueue.remove(shortestJob);
            currentProcess = shortestJob;
            currentProcess.setState(Process.ProcessState.RUNNING);
            
            if (currentProcess.getStartTime() == 0) {
                currentProcess.setStartTime(currentTime);
            }
        }
        
        return currentProcess;
    }
    
    @Override
    public SimulationMetrics executeScheduling() {
        executionHistory.add("=== STARTING SJF SCHEDULING ===");
        executionHistory.add("Type: " + (preemptive ? "Preemptive" : "No Preemptive"));
        
        while (hasPendingProcesses()) {
            Process process = getNextProcess();
            
            if (process != null) {
                process.execute(1);
                executionHistory.add(String.format("Time %d: Executing process %d (Time remaining: %d)", 
                    currentTime, process.getId(), process.getRemainingTime()));
                
                if (process.isFinished()) {
                    completeProcess(process);
                    currentProcess = null;
                }
            } else {
                executionHistory.add(String.format("Time %d: CPU inactive", currentTime));
            }
            
            tick();
        }
        
        calculateMetrics();
        executionHistory.add("=== SCHEDULE COMPLETED ===");
        return metrics;
    }
    
    @Override
    public String getAlgorithmName() {
        return "SJF - " + (preemptive ? "Preemptive" : "No Preemptive");
    }
}