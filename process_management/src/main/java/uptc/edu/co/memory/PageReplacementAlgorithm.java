package uptc.edu.co.memory;

import java.util.List;

public interface PageReplacementAlgorithm {
    PageFrame selectPageToReplace(List<PageFrame> pageFrames, int currentTime);
    String getName();
}
