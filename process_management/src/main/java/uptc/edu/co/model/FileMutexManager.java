package uptc.edu.co.model;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Iterator;

public class FileMutexManager {
    private static FileMutexManager instance;
    private final Semaphore fileMutex;
    private Process currentProcess;
    private int remainingMutexTime;
    private final Map<Process, Integer> waitingProcesses;
    private long lastUpdateTime;
    
    private FileMutexManager() {
        this.fileMutex = new Semaphore(1, true);
        this.currentProcess = null;
        this.remainingMutexTime = 0;
        this.waitingProcesses = new ConcurrentHashMap<>();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public static synchronized FileMutexManager getInstance() {
        if (instance == null) {
            instance = new FileMutexManager();
        }
        return instance;
    }
    
    public boolean requestAccess(Process process) {
        if (fileMutex.tryAcquire()) {
            currentProcess = process;
            remainingMutexTime = 5;
            waitingProcesses.remove(process);
            lastUpdateTime = System.currentTimeMillis();
            
            return true;
        } else {
            waitingProcesses.put(process, 5);
            return false;
        }
    }
    
    public void extendAccess(Process process) {
        if (currentProcess != null && currentProcess.equals(process)) {
            remainingMutexTime += 5;
            lastUpdateTime = System.currentTimeMillis();
        }
    }
    
    public void releaseAccess(Process process) {
        if (currentProcess != null && currentProcess.equals(process)) {
            
            fileMutex.release();
            currentProcess = null;
            remainingMutexTime = 0;
            
        }
    }
    
    public void updateMutexTime() {
        if (currentProcess != null && remainingMutexTime > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - lastUpdateTime) / 1000;
            
            if (elapsedSeconds >= 1) {
                remainingMutexTime -= elapsedSeconds;
                lastUpdateTime = currentTime;
                
                if (remainingMutexTime <= 0) {
                    releaseAccess(currentProcess);
                    assignToNextWaitingProcess();
                }
            }
        }
    }
    
    private void assignToNextWaitingProcess() {
        if (!waitingProcesses.isEmpty()) {
            Iterator<Map.Entry<Process, Integer>> iterator = waitingProcesses.entrySet().iterator();
            if (iterator.hasNext()) {
                Map.Entry<Process, Integer> nextEntry = iterator.next();
                Process nextProcess = nextEntry.getKey();
                
                if (requestAccess(nextProcess)) {
                    iterator.remove();
                }
            }
        }
    }
    
    public void forceTimeUpdate() {
        updateMutexTime();
    }
    
    public Process getCurrentProcess() { return currentProcess; }
    public int getRemainingMutexTime() { return remainingMutexTime; }
    public Map<Process, Integer> getWaitingProcesses() { return waitingProcesses; }
    public boolean isFileInUse() { return currentProcess != null; }
    
    public void reset() {
        fileMutex.drainPermits();
        fileMutex.release(1);
        currentProcess = null;
        remainingMutexTime = 0;
        waitingProcesses.clear();
        lastUpdateTime = System.currentTimeMillis();
    }
}