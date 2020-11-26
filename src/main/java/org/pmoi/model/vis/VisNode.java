package org.pmoi.model.vis;

import com.google.gson.Gson;

public class VisNode {
    private long id;
    private final String label;
    private final String title;
    private final int group;
    private final String color;

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

}
