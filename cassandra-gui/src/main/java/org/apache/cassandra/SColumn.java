package org.apache.cassandra;

import java.io.Serializable;
import java.util.Map;

public class SColumn implements Serializable {
    private static final long serialVersionUID = -8041985483479505351L;

    private String name;
    private Map<String, Cell> cells;

    public SColumn() {
    }

    public SColumn(String name, Map<String, Cell> cells) {
        this.name = name;
        this.cells = cells;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the cells
     */
    public Map<String, Cell> getCells() {
        return cells;
    }

    /**
     * @param keys the keys to set
     */
    public void setCells(Map<String, Cell> cells) {
        this.cells = cells;
    }
}
