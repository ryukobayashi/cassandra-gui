package org.apache.cassandra;

import java.io.Serializable;
import java.util.Date;

public class Cell implements Serializable {
    private static final long serialVersionUID = 4517336493185234248L;

    private Key key;
    private String name;
    private String value;
    private Date date;

    public Cell() {
    }

    public Cell(Key key, String name, String value, Date date) {
        this.key = key;
        this.name = name;
        this.value = value;
        this.date = date;
    }

    /**
     * @return the key
     */
    public Key getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(Key key) {
        this.key = key;
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
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }
}
