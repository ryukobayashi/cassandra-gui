package org.apache.cassandra.node;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cassandra.dht.Range;

public class RingNode implements Serializable {
    private static final long serialVersionUID = 8351368757758010586L;

    private Map<Range, List<String>> rangeMap;
    private List<Range> ranges;
    private Set<String> liveNodes;
    private Set<String> deadNodes;
    private Map<String, String> loadMap;

    /**
     * @return the rangeMap
     */
    public Map<Range, List<String>> getRangeMap() {
        return rangeMap;
    }

    /**
     * @param rangeMap the rangeMap to set
     */
    public void setRangeMap(Map<Range, List<String>> rangeMap) {
        this.rangeMap = rangeMap;
    }

    /**
     * @return the ranges
     */
    public List<Range> getRanges() {
        return ranges;
    }

    /**
     * @param ranges the ranges to set
     */
    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    /**
     * @return the liveNodes
     */
    public Set<String> getLiveNodes() {
        return liveNodes;
    }

    /**
     * @param liveNodes the liveNodes to set
     */
    public void setLiveNodes(Set<String> liveNodes) {
        this.liveNodes = liveNodes;
    }

    /**
     * @return the deadNodes
     */
    public Set<String> getDeadNodes() {
        return deadNodes;
    }

    /**
     * @param deadNodes the deadNodes to set
     */
    public void setDeadNodes(Set<String> deadNodes) {
        this.deadNodes = deadNodes;
    }

    /**
     * @return the loadMap
     */
    public Map<String, String> getLoadMap() {
        return loadMap;
    }

    /**
     * @param loadMap the loadMap to set
     */
    public void setLoadMap(Map<String, String> loadMap) {
        this.loadMap = loadMap;
    }
}
