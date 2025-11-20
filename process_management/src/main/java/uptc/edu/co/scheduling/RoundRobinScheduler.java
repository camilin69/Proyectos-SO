package uptc.edu.co.scheduling;

import uptc.edu.co.model.Process;
import uptc.edu.co.model.SimulationMetrics;
import java.util.*;

public class RoundRobinScheduler extends BaseScheduler {
    private int quantum;
    private int currentQuantum;
    private Queue<Process> processQueue;
    
    public RoundRobinScheduler(int quantum) {
        super();
        this.quantum = quantum;
        this.currentQuantum = 0;
        this.processQueue = new LinkedList<>();
    }
    
    @Override
    public Process getNextProcess() {
        if (currentProcess != null) {
            if (currentProcess.isFinished()) {
                completeProcess(currentProcess);
                currentProcess = null;
                currentQuantum = 0;
            } else if (currentQuantum >= quantum) {
                processQueue.offer(currentProcess);
                currentProcess.setState(Process.ProcessState.READY);
                currentProcess = null;
                currentQuantum = 0;
            }
        }
        
        if (currentProcess == null && !processQueue.isEmpty()) {
            currentProcess = processQueue.poll();
            currentQuantum = 1; 
            
            if (currentProcess != null) {
                currentProcess.setState(Process.ProcessState.RUNNING);
                if (currentProcess.getStartTime() == 0) {
                    currentProcess.setStartTime(currentTime);
                }
                
                executionHistory.add(String.format("Time %d: Process %d STARTS EXECUTION (Quantum: 1/%d)", 
                    currentTime, currentProcess.getId(), quantum));
            }
        } else if (currentProcess != null && !currentProcess.isFinished()) {
            currentQuantum++;
        }
        
        return currentProcess;
    }
    
    @Override
    public void tick() {
        currentTime++;
    }
    
    @Override
    public void addProcess(Process process) {
        processQueue.offer(process);
        process.setState(Process.ProcessState.READY);
        executionHistory.add(String.format("Time %d: Process %d added to Round Robin queue", 
            currentTime, process.getId()));
    }
    
    @Override
    public boolean hasPendingProcesses() {
        return !processQueue.isEmpty() || currentProcess != null;
    }
    
    @Override
    public List<Process> getReadyQueue() {
        return new ArrayList<>(processQueue);
    }
    
    @Override
    public SimulationMetrics executeScheduling() {
        executionHistory.add("=== Starting ROUND ROBIN ===");
        executionHistory.add(String.format("Quantum: %d time unities", quantum));
        
        while (hasPendingProcesses()) {
            Process process = getNextProcess();
            
            if (process != null) {
                process.execute(1);
                executionHistory.add(String.format("Time %d: Executing process %d (Time remaining: %d, Quantum: %d/%d)", 
                    currentTime, process.getId(), process.getRemainingTime(), currentQuantum, quantum));
                
                if (process.isFinished()) {
                    completeProcess(process);
                    currentProcess = null;
                    currentQuantum = 0;
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
        return "Round Robin (Quantum: " + quantum + ")";
    }
    
    public int getCurrentQuantum() {
        return currentQuantum;
    }
    
    public int getQuantumValue() {
        return quantum;
    }
}