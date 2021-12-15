package com.haxzz.tl.kvchecker;

import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.haxzz.tl.enums.KV_CHECK;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class KVChecker {
	
	private Timer KVTimer;
	//private Timer TaskTimer;
	private static Map<ClientData, KVData> Queue = new HashMap<ClientData, KVData>();
	
	public KVChecker(){
		Security.addProvider(new BouncyCastleProvider());
		KVLogic.LoadFiles();
		/* Individual Timer */
		KVTimer = new Timer();
		KVTimer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				Map<ClientData, KVData> QueueTemp = new HashMap<ClientData, KVData>(Queue);
				for(Entry<ClientData, KVData> entry : QueueTemp.entrySet()){
					//get client
					ClientData cd = entry.getKey();
					//get kv status
					int check = cd.getKVStatus();
					if(check == 0 || check == 2){
						//get jv data
						KVData kvd = entry.getValue();
						try {
							KV_CHECK status = KVLogic.CheckKV(kvd.getKV(), entry.getKey());
							//kv invalid.... lets not check again
							if(status == KV_CHECK.INVALID){
								Queue.remove(entry.getKey());
							}
							//kv error... lets check 3 times total
							else if (status == KV_CHECK.ERROR){
								//increment check
								kvd.setTimes(kvd.getTimes()+1);
								if(kvd.getTimes() <= 2){
									Queue.put(entry.getKey(), kvd);
								} else {
									Queue.remove(entry.getKey());
								}
							} else {
								cd.setKVStatus(status.getValue());
								cd.updateKVStatus();
								Queue.remove(entry.getKey());
							}
						} catch (Exception e) {
							Logger.error(e,  "KVChecker.Timer.run()");
							//retask kv
							kvd.setTimes(kvd.getTimes()+1);
							if(kvd.getTimes() <= 2){
								Queue.put(entry.getKey(), kvd);
							} else {
								Queue.remove(entry.getKey());
							}
						}
					} else {
						Queue.remove(entry.getKey());
					}
				}
				if(QueueTemp.size() > 5){
					Logger.logWithTime("%g_KV Checking Task Completed.\r\n", "Global", "", "");
				}
			}
		}, 5000, 3000);
		/* Task Timer */
		/*TaskTimer = new Timer();
		//get next 4 hour interval
		DateTime start = new DateTime().withTimeAtStartOfDay();
		DateTime now = DateTime.now();
		while(now.isAfter(start)){
			start = start.plusHours(8);
		}
		//back to timer
		TaskTimer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				TaskAllKVs();
			}
			
		}, start.toDate(), 28800000L);*/
		
	}
	
	public void AddToQueue(ClientData client, byte[] KV){
		if(!Queue.containsKey(client)){
			Queue.put(client, new KVData(KV, 0));
		}
	}
	
	public void TaskAllKVs(){
		Logger.logWithTime("%r_CHECKING ALL KVs! Lag Expected.\r\n", "Global", "", "");
		List<String> CPUKeys = Utilities.getAllClients();
		for(String s : CPUKeys){
			ClientData cd = new ClientData(s);
			DateTime lastPing = new DateTime(cd.getLastPing()*1000);
			DateTime now = DateTime.now(DateTimeZone.UTC);
			int check = cd.getKVStatus();
			if((check == 0 || check == 2) && now.minusHours(8).isBefore(lastPing) && cd.getKVSerial().matches("\\d+")){
				Queue.put(cd, new KVData(cd.getKV(), 0));
			}
		}
		Logger.logWithTime("%r_"+Queue.size()+" KVs in queue.\r\n", "Global", "", "");
	}

}

class KVData {
	private byte[] KV = new byte[16384];
	private int times = 0;
	
	public KVData(byte[] KV, int times){
		this.KV = KV;
		this.times = times;
	}

	public byte[] getKV() {
		return KV;
	}

	public void setKV(byte[] KV) {
		this.KV = KV;
	}

	public int getTimes() {
		return times;
	}

	public void setTimes(int times) {
		this.times = times;
	}
	
	
}
