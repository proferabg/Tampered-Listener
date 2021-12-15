package com.haxzz.tl.objects.wrapper;

import java.util.ArrayList;
import java.util.List;

public class Logs {

	private int placeholder;
	List<LogObject> logs = new ArrayList<LogObject>();
	
	public Logs(){}

    public Logs(int placeholder, List<LogObject> logs) {
        super();
        this.placeholder = placeholder;
        this.logs = logs;
    }

    public List<LogObject> getLogs() {
        return logs;
    }
    public int getPlaceholder(){
    	return placeholder;
    }
}
