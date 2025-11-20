package uptc.edu.co.memory;

public class PageFrame {
    private int frameId;
    private Page page;
    private boolean occupied;
    private int loadTime;
    private int lastAccessTime;
    
    public PageFrame(int frameId) {
        this.frameId = frameId;
        this.occupied = false;
        this.page = null;
        this.loadTime = -1;
        this.lastAccessTime = -1;
    }
    
    public void loadPage(Page page, int currentTime) {
        this.page = page;
        this.occupied = true;
        this.loadTime = currentTime;
        this.lastAccessTime = currentTime; 
        page.setLoaded(true);
    }
    
    public void access(int currentTime) {
        if (occupied) {
            this.lastAccessTime = currentTime; 
            if (this.page != null) {
                this.page.access(currentTime); 
            }
        }
    }
    
    public void unloadPage() {
        if (this.page != null) {
            this.page.setLoaded(false);
        }
        this.occupied = false;
        this.page = null;
        this.loadTime = -1;
        this.lastAccessTime = -1;
    }
    
    public int getFrameId() { return frameId; }
    public Page getPage() { return page; }
    public boolean isOccupied() { return occupied; }
    public int getLoadTime() { return loadTime; }
    public int getLastAccessTime() { return lastAccessTime; }
}