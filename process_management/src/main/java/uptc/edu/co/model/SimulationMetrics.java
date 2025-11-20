package uptc.edu.co.model;

import java.util.ArrayList;
import java.util.List;

public class SimulationMetrics {
    private double averageWaitingTime;
    private double averageTurnaroundTime;
    private double throughput;
    private double pageFaultRate;
    private int fileConflictsResolved;
    private List<String> executionLog;
    
    public SimulationMetrics() {
        this.executionLog = new ArrayList<>();
    }
    
    public double getAverageWaitingTime() { return averageWaitingTime; }
    public void setAverageWaitingTime(double averageWaitingTime) { this.averageWaitingTime = averageWaitingTime; }
    
    public double getAverageTurnaroundTime() { return averageTurnaroundTime; }
    public void setAverageTurnaroundTime(double averageTurnaroundTime) { this.averageTurnaroundTime = averageTurnaroundTime; }
    
    public double getThroughput() { return throughput; }
    public void setThroughput(double throughput) { this.throughput = throughput; }
    
    public double getPageFaultRate() { return pageFaultRate; }
    public void setPageFaultRate(double pageFaultRate) { this.pageFaultRate = pageFaultRate; }
    
    public int getFileConflictsResolved() { return fileConflictsResolved; }
    public void setFileConflictsResolved(int fileConflictsResolved) { this.fileConflictsResolved = fileConflictsResolved; }
    
    public List<String> getExecutionLog() { return executionLog; }
    public void addLogEntry(String entry) { executionLog.add(entry); }
    
}