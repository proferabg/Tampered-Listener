package com.haxzz.tl.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.AUTH_TYPE;
import com.haxzz.tl.enums.SECURITY_FLAG;
import com.haxzz.tl.enums.STATUS_TYPE;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.requests.SaltRequest;
import com.haxzz.tl.responses.SaltResponse;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class SaltHandler {
	
	Socket socket;
	ClientData client;
	SaltRequest request;
	SaltResponse response;
	boolean NewSession = false;

	public SaltHandler(Socket socket, byte[] Buffer) throws IOException {
		//copy socket
		this.socket = socket;
		//parse request
		request = new SaltRequest(Buffer);
		//grab client data
		client = new ClientData(request.getCPUKey());
		//initialize nw response
		response = new SaltResponse();
		//do response	
		DoResponse();
	}
	
	public void DoResponse(){
		//update some data in clientdata first
		client.setIP(socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
		client.setVersion(request.getVersion());
		client.setXEXHash(request.getXEXHash());
		client.setKV(request.getKeyVault());
		TamperedListener.getKVChecker().AddToQueue(client, request.getKeyVault());
		client.setLastPing((DateTime.now().getMillis()/1000L));
		
		//check session and flags before updating
		SECURITY_FLAG flag;
		if(request.getSession().equalsIgnoreCase("0000000000000000")){
			NewSession = true;
			flag = Utilities.checkFlags(client, "", request.getGenealogyHash(), false);
			//make new session
			byte[] tmp_8 = new byte[8];
			new Random().nextBytes(tmp_8);
			String Session = Utilities.bytesToHex(tmp_8);
			client.setSession(Session);
			client.setCRL(false);//reset crl for session
			response.setSession(Utilities.hexToBytes(Session));
		} else {
			flag = Utilities.checkFlags(client, request.getSession(), request.getGenealogyHash(), true);
			response.setSession(Utilities.hexToBytes(request.getSession()));
		}
		
		
		//do flags before data
		response.setStatus(Utilities.getStatusFromFlag(flag));
		
		//if bad flag then dont send any more data
		if (Utilities.isBadFlag(flag)){
			WriteOutput();
			client.updateData();
			Log();
			return;
		}
		
		//set kv used on
		response.setKvUsedOn((short)Utilities.getKVUsedOn(client));
		
		//set status
		if(!client.getIsInDB()){
			response.setStatus(STATUS_TYPE.NO_AUTH);
			response.setDays(0);
			response.setUnixExpire(0);
			response.setName("Tampered User");
		} else if(client.hasTime()){
			response.setStatus(STATUS_TYPE.AUTHED);
			response.setAuthStatus(client.handleAuth(true));
			response.setDays(client.getDays());
			response.setUnixExpire(client.getRemaining());
			response.setName(client.getName());
		} else {
			response.setStatus(STATUS_TYPE.EXPIRED);
			response.setName(client.getName());
			response.setDays(0);
			response.setUnixExpire(0);
			response.setName(client.getName());
		}
		
		WriteOutput();
		client.updateData();
		Log();
	}
	
	public void WriteOutput() {
		try {
			//write packet
			Utilities.WritePacketBinary("SaltResponse.bin", client.getCPUKey(), response.getBuffer());
			
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			byte[] EncryptedBuffer = TamperedListener.getRC4Cipher().doFinal(response.getBuffer());
			output.write(EncryptedBuffer);
			output.flush();
			output.close();
		} catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
			Logger.error(e, "SaltHandler.WriteOutput()");
		}
		
	}
	
	public void Log(){
		String print = "%bc_\n\n-----------------"+Utilities.getTimestamp() + "-----------------\n\n" +
					   "%bc_   Salt Command:" + "\n" +
					   "%bc_      - IP: %w_" + client.getIP() + "\n" +
				       "%bc_      - CPUKey: %w_"+ client.getCPUKey() + "\n" + 
				       "%bc_      - Name: %w_"+ client.getName() + "\n" + 
					   "%bc_      - Hash: %w_"+ client.getXEXHash()+"\n" + 
					   "%bc_      - Session: %w_"+request.getSession()+"\n" + 
					   ((NewSession) ? "%bc_      - New Session: %w_"+Utilities.bytesToHex(response.getSession())+"\n" : "") + 
					   "%bc_      - Genealogy: %w_"+Utilities.getGeneologyHash(Utilities.hexToBytes(request.getGenealogyHash()), client.getCPUKey())+"\n" + 
				       "%bc_      - Version: %w_"+ client.getVersion() + "\n" +
				       "%bc_      - Status: %w_" + response.getStatus() + "\n" + 
				       "%bc_      - Auth: %w_" + response.getAuthStatus() + "\n" + 
				       ((client.hasTime() && response.getAuthStatus() != AUTH_TYPE.LIFETIME) ? 
				       "%bc_      - Expire Time: %w_" + Utilities.getExpireTime(client) + "\n" + 
				       "%bc_      - Days: %w_" + client.getDays() + "\n" : "") +
				       "%bc_      - KV Used On: %w_" + response.getKvUsedOn() + " Other Console(s)\n" + 
					   "%bc_\n-----------------"+Utilities.getTimestamp() + "-----------------\n";
		Logger.log(print, client.getName(), client.getCPUKey(), client.getIP());
	}
	
	

}
