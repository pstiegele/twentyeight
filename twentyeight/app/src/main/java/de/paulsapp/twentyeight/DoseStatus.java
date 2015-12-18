package de.paulsapp.twentyeight;

/**
 * Created by pstiegele on 16.12.2015.
 */
public enum DoseStatus {
    AN,AUS;

    private int asInt;
    private boolean asBoolean;

    static {
        AN.asInt = 1;
        AUS.asInt = 0;
    }
    static {
        AN.asBoolean = true;
        AUS.asBoolean = false;
    }
    public int getAsInt(){
        return asInt;
    }
    public boolean getAsBoolean(){
        return asBoolean;
    }
}
