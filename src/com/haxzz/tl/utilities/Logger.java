package com.haxzz.tl.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.objects.Colors;
import com.haxzz.tl.objects.wrapper.LogObject;
import com.haxzz.tl.objects.wrapper.Logs;
import com.haxzz.tl.objects.wrapper.WebClient;

public class Logger {
	
	private static List<LogObject> log_history = new ArrayList<LogObject>();
	
	public static void logWithTime(String message, String Name, String CPUKey, String IP){
		//add timestamp
		message = Utilities.getTimestamp() + " " + message;
		//do colors
		String web = Colors.parseWebColors(message).replace("137.74.114.158", "x.x.x.x");
		message = Colors.parseColors(message);
		//print with colors
		System.out.println(message);
		//strip color
		message = Colors.stripColors(message);
		//print to file
		printToInfo(message);
		//save to log for web
		log_history.add(new LogObject(Name, CPUKey, IP, web));
		if(log_history.size() > 151){
			List<LogObject> newlog = new ArrayList<LogObject>();
			for (int i = 1; i < 152; i++){
				newlog.add(log_history.get(i));
			}
			log_history = newlog;
		}
		EmitLogs();
	}
	
	
	public static void log(String message, String Name, String CPUKey, String IP){
		//do colors
		String web = Colors.parseWebColors(message).replace("178.32.56.60", "x.x.x.x");
		message = Colors.parseColors(message);
		//print with colors
		System.out.println(message);
		//strip color
		message = Colors.stripColors(message);
		//print to file
		printToInfo(message);
		//save to log for web
		log_history.add(new LogObject(Name, CPUKey, IP, web));
		if(log_history.size() > 151){
			List<LogObject> newlog = new ArrayList<LogObject>();
			for (int i = 1; i < 152; i++){
				newlog.add(log_history.get(i));
			}
			log_history = newlog;
		}
		EmitLogs();
	}
	
	public static void printToInfo(String string){
		string = string + "\r\n";
		try {
			createDirFiles();
			Files.write(Paths.get("logs/info.txt"), string.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			error(e, "Logger.printToInfo()");
		}
	}
	
	public static void error(Exception e, String message){
		log("Error in Console: " + message, "Global", "", "");
		try {
			createDirFiles();
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File("logs/error.txt"), true));
			e.printStackTrace(pw);
			e.printStackTrace();
			pw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void createDirFiles(){
		try {
			//create dir
			Files.createDirectories(Paths.get("logs"));
			//create files if not exist
			Files.createFile(Paths.get("logs/error.txt"));
			Files.createFile(Paths.get("logs/info.txt"));
			//check size. if one or the other pass 50MB lets rename them.
			File info = new File("logs/info.txt");
			File error = new File("logs/error.txt");
			if(info.length()/1048576 > 50 || error.length()/1048576 > 50){
				info.renameTo(new File("logs/info-"+System.currentTimeMillis()+".txt"));
				error.renameTo(new File("logs/error-"+System.currentTimeMillis()+".txt"));
			}
			//create files if not exist after move
			Files.createFile(Paths.get("logs/error.txt"));
			Files.createFile(Paths.get("logs/info.txt"));
			
		} catch(IOException e){
			
		}
	}
	
	public static void EmitLogs(){
		WebWrapper wrapper = TamperedListener.getWebWrapper();
		if(wrapper != null && wrapper.isStarted()) {
			List<WebClient> tmp = new ArrayList<WebClient>(wrapper.getClients());
			for (WebClient wc1 : tmp){
				if(!wc1.hasChallenged()) continue;
				wc1.getSockClient().sendEvent("logs", new Logs(0, log_history));
			}
		}
	}
	
}
