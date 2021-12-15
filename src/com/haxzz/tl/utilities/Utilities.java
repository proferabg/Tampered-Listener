package com.haxzz.tl.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.CLI_CMD_TYPE;
import com.haxzz.tl.enums.CONSOLE_TYPE;
import com.haxzz.tl.enums.SECURITY_FLAG;
import com.haxzz.tl.enums.STATUS_TYPE;
import com.haxzz.tl.enums.TOKEN_STATUS;
import com.haxzz.tl.objects.ClientCommand;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.objects.TitleInfo;
import com.haxzz.tl.objects.config.Patch;
import com.haxzz.tl.objects.wrapper.MessageObject;
import com.haxzz.tl.objects.wrapper.WebClient;

@SuppressWarnings("deprecation")
public class Utilities {
	

	private static Map<String, String> TitleIds = new HashMap<String, String>();
	
	public static String getExpireTime(ClientData Client){
		DateFormat df = new SimpleDateFormat("MMM d, yyyy h:mm:ss aaa");
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		Date d = new Date(Client.getRemaining()*1000);
		return df.format(d);
	}
	
	public static String getTimestamp(){
		DateFormat df = new SimpleDateFormat("MMM d, yyyy h:mm:ss aaa");
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		Date d = new Date();
		df.format(d);
		return "[" + df.format(d) + "]";
	}
	
	public static String bytesToHex(byte[] bytes) {
		
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}
	
	public static byte[] hexToBytes(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public static byte[] trimZeros(byte[] input){
		int i = input.length;
		while (i-- > 0 && input[i] == 0) {}

		byte[] output = new byte[i+1];
		System.arraycopy(input, 0, output, 0, i+1);
		return output;
	}
	
	public static byte[] randomPadding(){
		byte[] padding = new byte[8];
		new Random().nextBytes(padding);
		return padding;
	}
	
	public static boolean isDbConnected(){
		try {
			if(TamperedListener.getDB() == null) return false;
			Connection con = TamperedListener.getDB().openConnection();
			if(con == null || con.isClosed()){
				return TamperedListener.InitializeDB();
			} else {
				con.close();
				return true;
			}
		} catch (SQLException e) {
			Logger.error(e, "Utilities.isDbConnected()");
			return TamperedListener.InitializeDB();
		}
	}
	
	public static void PoolTitles(){
		try {
			if (!TitleIds.isEmpty()) TitleIds.clear();
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT `hexid`, `title` FROM `titleids` WHERE 1");
				while (res.next()) {
					TitleIds.put(res.getString("hexid"), res.getString("title"));
				}
				res.close(); statement.close(); conn.close();
			} else {
				Logger.log("Pooling TitleIds Failed: Database not connected.", "Global", "", "");
			}
		} catch (SQLException e) {
			Logger.error(e, "Utilities.PoolTitles()");
		}
	}
	
	public static String GetTitle(String HexId){
		if(TitleIds.containsKey(HexId)){
			return TitleIds.get(HexId);
		} else {
			return fetchTitle(HexId);
		}
	}
	
	public static String fetchTitle(String HexId){
		try {
			//get data
			String json = readURLFirefox(TamperedListener.getIOOrigin() + "/inc/api.php?action=getTitleInfo&titleid="+HexId);
			Gson gson = new Gson();        
		    TitleInfo ti = gson.fromJson(json, TitleInfo.class);
		    
		    //check null
			if(ti == null || ti.Name == null){
				return "null";
			}
			
			//try insert
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				PreparedStatement statement = conn.prepareStatement("INSERT INTO `titleids`(`hexid`, `title`, `description`, `developer`, `boxart`) VALUES (?,?,?,?,?)");
				statement.setString(1, HexId);
				statement.setString(2, ti.Name);
				statement.setString(3, ti.Description);
				statement.setString(4, ti.Developer);
				statement.setString(5, ti.Image);
				statement.execute();
				statement.close(); conn.close(); 
			} 
			
			PoolTitles();
			
			//return name
			return ti.Name;
			
		} catch (Exception e) {
			Logger.error(e, "Utilities.fetchTitle()");
		}
		
