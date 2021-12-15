package com.haxzz.tl.utilities;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.objects.wrapper.MetricsPacket;
import com.haxzz.tl.objects.wrapper.WebClient;
import com.jezhumble.javasysmon.CpuTimes;
import com.jezhumble.javasysmon.JavaSysMon;

public class Metrics {
	
	private JavaSysMon monitor;
	private static Map<String, DateTime> pings = new HashMap<String, DateTime>();
	MetricsPacket mpack;
	private Timer runner1, runner2, runner3, runner4;
	private CpuTimes now, prev;
	private double cpu_last;
	private int max = 0;
	
	public Metrics(){
		monitor = new JavaSysMon();
        if (!monitor.supportedPlatform()) {
        	Logger.logWithTime("%br_Performance monitoring unsupported! Disabling Metrics.\r\n", "Global", "", "");
            monitor = null;
            return;
        }

        Logger.logWithTime("%bm_Metrics Started.\r\n", "Global", "", "");
            
        now = monitor.cpuTimes();
        
        mpack = new MetricsPacket(0,0,0,0,0,0,0,0,0,"","");
		
		runner1 = new Timer();
		runner2 = new Timer();
		runner3 = new Timer();
		runner4 = new Timer();
		//This is where we get the stats
		runner1.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				long sys_freemem = Long.parseLong(getSystemInfo("getFreePhysicalMemorySize").toString());
				long sys_totalmem = Long.parseLong(getSystemInfo("getTotalPhysicalMemorySize").toString());
				long sys_usedmem = sys_totalmem - sys_freemem;
				mpack.setLoad(cpu_last);
				mpack.setCores(monitor.numCpus());
				mpack.setProg_Freemem(Math.round(Runtime.getRuntime().freeMemory() / 1048576));
				mpack.setProg_Usedmem(Math.round((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576));
				mpack.setProg_Totalmem(Math.round(Runtime.getRuntime().totalMemory() / 1048576));
				mpack.setSys_freemem(Utilities.round(sys_freemem/1073741824.0, 2));
				mpack.setSys_usedmem(Utilities.round(sys_usedmem/1073741824.0, 2));
				mpack.setSys_totalmem(Utilities.round(sys_totalmem/1073741824.0, 2));
				int x = getClients();
				if(x > max) {
					max = x;
					Utilities.updateMaxOnline(max);
				};
				mpack.setClients(x);
				mpack.setOs(monitor.osName());
				mpack.setJavaver(Runtime.getRuntime().getClass().getPackage().getImplementationVersion());
			}
		}, 0, 100);

		//This is where we send the stats
		runner2.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				WebWrapper wrapper = TamperedListener.getWebWrapper();
				if(wrapper != null && wrapper.isStarted()){
					List<WebClient> tmp = new ArrayList<WebClient>(wrapper.getClients());
					for(WebClient wc : tmp){
						if(!wc.hasChallenged()) continue;
						wc.getSockClient().sendEvent("stats", mpack);
					}
				}
			}
		}, 0, 1000);
		
		//This calls garbage collect every 10 minutes
		runner3.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				System.gc();
			}
		}, 600000, 600000);
		
		//this gets the cpu load
		runner4.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				prev = now;
				now = monitor.cpuTimes();
				cpu_last = Utilities.round(now.getCpuUsage(prev) * 100.0, 2);
			}
		}, 0, 1000);
	}

	protected int getClients() {
		int tmp = 0;
		DateTime now = new DateTime(System.currentTimeMillis());
		if (!pings.isEmpty()){
			Map<String, DateTime> tmpPings = new HashMap<String, DateTime>(pings);
			for (Entry<String, DateTime> entry : tmpPings.entrySet()) {
				if (entry.getValue().isAfter(now.minusSeconds(70))){
					tmp += 1;
				} else {
					pings.remove(entry.getKey());
				}
			}
		}
		return tmp;
	}
	
	public void logClient(String cpukey){
		pings.put(cpukey, new DateTime(System.currentTimeMillis()));
	}
	
	private Object getSystemInfo(String param) {
		try {
	        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
	        for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {
	            method.setAccessible(true);
	            if (method.getName().equalsIgnoreCase(param) && Modifier.isPublic(method.getModifiers())) {
	            	return method.invoke(operatingSystemMXBean);
	            }
	        }
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	
	}
	
}
