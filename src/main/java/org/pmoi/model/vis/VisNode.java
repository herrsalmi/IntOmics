package org.pmoi.model.vis;

import com.google.gson.Gson;

public class VisNode {
    private long id;
    private String label;
    private String title;
    private int group;
    private String color;

    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public VisNode(long id, String label, String title, int group, String color) {
        this.id = id;
        this.label = label;
        this.title = title;
        this.group = group;
        this.color = color;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}