		return "null";
	}

	public static Map<String, String> getTitleIds() {
		return TitleIds;
	}
	
	public static SECURITY_FLAG checkFlags(ClientData client, String Session, String Genealogy, boolean CheckSession){
		double DBVersion = 0.0;
		boolean Gene = false, XEXHash = false;
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'version' LIMIT 1");
				while (res.next()) {
					DBVersion = res.getDouble("value");
				}
				res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'xexhash' LIMIT 1");
				while (res.next()) {
					XEXHash = res.getBoolean("value");
				}
				res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'genealogy_hash' LIMIT 1");
				while (res.next()) {
					Gene = res.getBoolean("value");
				}

				res.close(); statement.close(); conn.close();
				
				//check blacklist first
				if(client.getIsInDB() && client.isBlacklisted()){
					return SECURITY_FLAG.BLACKLISTED;
				}
				
				//check version
				if(client.getVersion() < DBVersion  && !client.isDeveloper()){
					return SECURITY_FLAG.UPDATE_AVAIL;
				}

				//check genealogy hash
				String ghash = Utilities.getGeneologyHash(Utilities.hexToBytes(Genealogy), client.getCPUKey());
				if(client.getIsInDB() && client.getGenealogyHash().equals("") && CheckSession){
					client.setGenealogyHash(ghash);
				} 
				else if(client.getIsInDB() && !client.getGenealogyHash().equalsIgnoreCase(ghash) && CheckSession){
					client.setFails(client.getFails()+1);
					Logger.logWithTime("%br_" + client.getName() + " FAILED THE GENEALOGY HASH!!!!", client.getName(), client.getCPUKey(), client.getIP());
					if(Gene) return SECURITY_FLAG.GENEALOGY_FAILED;
				}
				
				//check xex hash
				if(!client.isDeveloper()){
					//call api
					int result = GetXEXHashResult(TamperedListener.getAPIURL(), TamperedListener.getAPIKey(), client, CheckSession);
					if(result == 0){
						client.setFails(client.getFails()+1);
						Logger.logWithTime("%br_" + client.getName() + " FAILED THE XEX HASH!!!!", client.getName(), client.getCPUKey(), client.getIP());
						if(XEXHash) return SECURITY_FLAG.UPDATE_AVAIL;
					}
				}
				
				//check session
				if(client.getIsInDB() && CheckSession && !Session.equalsIgnoreCase(client.getSession())){
					client.setFails(client.getFails()+1);
					return SECURITY_FLAG.SESSION_FAILED;
				}
				
				//all checks passed
				return SECURITY_FLAG.PASSED;
			} else {
				Logger.log("Check Flags: Database Not Connected!", "Global", "", "");
				//error but pass challenge for client sake
				return SECURITY_FLAG.PASSED;
			}
		} catch (SQLException e) {
			Logger.error(e, "Utilities.checkFlags()");
			//error but pass challenge for client sake
			return SECURITY_FLAG.PASSED;
		}
	}
	
	public static int GetXEXHashResult(String APIURL, String APIKey, ClientData Client, boolean CheckSession){
		try {
			String tmp = Utilities.readURLFirefox(APIURL + "?action=checkXex" + ((TamperedListener.isDeveloper()) ? "Dev" : "") + "&sess=" + ((CheckSession) ? Client.getSession() : "0000000000000000") + "&cpu=" + Client.getCPUKey() + "&resulthash=" + Client.getXEXHash() + "&version=" + Client.getVersion());
			if(tmp == null ||  tmp.length() < 1 ||  tmp.contains("0xC0000040") || tmp.contains("Please make") || tmp.contains("This API Key")){
				//api call failed lets retry
				Logger.logWithTime("%br_API Call Returned Error. Retrying...", Client.getName(), Client.getCPUKey(), Client.getIP());
				tmp = Utilities.readURLFirefox(APIURL + "?action=checkXexDev&sess=" + ((CheckSession) ? Client.getSession() : "0000000000000000") + "&cpu=" + Client.getCPUKey() + "&resulthash=" + Client.getXEXHash() + "&version=" + Client.getVersion());
				//api call failed again.....
				if(tmp == null ||  tmp.length() < 1 ||  tmp.contains("0xC0000040") || tmp.contains("Please make") || tmp.contains("This API Key")){
					Logger.logWithTime("%br_API Call Failed.", Client.getName(), Client.getCPUKey(), Client.getIP());
					Utilities.WritePacketBinary("XEXHash.txt", Client.getCPUKey(), "API FAILED!".getBytes());
					return 1;
				} 
			}
			Utilities.WritePacketBinary("XEXHash.txt", Client.getCPUKey(), tmp.getBytes());
			return Integer.parseInt(tmp.replaceAll("[^0-9]+", ""));
		} catch (Exception e) {
			Logger.error(e, "Utilities.GetXEXHashResult()");
			return 1;
		}
	}
	
	public static STATUS_TYPE getStatusFromFlag(SECURITY_FLAG flag){
		if(flag == SECURITY_FLAG.BLACKLISTED){
			return STATUS_TYPE.BLACKLIST;
		} 
		else if (flag == SECURITY_FLAG.HASH_FAILED || flag == SECURITY_FLAG.GENEALOGY_FAILED){
			//Logger.logWithTime("%br_Failed " + ((flag == SECURITY_FLAG.GENEALOGY_FAILED) ? "Genealogy" : "XEX") + " Hash!");
			return STATUS_TYPE.TAMPER;
		} 
		else if (flag == SECURITY_FLAG.SESSION_FAILED){
			return STATUS_TYPE.INVALID_SESSION;
		}
		else if (flag == SECURITY_FLAG.UPDATE_AVAIL){
			return STATUS_TYPE.UPDATE;
		}
		else if (flag == SECURITY_FLAG.PASSED){
			return STATUS_TYPE.NO_AUTH;
		}
		else {
			return STATUS_TYPE.ERROR;
		}
	}
	
	public static boolean isBadFlag(SECURITY_FLAG flag){
		if(flag == SECURITY_FLAG.BLACKLISTED || 
				flag == SECURITY_FLAG.HASH_FAILED || 
				flag == SECURITY_FLAG.SESSION_FAILED || 
				flag == SECURITY_FLAG.GENEALOGY_FAILED ||
				flag == SECURITY_FLAG.UPDATE_AVAIL){
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isFreeTime(){
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'free' LIMIT 1");
				while(res.next()){
					res.close(); statement.close(); conn.close();
					return res.getBoolean("value");
				}
				res.close(); statement.close(); conn.close();
				return false;
			} else {
				return false;
			}
		} catch (SQLException e) {
			Logger.error(e, "Utilities.isFreeTime()");
			return false;
		}
	}
	
	public static void deleteCommand(int Id){
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				statement.execute("DELETE FROM `commands` WHERE `id` = '"+Id+"'");
				statement.close(); conn.close();
			}
		} catch (SQLException e){
			Logger.error(e, "Utilities.deleteCommand()");
		}
	}
	
	public static ClientCommand getCommand(String CPUKey){
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT * FROM `commands` WHERE `cpukey` = '"+ CPUKey+"' LIMIT 1");
				while(res.next()){
					int Id = res.getInt("id");
					CLI_CMD_TYPE Type = CLI_CMD_TYPE.getValue(res.getInt("type"));
					String Message = "", Button = "";
					if(Type == CLI_CMD_TYPE.MSGBOX || Type == CLI_CMD_TYPE.XNOTIFY){
						Message = res.getString("message");
						Button = res.getString("button");
					}
					res.close(); statement.close(); conn.close();
					return new ClientCommand(Id, Type, Message, Button, CPUKey);
				}
				res.close(); statement.close(); conn.close();
			}
			return new ClientCommand();
		} catch (SQLException e){
			Logger.error(e, "Utilities.getCommand()");
			return new ClientCommand();
		}
	}
	
	public static TOKEN_STATUS isTokenValid(String Token){
		try {
			if(!Token.matches("[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}")){
				return TOKEN_STATUS.BAD_FORMAT;
			}
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT * FROM `tokens` WHERE `token` =  '"+ Token +"' LIMIT 1");
				if(res.next()){
					boolean enabled = res.getBoolean("enabled");
					if(!enabled){
						res.close(); statement.close(); conn.close();
						return TOKEN_STATUS.NOT_EXIST;
					}
					boolean rdm = res.getBoolean("redeemed");
					if(rdm){
						res.close(); statement.close(); conn.close();
						return TOKEN_STATUS.ALRDY_RDM;
					} else {
						res.close(); statement.close(); conn.close();
						return TOKEN_STATUS.EXISTS;
					}
				} else {
					res.close(); statement.close(); conn.close();
					return TOKEN_STATUS.NOT_EXIST;
				}
			}
			return TOKEN_STATUS.ERROR;
		} catch (SQLException e){
			Logger.error(e, "Utilities.isTokenValid()");
			return TOKEN_STATUS.ERROR;
		}
	}
	
	public static int getKVUsedOn(ClientData client){
		try {
			if(!client.getKVSerial().matches("\\d+")) return 0;
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT count(*) as 'count' FROM `clients` WHERE `kvserial` = '"+client.getKVSerial()+"' AND `cpukey` != '" + client.getCPUKey() + "'");
				if(res.next()){
					return res.getInt("count");
				}
			}
			return 0;
		} catch (SQLException e){
			Logger.error(e, "Utilities.getKVUsedOn()");
			return 0;
		}
	}
	
	public static int GetTokenDays(String Token){
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT * FROM `tokens` WHERE `token` =  '"+ Token +"' LIMIT 1");
				if(res.next()){
					boolean enabled = res.getBoolean("enabled");
					if(!enabled){
						res.close(); statement.close(); conn.close();
						return TOKEN_STATUS.NOT_EXIST.getValue();
					}
					boolean rdm = res.getBoolean("redeemed");
					//check already redeemed
					if (rdm){
						res.close(); statement.close(); conn.close();
						return TOKEN_STATUS.ALRDY_RDM.getValue();
					}

					res.close(); statement.close(); conn.close();
					return res.getInt("days");
				}
				res.close(); statement.close(); conn.close();
			}
			return TOKEN_STATUS.ERROR.getValue();
		} catch (SQLException e){
			Logger.error(e, "Utilities.redeemToken()");
			return TOKEN_STATUS.ERROR.getValue();
		}
	}
	
	public static TOKEN_STATUS redeemToken(ClientData Client, String Token){
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT * FROM `tokens` WHERE `token` =  '"+ Token +"' LIMIT 1");
				if(res.next()){
					int id = res.getInt("id");
					boolean enabled = res.getBoolean("enabled");
					boolean trial = res.getBoolean("trial");
					boolean rdm = res.getBoolean("redeemed");
					//check enabled
					if(!enabled){
						res.close(); statement.close(); conn.close();
						return TOKEN_STATUS.NOT_EXIST;
					}
					//has client used trial?
					if(trial && Client.hasUsedTrial()){
						res.close(); statement.close(); conn.close();
						return TOKEN_STATUS.TRIAL_ALRDY_RDM;
					}
					//check already redeemed
					if (rdm){
						res.close(); statement.close(); conn.close();
						return TOKEN_STATUS.ALRDY_RDM;
					}
					if(trial) Client.setTrialUsed(true);
					int days = res.getInt("days");
					int rdays = res.getInt("reserve_days");
					if(days == 99999){
						Client.setLifetime(true);
					} else {
						if(rdays != 0){
							Client.setDays(Client.getDays()+rdays);
						}
						if(days != 0){
							DateTime now = DateTime.now(DateTimeZone.UTC);
							DateTime expire = new DateTime(Client.getRemaining() * 1000L);
							if(now.isAfter(expire)) expire = now;
							expire = expire.plusDays(days);
							Client.setRemaining(expire.getMillis()/1000L);
						}
					}
					Statement statement2 = conn.createStatement();
					statement2.setEscapeProcessing(true);
					statement2.executeUpdate("UPDATE `tokens` SET `redeemed` = 1,`redeemed_by` = '" + Client.getName() + " - " + Client.getCPUKey() + "',`redeemed_date` = '" + (System.currentTimeMillis()/1000) + "' WHERE `id` = '"+id+"'");
					res.close(); statement.close(); statement2.close(); conn.close();
					return TOKEN_STATUS.REDEEMED;
				}
				res.close(); statement.close(); conn.close();
			}
			return TOKEN_STATUS.ERROR;
		} catch (SQLException e){
			Logger.error(e, "Utilities.redeemToken()");
			return TOKEN_STATUS.ERROR;
		}
	}
	
	public static int getMaxOnline(){
		int max = 0;
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'max_online' LIMIT 1");
				if (res.next()) {
					max = res.getInt("value");
					
				}
				res.close(); statement.close(); conn.close();
			}
			return max;
		} catch (SQLException e){
			Logger.error(e, "Utilities.getMaxOnline");
			return 0;
		}
	}
	
	public static void updateMaxOnline(int Current){
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT `value` FROM `options` WHERE `object` = 'max_online' LIMIT 1");
				if (res.next()) {
					int max = res.getInt("value");
					if(Current > max){
						Statement statement2 = conn.createStatement();
						statement2.setEscapeProcessing(true);
						statement2.executeUpdate("UPDATE `options` SET `value` = '" + Current + "' WHERE `object` = 'max_online'");
						statement2.close();
					}
				}
				res.close(); statement.close(); conn.close();
			}
		} catch (SQLException e){
			Logger.error(e, "Utilities.updateMaxOnline");
		}
	}
	
	public static String readUrl(String urlString) throws Exception {
	    BufferedReader reader = null;
	    try {
	        URL url = new URL(urlString);
	        reader = new BufferedReader(new InputStreamReader(url.openStream()));
	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        return buffer.toString();
	    } finally {
	        if (reader != null)
	            reader.close();
	    }
	}
	
	public static String readURLFirefox(String string) throws Exception {
		try {
			//start time
			long x = System.currentTimeMillis();
			//set url
			URL url = new URL(string);
			//initialize connection
			HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
			httpcon.setConnectTimeout(10000);
			httpcon.addRequestProperty("User-Agent", "Mozilla/5.0");
			//create reader
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
			//create string buffer
			StringBuffer buffer = new StringBuffer();
			//read characters
			int read;
			char[] chars = new char[1024];
		    while ((read = reader.read(chars)) != -1){
		    	buffer.append(chars, 0, read); 
		    }
		    //log time taken
		    Logger.logWithTime("%w_URL: '" + string + "' Took " + (System.currentTimeMillis() - x) + " ms to respond.", "URL", "", "");
		    //close reader
            reader.close();
            //return data
	        return buffer.toString();
		} catch (Exception e){
			Logger.logWithTime("%w_URL: '" + string + "' Timed Out!", "URL", "", "");
	        return "";
		}
	}
	
	public static boolean isType1KV(byte[] kvSignature){
		int Count = 0;
		for (int i = 0; i < 0xFF; i++){
			if (kvSignature[i] == 0){
				Count++;
			}
		}
		if (Count > 0x50){
			return true;
		} else {
			return false;
		}
	}
	
	// this should pull real console type but doesnt always for faulty kvs...
	public static CONSOLE_TYPE getConsoleType(byte[] KVData, boolean type1){
		byte type = (byte) (((KVData[0] << 4) & 0xF0) | (KVData[1] & 0x0F));
	  	if(type < 0x10) {
	  		if (type == 0 || !type1) return CONSOLE_TYPE.CORONA;
	  		return CONSOLE_TYPE.XENON;
	  	}
	  	else if(type < 0x14) return CONSOLE_TYPE.ZEPHYR;
	  	else if(type < 0x18) return CONSOLE_TYPE.FALCON;
	  	else if(type < 0x52) return CONSOLE_TYPE.JASPER;
	  	else if(type < 0x58) return CONSOLE_TYPE.TRINITY;
	  	else return CONSOLE_TYPE.CORONA;
	}
	
	/*
	@Deprecated
	public static int getConsoleTypeOld(byte[] KVData){
		ByteBuffer Tmp = ByteBuffer.wrap(KVData);
		int Type = Tmp.getInt();
		if(Type == 0x39383131){ //xenon
			return 0x10B0524;
		}
		else if(Type == 0x39383130){ //zephyr
			return 0x10C0AD0;
		}
		else if(Type == 0x39393430){ //falcon
			return 0x10C0AD8;
		}
		else if(Type == 0x39393564){ //jasper
			return 0x10C0FFB;
		}
		else if(Type == 0x39386662){ //trinity
			return 0x304000D;
		}
		else if(Type == 0x39386661){ //corona
			return 0x304000E;
		} 
		else { //corona / winchester
			return 0x304000E;
		}
	}
	
	
	
	@Deprecated
	public static String getConsoleTypeString(byte[] KVData){
		ByteBuffer Tmp = ByteBuffer.wrap(KVData);
		int Type = Tmp.getInt();
		if(Type == 0x39383131){ //xenon
			return "Xenon";
		}
		else if(Type == 0x39383130){ //zephyr
			return "Zephyr";
		}
		else if(Type == 0x39393430){ //falcon
			return "Falcon";
		}
		else if(Type == 0x39393564){ //jasper
			return "Jasper";
		}
		else if(Type == 0x39386662){ //trinity
			return "Trinity";
		}
		else if(Type == 0x39386661){ //corona
			return "Corona";
		} 
		else { //corona / winchester
			return "Corona/Winchester";
		}
	}
	
	@Deprecated
	public static int getHardwareInfo(byte[] Buffer) {
		ByteBuffer Tmp = ByteBuffer.wrap(Buffer);
		int Type = Tmp.getInt();
		
		if(Type == 0x39383131){ //xenon
			return 0x00000227;
		}
		else if(Type == 0x39383130){ //zephyr
			return 0x10000227;
		}
		else if(Type == 0x39393430){ //falcon
			return 0x20000227;
		}
		else if(Type == 0x39393564){ //jasper
			return 0x30000227;
		}
		else if(Type == 0x39386662){ //trinity
			return 0x40000227;
		}
		else if(Type == 0x39386661){ //corona
			return 0x50000227;
		} 
		else { //corona / winchester
			return 0x50000227;
		}
	}
	
	@Deprecated
	public static String getFuseDigest(byte[] buffer){
		ByteBuffer Tmp = ByteBuffer.wrap(buffer);
		int Type = Tmp.getInt();
		
		if(Type == 0x39383131){ //xenon
			return "0224EEA61E898BA155B5AF74AA78AD0B";
		}
		else if(Type == 0x39383130){ //zephyr
			return "0224EEA61E898BA155B5AF74AA78AD0B";
		}
		else if(Type == 0x39393430){ //falcon
			return "4EEAA3323D9F40AA90C00EFC5AD5B000";
		}
		else if(Type == 0x39393564){ //jasper
			return "FF239990ED61D154B23135990D90BDBC";
		}
		else if(Type == 0x39386662){ //trinity
			return "DBE6358778CBFC2F52A3BAF892458D65";
		}
		else if(Type == 0x39386661){ //corona
			return "D132FB439B4847E39FE54646F0A99EB1";
		} 
		else { //corona / winchester
			return "D132FB439B4847E39FE54646F0A99EB1";
		}
    }
	
	@Deprecated
	public static String getSMCVersion(byte[] buffer){
		ByteBuffer Tmp = ByteBuffer.wrap(buffer);
		int Type = Tmp.getInt();
		
		if(Type == 0x39383131){ //xenon
			return "1231010600";
		}
		else if(Type == 0x39383130){ //zephyr
			return "1221010A00";
		}
		else if(Type == 0x39393430){ //falcon
			return "1231010600";
		}
		else if(Type == 0x39393564){ //jasper
			return "1241020300";
		}
		else if(Type == 0x39386662){ //trinity
			return "1251030100";
		}
		else if(Type == 0x39386661){ //corona
			return "1262020500";
		} 
		else { //corona / winchester
			return "1262020500";
		}
	}*/
	
	public static short getBLDR(boolean type1kv){
		return (short) ((type1kv) ? (0xD83E & ~0x20) : 0xD83E);
	}
	
	public static boolean  getFCRT(byte[] Odd){
		ByteBuffer buffer =  ByteBuffer.wrap(Odd);
		buffer.position(0);
		short fcrt = buffer.getShort();
        return (fcrt & 0x120) != 0 ? true : false;
    }
	
	public static int getHVFlags(boolean CRL, boolean FCRT){
		int HvKeysStatusFlag = (CRL) ? (0x023289D3 | 0x10000) : 0x023289D3;
		return HvKeysStatusFlag = (FCRT) ? (HvKeysStatusFlag | 0x1000000) : HvKeysStatusFlag;
	}
	
	public static String getGeneologyHash(byte[] Data, String CPUKey){
		byte[] cpukey = hexToBytes(new StringBuilder(CPUKey).reverse().toString());
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			sha1.update(Data, 0, 4);
		    sha1.update(Data, 16, 4);
		    sha1.update(cpukey, 0, 8);
		    sha1.update(Data, 4, 12);
		    sha1.update(cpukey, 8, 8);
		    return bytesToHex(sha1.digest());
		} catch (NoSuchAlgorithmException e) {
			Logger.error(e, "Utilities.getGeneologyHash()");
			return "";
		}
	    
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static List<MessageObject> getChat(int amount){
		List<MessageObject> messages = new ArrayList<MessageObject>();
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT * FROM `chat` ORDER BY `time` DESC LIMIT "+amount);
				while(res.next()){
					//get values from db
					String message = res.getString("message");
					int id = res.getInt("uid");
					long time = res.getLong("time");
					//format string
					DateTimeFormatter fmt = DateTimeFormat.forPattern("hh:mm a").withZone(DateTimeZone.forID("America/New_York"));
					DateTime now = new DateTime(time);
					//get webclient
					WebClient wc = new WebClient(id);
					wc.PoolData();
					//create message object
					MessageObject mo = new MessageObject(id, wc.getName(), wc.getAvatar(), message, fmt.print(now));
					//add message
					messages.add(mo);
				}
				res.close(); statement.close(); conn.close();
				Collections.reverse(messages);
				List<MessageObject> tmp = new ArrayList<MessageObject>(messages);
				messages.clear();
				for(MessageObject mo : tmp){
					messages.add(mo);
					if(mo.getMessage().contains("*0XCCX0*")) messages.clear();
				}
				return messages;
			} 
			return messages;
		} catch (SQLException e){
			Logger.error(e, "Utilities.getChat()");
			return messages;
		}
	}
	
	public static Patch getPatch(int id){
		List<Patch> tmp = new ArrayList<Patch>(TamperedListener.getPatches());
		for(Patch p : tmp){
			if (p.getIndex() == id){
				return p;
			}
		}
		return null;
	}
	
	public static void SaveChatToDB(int ID, String Message){
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				long Time = System.currentTimeMillis();
				statement.execute("INSERT INTO `chat`(`uid`, `message`, `time`) VALUES ('"+ID+"','"+Message+"','"+Time+"')");
				statement.close(); conn.close();
			}
		} catch (SQLException e){
			Logger.error(e, "Utilities.SaveChatToDB()");
		}
	}
	
	public static void WritePacketBinary(String Name, String CPUKey, byte[] data){
		if(!TamperedListener.getWritePackets()) return;
		try {
			Name = Name.replaceAll(".bin", "_" + (System.currentTimeMillis()/1000) + ".bin");
			Name = Name.replaceAll(".txt", "_" + (System.currentTimeMillis()/1000) + ".txt");
			Files.createDirectories(Paths.get("packets/"+CPUKey));
			FileOutputStream fos = new FileOutputStream(new File("packets/"+CPUKey+"/"+Name));
			fos.write(data);
			fos.flush();
			fos.close();
		} catch (IOException e){
			Logger.error(e, "Utilities.WritePacketBinary()");
		}
	}
	
	public static void DoFileUpload()  throws MalformedURLException, IOException, InterruptedException {
		// new HttpClient
        CloseableHttpClient httpClient = new DefaultHttpClient();
		
		HttpEntity entity = MultipartEntityBuilder
			    .create()
			    .addBinaryBody("file", new File("file.bin"), ContentType.create("binary/octet-stream"), "file.bin")
			    .build();

		HttpPost httpPost = new HttpPost("http://137.74.114.158/API/TLSC/TLSC.php");
		httpPost.setEntity(entity);
		HttpResponse response = httpClient.execute(httpPost);
		HttpEntity result = response.getEntity();
			
		if (result != null) {
			String responseStr = EntityUtils.toString(result).trim();
			// you can add an if statement here and do other actions based on the response
			System.out.println(responseStr);
			System.out.println(response.getStatusLine());
	    } 
	    httpClient.close();
	}
	
	public static byte[] Reverse8(byte[] input) {
        byte[] buffer = new byte[input.length];
        int num = input.length - 8;
        int num2 = 0;
        for (int i = 0; i < (input.length / 8); i++){
            for (int j = 0; j < 8; j++){
                buffer[num2 + j] = input[num + j];
            }
            num -= 8;
            num2 += 8;
        }
        return buffer;
    }
	
	public static byte[] Reverse(byte[] array) {
		byte[] tmp = new byte[array.length];
		int i = array.length-1;
		for(byte b : array){
			tmp[i] = b;
			i--;
		}
		return tmp;
	}
	
	public static void WriteFile(String path, byte[] data){
		try {
			FileOutputStream fos = new FileOutputStream(new File(path));
			fos.write(data);
			fos.flush();
			fos.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static int ByteToInt(byte[] tmp){
		//format bytearray then wrap
		if(tmp.length < 4){
			int x = 4 - tmp.length;
			byte[] tmp2 = new byte[8];
			System.arraycopy(tmp, 0, tmp2, x, tmp.length);
			tmp = tmp2;
		}
		ByteBuffer tmp1 = ByteBuffer.wrap(tmp);
		tmp1.position(0);
		return tmp1.getInt();
	}
	
	public static byte[] IntToByte(int tmp){
		ByteBuffer tmp1 = ByteBuffer.allocate(4);
		tmp1.putInt(tmp);
		return tmp1.array();
	}
	
	public static long ByteToLong(byte[] tmp){
		//format bytearray then wrap
		if(tmp.length < 8){
			int x = 8 - tmp.length;
			byte[] tmp2 = new byte[8];
			System.arraycopy(tmp, 0, tmp2, x, tmp.length);
			tmp = tmp2;
		}
		ByteBuffer tmp1 = ByteBuffer.wrap(tmp);
		tmp1.position(0);
		return tmp1.getLong();
	}

	public static List<String> getAllClients() {
		List<String> cpukeys = new ArrayList<String>();
		try {
			if(isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT * FROM `clients` WHERE `kvstatus` != 1");
				while(res.next()){
					cpukeys.add(res.getString("cpukey"));
				}
				res.close(); statement.close(); conn.close();
			}
			return cpukeys;
		} catch (SQLException e){
			Logger.error(e, "Utilities.getAllClients()");
			return cpukeys;
		}
	}
}
