package org.pmoi.util;

import com.google.gson.*;
import org.pmoi.model.vis.VisEdge;

import java.lang.reflect.Type;
import java.util.List;

public class VisEdgeAdapter implements JsonSerializer<VisEdge>{

    @Override
    public JsonElement serialize(VisEdge visEdge, Type type, JsonSerializationContext jsonSerializationContext) {
        return getAsJsonObject(visEdge);
    }

    public static JsonArray getAsJsonArray(List<VisEdge> edges){
        JsonArray jsonArray = new JsonArray();
        for(VisEdge visEdge : edges){
            jsonArray.add(getAsJsonObject(visEdge));
        }
        return jsonArray;
    }

    private static JsonObject getAsJsonObject(VisEdge visEdge){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("from",visEdge.getFrom().getId());
        jsonObject.addProperty("to",visEdge.getTo().getId());
        jsonObject.addProperty("label",visEdge.getLabel());
        jsonObject.addProperty("arrows",visEdge.getArrows());
        return jsonObject;
    }
}