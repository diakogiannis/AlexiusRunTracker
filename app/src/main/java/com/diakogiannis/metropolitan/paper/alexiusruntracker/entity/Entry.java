package com.diakogiannis.metropolitan.paper.alexiusruntracker.entity;

import java.io.Serializable;

public class Entry implements Serializable {
    private int id;
    private String runEntry;

    public Entry() {
        //default
    }

    public Entry(String runEntry) {
        this.runEntry = runEntry;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRunEntry() {
        return runEntry;
    }

    public void setRunEntry(String runEntry) {
        this.runEntry = runEntry;
    }
}
