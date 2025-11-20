package uptc.edu.co.memory;

import java.util.*;

public class FIFOAlgorithm implements PageReplacementAlgorithm {
    
    @Override
    public PageFrame selectPageToReplace(List<PageFrame> pageFrames, int currentTime) {
        PageFrame oldest = null;
        int oldestLoadTime = Integer.MAX_VALUE;
        
        for (PageFrame frame : pageFrames) {
            if (frame.isOccupied() && frame.getPage() != null) {
                if (frame.getLoadTime() < oldestLoadTime) {
                    oldestLoadTime = frame.getLoadTime();
                    oldest = frame;
                }
            }
        }
        
        return oldest;
    }
    
    @Override
    public String getName() {
        return "FIFO";
    }
}