package com.sirajinhoapp.wificodeeditor;

/**
 * Created by siraj on 04.04.17.
 */
public class Config {

    private static Config current;
    public int listenPort;
    public String ip;
    public String workspacePath;
    public String lang;
    public boolean openInNewTab;
    public String workspacename = "wifiworkspace";
    public boolean running;

    public Config(String workspacePath) {
        listenPort = 8080;
        this.workspacePath = workspacePath;
        ip = "";
        lang = "Java";
        running = false;
        openInNewTab = false;
    }

    public static void init(String workspacePath) {
       current =  new Config(workspacePath);
    }

    public static Config getCurrent() {
        if(current != null) {
            return current;
        }
        return null;
    }
}
