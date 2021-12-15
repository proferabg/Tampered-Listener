package com.haxzz.tl.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import org.mariadb.jdbc.MariaDbDataSource;

import com.haxzz.tl.utilities.Logger;

public class MariaDB {

	private String Username, Database, Password, Port, Hostname;
	private static MariaDbDataSource DS;
	
	public MariaDB(String Hostname, String Port, String Database, String Username, String Password){
		//set this shit first
		this.Hostname = Hostname;
		this.Port = Port;
		this.Database = Database;
		this.Username = Username;
		this.Password = Password;
		//try datasource
		try {
			DS = new MariaDbDataSource("jdbc:mysql://" + this.Hostname + ":" + this.Port + "/" + this.Database);
			DS.setUser(this.Username);
			DS.setPassword(this.Password);
			DS.setLoginTimeout(15);
		} catch (SQLException e){
			Logger.error(e, "MariaDB()");
		}
	}
	
	public Connection openConnection(){
		try {
			return DS.getConnection();
		} catch (SQLException e){
			Logger.error(e, "MariaDB.openConnection()");
			return null;
		}
	}

}
