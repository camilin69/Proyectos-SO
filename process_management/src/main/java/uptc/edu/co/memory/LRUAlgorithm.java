package uptc.edu.co.memory;

import java.util.*;

public class LRUAlgorithm implements PageReplacementAlgorithm {
    
    @Override
    public PageFrame selectPageToReplace(List<PageFrame> pageFrames, int currentTime) {
        PageFrame leastRecent = null;
        int oldestAccessTime = Integer.MAX_VALUE;
        
        for (PageFrame frame : pageFrames) {
            if (frame.isOccupied() && frame.getPage() != null) {
                if (frame.getLastAccessTime() < oldestAccessTime) {
                    oldestAccessTime = frame.getLastAccessTime();
                    leastRecent = frame;
                }
            }
        }
        
        return leastRecent;
    }
    
    @Override
    public String getName() {
        return "LRU";
    }
}