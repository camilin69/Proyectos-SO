package uptc.edu.co.memory;

import uptc.edu.co.model.Process;

public class Page {
    private int pageId;
    private Process process;
    private boolean loaded;
    private int loadTime;
    private int lastAccessTime;
    private boolean referenced;
    
    public Page(int pageId, Process process) {
        this.pageId = pageId;
        this.process = process;
        this.loaded = false;
        this.loadTime = -1;
        this.lastAccessTime = -1;
        this.referenced = false;
    }
    
    public int getPageId() { return pageId; }
    public Process getProcess() { return process; }
    public boolean isLoaded() { return loaded; }
    public int getLoadTime() { return loadTime; }
    public int getLastAccessTime() { return lastAccessTime; }
    public boolean isReferenced() { return referenced; }
    
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    public void setLoadTime(int loadTime) { this.loadTime = loadTime; }
    public void setLastAccessTime(int lastAccessTime) { this.lastAccessTime = lastAccessTime; }
    public void setReferenced(boolean referenced) { this.referenced = referenced; }
    
    public void access(int currentTime) {
        this.lastAccessTime = currentTime;
        this.referenced = true;
    }
}