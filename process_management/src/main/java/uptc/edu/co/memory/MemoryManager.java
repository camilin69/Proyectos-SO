package uptc.edu.co.memory;
import java.util.*;
import uptc.edu.co.model.Process;
public class MemoryManager {
    private List<PageFrame> pageFrames;
    private PageReplacementAlgorithm replacementAlgorithm;
    private int pageFaults;
    private int pageHits;
    private int currentTime;
    private int frameSize; 
    
    public MemoryManager(int totalFrames, PageReplacementAlgorithm algorithm) {
        this.pageFrames = new ArrayList<>();
        this.replacementAlgorithm = algorithm;
        this.pageFaults = 0;
        this.pageHits = 0;
        this.currentTime = 0;
        this.frameSize = 4; 
        
        for (int i = 0; i < totalFrames; i++) {
            pageFrames.add(new PageFrame(i));
        }
    }
    
    public MemoryManager(int totalFrames, int frameSize, PageReplacementAlgorithm algorithm) {
        this.pageFrames = new ArrayList<>();
        this.replacementAlgorithm = algorithm;
        this.pageFaults = 0;
        this.pageHits = 0;
        this.currentTime = 0;
        this.frameSize = frameSize;
        
        for (int i = 0; i < totalFrames; i++) {
            pageFrames.add(new PageFrame(i));
        }
    }
    
    public boolean accessPage(Page page, int time) {
        this.currentTime = time;
        
        for (PageFrame frame : pageFrames) {
            if (frame.isOccupied() && frame.getPage() != null && 
                frame.getPage().equals(page)) {
                frame.access(time); 
                pageHits++;
                return true; 
            }
        }
        
        pageFaults++;
        
        return loadPage(page, time);
    }
    
    private boolean loadPage(Page page, int time) {
        for (PageFrame frame : pageFrames) {
            if (frame.isOccupied() && frame.getPage() != null && 
                frame.getPage().equals(page)) {
                frame.access(time);
                return true;
            }
        }
        
        checkForDuplicatePages(page.getProcess());
        
        for (PageFrame frame : pageFrames) {
            if (!frame.isOccupied()) {
                frame.loadPage(page, time);
                return true;
            }
        }
        
        PageFrame frameToReplace = replacementAlgorithm.selectPageToReplace(pageFrames, time);
        if (frameToReplace != null) {
            Page replacedPage = frameToReplace.getPage();
            
            if (replacedPage != null) {
                Process processToFree = replacedPage.getProcess();
                
                int pagesBefore = processToFree.getPagesInMemory();
                
                replacedPage.setLoaded(false);
                
                int pagesAfter = processToFree.getPagesInMemory();
                
                if (pagesBefore - pagesAfter != 1) {
                    forcePageCountSync(processToFree);
                }
            }
            
            frameToReplace.unloadPage();
            frameToReplace.loadPage(page, time);
            
            return true;
        }
        
        return false;
    }

    private void forcePageCountSync(Process process) {
        int actualLoadedPages = 0;
        
        for (PageFrame frame : pageFrames) {
            if (frame.isOccupied() && frame.getPage() != null && 
                frame.getPage().getProcess().equals(process)) {
                actualLoadedPages++;
            }
        }
        
        int believedLoadedPages = 0;
        for (Page page : process.getPages()) {
            if (page.isLoaded()) {
                believedLoadedPages++;
            }
        }
        
        if (actualLoadedPages != believedLoadedPages) {
            
            for (Page page : process.getPages()) {
                boolean actuallyInMemory = false;
                
                for (PageFrame frame : pageFrames) {
                    if (frame.isOccupied() && frame.getPage() != null && 
                        frame.getPage().equals(page)) {
                        actuallyInMemory = true;
                        break;
                    }
                }
                
                if (page.isLoaded() != actuallyInMemory) {
                    page.setLoaded(actuallyInMemory);
                }
            }
            
        }
    }
    
    private void checkForDuplicatePages(Process process) {
        Map<Integer, List<PageFrame>> pageIdToFrames = new HashMap<>();
        
        for (PageFrame frame : pageFrames) {
            if (frame.isOccupied() && frame.getPage() != null && 
                frame.getPage().getProcess().equals(process)) {
                
                int pageId = frame.getPage().getPageId();
                pageIdToFrames.putIfAbsent(pageId, new ArrayList<>());
                pageIdToFrames.get(pageId).add(frame);
            }
        }
        
        // Eliminar duplicados (mantener solo el primer marco para cada página)
        for (Map.Entry<Integer, List<PageFrame>> entry : pageIdToFrames.entrySet()) {
            List<PageFrame> framesWithSamePage = entry.getValue();
            
            if (framesWithSamePage.size() > 1) {
                for (int i = 1; i < framesWithSamePage.size(); i++) {
                    PageFrame duplicateFrame = framesWithSamePage.get(i);
                    Page duplicatePage = duplicateFrame.getPage();
                    
                    duplicateFrame.unloadPage();
                    duplicatePage.setLoaded(false);
                }
            }
        }
    }

    public void checkAndCleanDuplicates() {
        Map<String, List<PageFrame>> duplicateMap = new HashMap<>();
        
        for (PageFrame frame : pageFrames) {
            if (frame.isOccupied() && frame.getPage() != null) {
                Page page = frame.getPage();
                String key = page.getProcess().getName() + "-P" + page.getPageId();
                duplicateMap.putIfAbsent(key, new ArrayList<>());
                duplicateMap.get(key).add(frame);
            }
        }
        
        for (Map.Entry<String, List<PageFrame>> entry : duplicateMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (int i = 1; i < entry.getValue().size(); i++) {
                    PageFrame duplicate = entry.getValue().get(i);
                    if (duplicate.getPage() != null) {
                        duplicate.getPage().setLoaded(false);
                    }
                    duplicate.unloadPage();
                }
            }
        }
        
    }
    
    public double getPageFaultRate() {
        int totalAccesses = pageFaults + pageHits;
        return totalAccesses > 0 ? (double) pageFaults / totalAccesses : 0.0;
    }
    
    public int getPageFaults() { return pageFaults; }
    public int getPageHits() { return pageHits; }
    public int getCurrentTime() { return currentTime; }
    public int getFrameSize() { return frameSize; } 
    
    public List<PageFrame> getPageFrames() {
        return Collections.unmodifiableList(pageFrames);
    }
    
    public void resetMetrics() {
        pageFaults = 0;
        pageHits = 0;
    }
    

    public void setPageFrames(List<PageFrame> pageFrames) {
        this.pageFrames = pageFrames;
    }

    public PageReplacementAlgorithm getReplacementAlgorithm() {
        return replacementAlgorithm;
    }

    public void setReplacementAlgorithm(PageReplacementAlgorithm replacementAlgorithm) {
        this.replacementAlgorithm = replacementAlgorithm;
    }

    public void setPageFaults(int pageFaults) {
        this.pageFaults = pageFaults;
    }

    public void setPageHits(int pageHits) {
        this.pageHits = pageHits;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }
    
    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;
    }
}
