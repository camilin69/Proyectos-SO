package uptc.edu.co.scheduling;

import uptc.edu.co.model.Process;
import uptc.edu.co.model.SimulationMetrics;
import java.util.*;

public class PriorityScheduler extends BaseScheduler {
    private boolean preemptive;
    
    public PriorityScheduler(boolean preemptive) {
        super();
        this.preemptive = preemptive;
    }
    
    @Override
    public Process getNextProcess() {
        if (readyQueue.isEmpty() && (currentProcess == null || currentProcess.isFinished())) {
            currentProcess = null;
            return null;
        }
        
        if (currentProcess != null && !currentProcess.isFinished() && !preemptive) {
            return currentProcess;
        }
        
        readyQueue.sort(Comparator.comparingInt(Process::getPriority));
        Process highestPriority = readyQueue.isEmpty() ? null : readyQueue.get(0);
        
        if (currentProcess == null || currentProcess.isFinished()) {
            if (currentProcess != null && currentProcess.isFinished()) {
                completeProcess(currentProcess);
                currentProcess = null;
            }
            
            if (highestPriority != null) {
                readyQueue.remove(highestPriority);
                currentProcess = highestPriority;
                currentProcess.setState(Process.ProcessState.RUNNING);
                
                if (currentProcess.getStartTime() == 0) {
                    currentProcess.setStartTime(currentTime);
                }
                
                return currentProcess;
            }
            return null;
        }
        
        if (preemptive && highestPriority != null && 
            highestPriority.getPriority() < currentProcess.getPriority()) {
            
            // Volver el proceso actual a la cola
            readyQueue.add(currentProcess);
            currentProcess.setState(Process.ProcessState.READY);
            
            // Tomar el nuevo proceso de mayor prioridad
            readyQueue.remove(highestPriority);
            currentProcess = highestPriority;
            currentProcess.setState(Process.ProcessState.RUNNING);
            
            if (currentProcess.getStartTime() == 0) {
                currentProcess.setStartTime(currentTime);
            }
            
            return currentProcess;
        }
        
        return currentProcess;
    }
    
    @Override
    public SimulationMetrics executeScheduling() {
        executionHistory.add("=== STARTING PRIORITY SCHEDULING ===");
        executionHistory.add("Type: " + (preemptive ? "Preemptive" : "No Preemptive"));
        
        while (hasPendingProcesses()) {
            Process process = getNextProcess();
            
            if (process != null) {
                
                process.execute(1);
                
                if (process.isFinished()) {
                    completeProcess(process);
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
    public boolean hasPendingProcesses() {
        boolean hasPending = !readyQueue.isEmpty() || (currentProcess != null && !currentProcess.isFinished());
        return hasPending;
    }
    
    @Override
    public String getAlgorithmName() {
        return "Priority - " + (preemptive ? "Preemptive" : "No Preemptive");
    }
}