package ro.cofi.relicdb;

import com.google.gson.JsonObject;

public class LoadedDB {

    private final JsonObject data;
    private final DBChoice dbChoice;

    public LoadedDB(JsonObject data, DBChoice dbChoice) {
        this.data = data;
        this.dbChoice = dbChoice;
    }

    public JsonObject getData() {
        return data;
    }

    public DBChoice getDBChoice() {
        return dbChoice;
    }
}
