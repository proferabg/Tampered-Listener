package com.haxzz.tl.objects;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.sql.rowset.serial.SerialBlob;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.AUTH_TYPE;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class ClientData {
	
	//initialize default values
	private int ID = -1, Days = 0, LastDays = 0, Fails = 0, XStatus = 0, KVStatus = 0;
	private String CPUKey = "", XEXHash = "", IP = "", Name = "Free User", Gamertag = "", TitleID = "", Session = "", GenealogyHash = "", KVSerial = "";
	private double Version = 0.0;
	private boolean Lifetime = false, Blacklisted = false, CRL = false, KVDifferent = false, Developer = false, UsedTrial = false;
	private long Remaining = 0, LastPing = 0, LastExpire = 0, KVStart = 0, TotalTimeUsed = 0;
	private byte[] KV = new byte[16384];
	
	//is registered check
	private boolean IsInDB = false;
	
	public ClientData(String CPUKey){
		this.CPUKey = CPUKey;
		poolData();
	}
	
	public void poolData(){
		try {
			if(Utilities.isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT * FROM `clients` WHERE `cpukey` = '"+CPUKey+"' LIMIT 1");
				if(res.next()) {
					//Ints
					ID = res.getInt("id");
					Days = res.getInt("days");
					Fails = res.getInt("fails");
					LastDays = res.getInt("lastdays"); 
					XStatus = res.getInt("xstatus");
					KVStatus = res.getInt("kvstatus");
					//Strings
					XEXHash = res.getString("xexhash");
					IP = res.getString("ip");
					Name = res.getString("name");
					Gamertag = res.getString("gamertag");
					TitleID = res.getString("titleid");
					Session = res.getString("session");
					GenealogyHash = res.getString("genealogy_hash");
					KVSerial = res.getString("kvserial");
					//doubles
					Version = res.getDouble("version");
					//booleans
					Lifetime = res.getBoolean("lifetime");
					Blacklisted = res.getBoolean("blacklisted");
					CRL = res.getBoolean("crl");
					Developer = res.getBoolean("developer");
					UsedTrial = res.getBoolean("usedtrial");
					//longs
					Remaining = res.getLong("remaining");
					LastPing = res.getLong("lastping");
					LastExpire = res.getLong("lastexpire");
					KVStart = res.getLong("kvstart");
					TotalTimeUsed = res.getLong("totaltimeused");
					//blobs
					Blob blob = res.getBlob("kv");
					if(blob != null && !res.wasNull() && blob.length() == 16384){
						KV = blob.getBytes(1, 16384);
						//free blob
						blob.free();
					}
					//client is in db
					IsInDB = true;
				}
				res.close(); statement.close(); conn.close();
			} else {
				Logger.log("Error Pooling Data: Database Not Connected!", "Global", "", "");
			}
		} catch (SQLException e) {
			Logger.error(e, "ClientData.poolData()");
		}
	}
	
	public void insertData(){
		if(IsInDB){
			return;
		}
		try {
			if(Utilities.isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				statement.execute("INSERT INTO `clients`(`cpukey`, `name`, `days`, `remaining`) VALUES ('" + CPUKey + "' , 'Tampered User', '" + Days + "', '" + Remaining + "')");
				//cleanup
				statement.close(); conn.close();
				IsInDB = true;
			} else {
				Logger.log("Error Inserting Data: Database Not Connected!", "Global", "", "");
			}
		} catch (SQLException e) {
			Logger.error(e, "ClientData.insertData()");
		}
		
	}
	
	public void updateData(){
		if(!IsInDB){
			Name = "Tampered User";
			insertData();
		}
		try {
			if(Utilities.isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				String Query = getChangedValues();
				//String query = "UPDATE `clients` SET `xexhash` = '"+XEXHash+"', `version` = '"+Version+"', `ip` = '"+IP+ "', `days` = '"+Days+"', `remaining` = '"+Remaining+"', `fails` = '"+Fails+"', `gamertag` = '"+Gamertag+"', `titleid` = '"+TitleID+"', `session` = '"+Session+"', `kvserial` = '" + KVSerial + "', `genealogy_hash` = '" + GenealogyHash +"', `crl` = '" + ((CRL) ? 1 : 0) + "', `xstatus` = '" + XStatus + "', `lastping` = '"+LastPing+"', `lastdays` = '"+LastDays+"', `kvstatus` = '" + KVStatus + "', `lastexpire` = '" + LastExpire + "', `kvstart` = '" + KVStart + "', `totaltimeused` = '" + TotalTimeUsed + "' WHERE `cpukey` = '"+CPUKey+"'";
				if(Query != ""){
					Query = "UPDATE `clients` SET "+Query+" WHERE `cpukey` = '"+CPUKey+"'";
					//System.out.println(Query);
					statement.executeUpdate(Query);
				}
				
				if(KVDifferent){
					PreparedStatement statement2 = conn.prepareStatement("UPDATE `clients` SET `kv` = ? WHERE `cpukey` = '"+CPUKey+"'");
					statement2.setBlob(1, new SerialBlob(KV));
					statement2.execute();
					//cleanup
					statement2.close();
				}
				//cleanup
				statement.close(); conn.close();
			} else {
				Logger.log("Error Updating Data: Database Not Connected!", "Global", "", "");
			}
		} catch (SQLException e) {
			Logger.error(e, "ClientData.updateData()");
		}
	}
	
	public String getChangedValues(){
		try {
			if(Utilities.isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				ResultSet res = statement.executeQuery("SELECT * FROM `clients` WHERE `cpukey` = '"+CPUKey+"' LIMIT 1");
				String Query = "";
				String Comma = "";
				if(res.next()) {
					//Ints
					if(Days != res.getInt("days")){
						Query += Comma + "`days` = '"+Days+"'";
						Comma = ", ";
					}
					if(Fails != res.getInt("fails")){
						Query += Comma + "`fails` = '"+Fails+"'";
						Comma = ", ";
					}
					if(LastDays != res.getInt("lastdays")){
						Query += Comma + "`lastdays` = '"+LastDays+"'";
						Comma = ", ";
					}
					if(XStatus != res.getInt("xstatus")){
						Query += Comma + "`xstatus` = '"+XStatus+"'";
						Comma = ", ";
					}
					if(KVStatus != res.getInt("kvstatus")){
						Query += Comma + "`kvstatus` = '"+KVStatus+"'";
						Comma = ", ";
					}
					//Strings
					if(!XEXHash.equalsIgnoreCase(res.getString("xexhash"))){
						Query += Comma + "`xexhash` = '"+XEXHash+"'";
						Comma = ", ";
					}
					if(!IP.equalsIgnoreCase(res.getString("ip"))){
						Query += Comma + "`ip` = '"+IP+"'";
						Comma = ", ";
					}
					if(!Name.equalsIgnoreCase(res.getString("name"))){
						Query += Comma + "`name` = '"+Name+"'";
						Comma = ", ";
					}
					if(!Gamertag.equalsIgnoreCase(res.getString("gamertag"))){
						Query += Comma + "`gamertag` = '"+Gamertag+"'";
						Comma = ", ";
					}
					if(!TitleID.equalsIgnoreCase(res.getString("titleid"))){
						Query += Comma + "`titleid` = '"+TitleID+"'";
						Comma = ", ";
					}
					if(!Session.equalsIgnoreCase(res.getString("session"))){
						Query += Comma + "`session` = '"+Session+"'";
						Comma = ", ";
					}
					if(!GenealogyHash.equalsIgnoreCase(res.getString("genealogy_hash"))){
						Query += Comma + "`genealogy_hash` = '"+GenealogyHash+"'";
						Comma = ", ";
					}
					if(!KVSerial.equalsIgnoreCase(res.getString("kvserial"))){
						Query += Comma + "`kvserial` = '"+KVSerial+"'";
						Comma = ", ";
					}
					//doubles
					if(Version != res.getDouble("version")){
						Query += Comma + "`version` = '"+Version+"'";
						Comma = ", ";
					}
					//booleans
					if(Lifetime != res.getBoolean("lifetime")){
						Query += Comma + "`lifetime` = '"+((Lifetime) ? 1 : 0)+"'";
						Comma = ", ";
					}
					if(CRL != res.getBoolean("crl")){
						Query += Comma + "`crl` = '"+((CRL) ? 1 : 0)+"'";
						Comma = ", ";
					}
					if(UsedTrial != res.getBoolean("usedtrial")){
						Query += Comma + "`usedtrial` = '"+((UsedTrial) ? 1 : 0)+"'";
						Comma = ", ";
					}
					/* NOTE DEVLOPER AND BLACKLIST DO NOT UPDATE ON LISTENER SIDE */
					//longs
					if(Remaining != res.getLong("remaining")){
						Query += Comma + "`remaining` = '"+Remaining+"'";
						Comma = ", ";
					}
					if(LastPing != res.getLong("lastping")){
						Query += Comma + "`lastping` = '"+LastPing+"'";
						Comma = ", ";
					}
					if(LastExpire != res.getLong("lastexpire")){
						Query += Comma + "`lastexpire` = '"+LastExpire+"'";
						Comma = ", ";
					}
					if(KVStart != res.getLong("kvstart")){
						Query += Comma + "`kvstart` = '"+KVStart+"'";
						Comma = ", ";
					}
					if(TotalTimeUsed != res.getLong("totaltimeused")){
						Query += Comma + "`totaltimeused` = '"+TotalTimeUsed+"'";
						Comma = ", ";
					}
				}
				res.close(); statement.close(); conn.close();
				return Query;
			} else {
				Logger.log("Error Getting Changed Values Data: Database Not Connected!", "Global", "", "");
				return "";
			}
		} catch (SQLException e) {
			Logger.error(e, "ClientData.getChangedValues()");
			return "";
		}
	}
	
	public void updateKVStatus(){
		if(!IsInDB){
			return;
		}
		try {
			if(Utilities.isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				statement.executeUpdate("UPDATE `clients` SET `kvstatus` = '" + KVStatus + "' WHERE `cpukey` = '"+CPUKey+"'");
				//cleanup
				statement.close(); conn.close();
			} else {
				Logger.log("Error Updating KV Status: Database Not Connected!", "Global", "", "");
			}
		} catch (SQLException e) {
			Logger.error(e, "ClientData.updateKVStatus()");
		}
	}
	
	public void updateMinorData(){
		if(!IsInDB){
			return;
		}
		try {
			if(Utilities.isDbConnected()){
				Connection conn = TamperedListener.getDB().openConnection();
				Statement statement = conn.createStatement();
				statement.setEscapeProcessing(true);
				statement.executeUpdate("UPDATE `clients` SET `xexhash` = '" + XEXHash + "', `version` = '" + Version + "', `ip` = '" + IP + "', `lastping` = '" + LastPing + "' WHERE `cpukey` = '"+CPUKey+"'");
				//cleanup
				statement.close(); conn.close();
			} else {
				Logger.log("Error Updating Minor Data: Database Not Connected!", "Global", "", "");
			}
		} catch (SQLException e) {
			Logger.error(e, "ClientData.updateMinorData()");
		}
	}
	
	public boolean hasTime(){
		//freetime first
		if(Utilities.isFreeTime()){
			return true;
		}
		//then check is in db
		if(!getIsInDB()){
			return false;
		}
		//lifetime
		if(Lifetime){
			return true;
		}
		//has days remaining
		if(Days > 0){
			return true;
		}
		
		DateTime now = DateTime.now(DateTimeZone.UTC);
		DateTime expire = new DateTime(Remaining*1000L);
		
		//has time remaining on day
		if (now.isBefore(expire)){
			return true;
		}
		
		//no time, no days, no lifetime... what are you doing with your life
		return false;
	}
	
	public AUTH_TYPE handleAuth(boolean salt){
		//throw that null exception
		if(!hasTime()){
			return null;
		}
		
		//freetime first
		if(Utilities.isFreeTime()){
			return AUTH_TYPE.LIFETIME; //FREETIME PATCH
		}
		
		//lifetime
		if(Lifetime){
			return AUTH_TYPE.LIFETIME;
		}
		
		if(!salt && (Days != LastDays || Remaining != LastExpire)){
			LastDays = Days;
			LastExpire = Remaining;
			return AUTH_TYPE.CHANGED_DAYS;
		}
		
		//do current day time first
		DateTime now = DateTime.now(DateTimeZone.UTC);
		DateTime expire = new DateTime(Remaining*1000L);
		
		//has time remaining on day
		if (now.isBefore(expire)){
			return AUTH_TYPE.GENERAL;
		}
		
		//has days remaining
		if(Days > 0){
			Days -= 1;
			Remaining = (DateTime.now(DateTimeZone.UTC).getMillis() + 86400000)/1000; //now + 24 hours
			return ((salt) ? AUTH_TYPE.GENERAL : AUTH_TYPE.CHANGED_DAYS);
		}
		
		//shouldn't get here
		return null;
	}
	
	

	public void setID(int iD) {
		ID = iD;
	}

	public void setDays(int days) {
		Days = days;
	}

	public void setFails(int fails) {
		Fails = fails;
	}

	public void setCPUKey(String cPUKey) {
		CPUKey = cPUKey;
	}

	public void setXEXHash(String xEXHash) {
		XEXHash = xEXHash;
	}

	public void setIP(String iP) {
		IP = iP;
	}

	public void setName(String name) {
		Name = name;
	}

	public void setGamertag(String gamertag) {
		Gamertag = gamertag;
	}

	public void setTitleID(String titleID) {
		TitleID = titleID;
	}

	public void setSession(String session) {
		Session = session;
	}

	public void setVersion(double version) {
		Version = version;
	}

	public void setLifetime(boolean lifetime) {
		Lifetime = lifetime;
	}

	public void setBlacklisted(boolean blacklisted) {
		Blacklisted = blacklisted;
	}

	public void setRemaining(long remaining) {
		//Logger.logWithTime("%br_REMAINING CHANGED " + Remaining + " -> " + remaining, Name, CPUKey, IP);
		Remaining = remaining;
	}

	public void setKV(byte[] kV) {
		String NewSerial = new String(Utilities.trimZeros(Arrays.copyOfRange(kV, 0xB0, 0xB0 + 0xC)), StandardCharsets.UTF_8);
		if(KVSerial.equalsIgnoreCase(NewSerial) || !KVSerial.matches("\\d+")) {
			return;
		}
		KV = kV;
		KVSerial = NewSerial;
		KVStatus = 0;
		KVDifferent = true;
		KVStart = (DateTime.now().getMillis()/1000);
	}

	public void setIsInDB(boolean isInDB) {
		IsInDB = isInDB;
	}
	
	public void setCRL(boolean crl){
		CRL = crl;
	}
	
	public void setXStatus(int xstatus){
		XStatus = xstatus;
	}

	public int getID() {
		return ID;
	}

	public int getDays() {
		return Days;
	}

	public int getFails() {
		return Fails;
	}

	public String getCPUKey() {
		return CPUKey;
	}

	public String getXEXHash() {
		return XEXHash;
	}

	public String getIP() {
		return IP;
	}

	public String getName() {
		return Name;
	}

	public String getGamertag() {
		return Gamertag;
	}

	public String getTitleID() {
		return TitleID;
	}

	public String getSession() {
		return Session;
	}

	public double getVersion() {
		return Version;
	}

	public boolean isLifetime() {
		return Lifetime;
	}

	public boolean isBlacklisted() {
		return Blacklisted;
	}

	public long getRemaining() {
		return Remaining;
	}

	public byte[] getKV() {
		return KV;
	}

	public boolean getIsInDB() {
		return IsInDB;
	}
	
	public boolean getCRL(){
		return CRL;
	}

	public String getGenealogyHash() {
		return GenealogyHash;
	}

	public void setGenealogyHash(String genealogyHash) {
		GenealogyHash = genealogyHash;
	}

	public String getKVSerial() {
		return KVSerial;
	}

	public void setKVSerial(String kVSerial) {
		if(!KVSerial.matches("\\d+")) return;
		KVSerial = kVSerial;
	}
	
	public int getXStatus(){
		return XStatus;
	}

	public long getLastPing() {
		return LastPing;
	}

	public void setLastPing(long lastPing) {
		LastPing = lastPing;
	}

	public int getLastDays() {
		return LastDays;
	}

	public void setLastDays(int lastDays) {
		LastDays = lastDays;
	}

	public int getKVStatus() {
		return KVStatus;
	}

	public void setKVStatus(int kVStatus) {
		KVStatus = kVStatus;
	}

	public long getLastExpire() {
		return LastExpire;
	}

	public void setLastExpire(long lastExpire) {
		LastExpire = lastExpire;
	}

	public boolean isDeveloper() {
		return Developer;
	}

	public long getTotalTimeUsed() {
		return TotalTimeUsed;
	}

	public void setTotalTimeUsed(long totalTimeUsed) {
		TotalTimeUsed = totalTimeUsed;
	}
	
	public boolean hasUsedTrial() {
		return UsedTrial;
	}

	public void setTrialUsed(boolean usedTrial) {
		UsedTrial = usedTrial;
	}
}
