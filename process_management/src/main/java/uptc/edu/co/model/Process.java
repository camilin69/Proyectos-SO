package uptc.edu.co.model;

import java.util.ArrayList;
import java.util.List;

import uptc.edu.co.memory.Page;

public class Process {
    private int id;
    private String name;
    private int priority;
    private int duration; 
    private int cpuTime; 
    private int memoryUsage;
    private int arrivalTime;
    private int remainingTime; 
    private boolean fileAccess; 
    private String fileAccessType;
    private ProcessState state;
    private int startTime; 
    private int endTime;
    private List<Page> pages;
    private int maxMemory; 
    private int totalWaitingTime = 0;
    private int lastReadyTime = 0;
    private boolean wasReady = false;
    
    public enum ProcessState {
        NEW, READY, RUNNING, BLOCKED, TERMINATED
    }
    
    public Process(int id, String name, int priority, int duration, int cpuTime, 
                   int initialMemory, int maxMemory, boolean fileAccess, int arrivalTime) {
        this(id, name, priority, duration, cpuTime, initialMemory, maxMemory, 
             fileAccess ? "r&w" : "r", arrivalTime); 
    }
    
    public Process(int id, String name, int priority, int duration, int cpuTime, 
                   int initialMemory, int maxMemory, String fileAccessType, int arrivalTime) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.duration = duration;
        this.remainingTime = duration;
        this.arrivalTime = arrivalTime;
        this.state = ProcessState.NEW;
        this.cpuTime = cpuTime;
        this.memoryUsage = initialMemory;
        this.maxMemory = maxMemory;
        this.pages = new ArrayList<>();
        
        setFileAccessType(fileAccessType);
        
        synchronizePagesWithMemory();
    }

    public void setFileAccessType(String fileAccessType) {
        if (fileAccessType == null) {
            fileAccessType = "r"; 
        }
        
        if (!fileAccessType.equals("r") && !fileAccessType.equals("w") && !fileAccessType.equals("r&w")) {
            fileAccessType = "r";
        }
        
        this.fileAccessType = fileAccessType;
        this.fileAccess = !fileAccessType.equals("r");
    }
    
    public String getFileAccessType() {
        return fileAccessType != null ? fileAccessType : (fileAccess ? "r&w" : "r");
    }
    
    public boolean needsFileAccess() { 
        return fileAccess; 
    }
    
    public boolean isReadOnly() {
        return "r".equals(fileAccessType);
    }
    
    public boolean canWrite() {
        return "w".equals(fileAccessType) || "r&w".equals(fileAccessType);
    }
    
    public boolean canRead() {
        return "r".equals(fileAccessType) || "r&w".equals(fileAccessType);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getPriority() { return priority; }
    public int getDuration() { return duration; }
    public int getRemainingTime() { return remainingTime; }
    public int getArrivalTime() { return arrivalTime; }
    public ProcessState getState() { return state; }
    public int getCpuTime() { return cpuTime; }
    public int getStartTime() { return startTime; }
    public int getEndTime() { return endTime; }
    public List<Page> getPages() { return pages; }
    public int getTotalPages() { 
        return (int) Math.ceil((double) memoryUsage / 100);
    }
    public int getMaxPages() {
        return (int) Math.ceil((double) maxMemory / 100);
    }
    public int getPagesInMemory() { 
        int count = 0;
        for (Page page : pages) {
            if (page.isLoaded()) {
                count++;
            }
        }
        return count;
    }

    public void updateWaitingTime(int currentTime) {
        if (state == ProcessState.READY) {
            if (!wasReady) {
                lastReadyTime = currentTime;
                wasReady = true;
            }
        } else {
            if (wasReady) {
                totalWaitingTime += (currentTime - lastReadyTime);
                wasReady = false;
            }
        }
    }

    public void setMemoryUsage(int memoryUsage) {
        if (memoryUsage > maxMemory) {
            memoryUsage = maxMemory;
        }
        this.memoryUsage = memoryUsage;
        synchronizePagesWithMemory();
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    private void synchronizePagesWithMemory() {
        int pagesNeeded = getTotalPages();
        
        while (pages.size() < pagesNeeded) {
            int newPageId = pages.size();
            Page newPage = new Page(newPageId, this);
            newPage.setLoaded(false);
            pages.add(newPage);
        }
        
        for (int i = pagesNeeded; i < pages.size(); i++) {
            Page page = pages.get(i);
            if (page.isLoaded()) {
                page.setLoaded(false);
            }
        }
        
    }

    public void setState(ProcessState state) { this.state = state; }
    public void setStartTime(int startTime) { this.startTime = startTime; }
    public void setEndTime(int endTime) { this.endTime = endTime; }
    
    public void execute(int time) {
        this.cpuTime += time;
        this.remainingTime -= time;
        if (this.remainingTime < 0) {
            this.remainingTime = 0;
        }
    }
    
    public boolean isFinished() {
        return remainingTime <= 0;
    }
    
    public int getWaitingTime(int currentTime) {
        return totalWaitingTime;
    }
    
    public int getTurnaroundTime() {
        if (!isFinished()) return 0;
        return endTime - arrivalTime;
    }
    
    @Override
    public String toString() {
        return String.format("Process{id=%d, name='%s', priority=%d, duration=%d, remaining=%d, state=%s, fileAccess=%s}",
                id, name, priority, duration, remainingTime, state, getFileAccessType());
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setCpuTime(int cpuTime) {
        this.cpuTime = cpuTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public boolean isFileAccess() {
        return fileAccess;
    }

    public void setFileAccess(boolean fileAccess) {
        this.fileAccess = fileAccess;
        this.fileAccessType = fileAccess ? "r&w" : "r";
    }

    public int getMemoryUsage() {
        return memoryUsage;
    }
    
    public void finish(int finishTime) {
        this.endTime = finishTime;
        this.state = ProcessState.TERMINATED;
        if (wasReady) {
            totalWaitingTime += (finishTime - lastReadyTime);
            wasReady = false;
        }
    }
}