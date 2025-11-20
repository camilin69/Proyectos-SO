package uptc.edu.co.scheduling;

import uptc.edu.co.model.Process;
import uptc.edu.co.model.SimulationMetrics;
import java.util.List;

public interface ProcessScheduler {
    void addProcess(Process process);
    Process getNextProcess();
    boolean hasPendingProcesses();
    SimulationMetrics executeScheduling();
    List<Process> getReadyQueue();
    String getAlgorithmName();
    Process getCurrentProcess();
    void tick();
}