package com.haxzz.tl.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.enums.SECURITY_FLAG;
import com.haxzz.tl.enums.TOKEN_STATUS;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.requests.TokenRequest;
import com.haxzz.tl.responses.TokenResponse;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class TokenHandler {
	
	Socket socket;
	ClientData client;
	TokenRequest request;
	TokenResponse response;
	TOKEN_STATUS TokenStatus;
	int Days = 0;
	
	public TokenHandler(Socket socket, byte[] Buffer){
		this.socket = socket;
		request = new TokenRequest(Buffer);
		client = new ClientData(request.getCPUKey());
		response = new TokenResponse();
		DoResponse();
	}
	
	public void DoResponse(){
		client.setIP(socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
		client.setVersion(request.getVersion());
		client.setXEXHash(request.getXEXHash());
		client.setLastPing((DateTime.now().getMillis()/1000L));
		
		SECURITY_FLAG flag = Utilities.checkFlags(client, request.getSession(), request.getGenealogyHash(), true);
		
		response.setDays(TOKEN_STATUS.ERROR.getValue());
		
		if (Utilities.isBadFlag(flag)){
			WriteOutput();
			client.updateData();
			Log();
			return;
		}
		
		TokenStatus = Utilities.isTokenValid(request.getToken());
		response.setDays(TokenStatus.getValue());
		
		if(TokenStatus == TOKEN_STATUS.EXISTS){
			Days = Utilities.GetTokenDays(request.getToken());
			if(request.getConfirm()){
				response.setDays(Utilities.redeemToken(client, request.getToken()).getValue());
			} else {
				response.setDays(Utilities.GetTokenDays(request.getToken()));
			}
		}
			
		
		WriteOutput();
		client.updateData();
		Log();
	}
	
	public void WriteOutput() {
		try {
			//write packet
			Utilities.WritePacketBinary("TokenResponse.bin", client.getCPUKey(), response.getBuffer());
			
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			byte[] EncryptedBuffer = TamperedListener.getRC4Cipher().doFinal(response.getBuffer());
			output.write(EncryptedBuffer);
			output.flush();
			output.close();
		} catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
			Logger.error(e, "TokenHandler.WriteOutput()");
		}
		
	}
	
	public void Log(){
		String print = "%m_\n\n-----------------"+Utilities.getTimestamp() + "-----------------\n\n" +
					   "%m_   Token Command:" + "\n" +
					   "%m_      - IP: %w_" + client.getIP() + "\n" +
				       "%m_      - CPUKey: %w_"+ client.getCPUKey() + "\n" + 
				       "%m_      - Name: %w_"+ client.getName() + "\n" + 
					   "%m_      - Hash: %w_"+ client.getXEXHash()+"\n" + 
					   "%m_      - Session: %w_"+request.getSession()+"\n" + 
					   "%m_      - Genealogy: %w_"+Utilities.getGeneologyHash(Utilities.hexToBytes(request.getGenealogyHash()), client.getCPUKey())+"\n" + 
				       "%m_      - Version: %w_"+ client.getVersion() + "\n" +
				       "%m_      - Token: %w_" + request.getToken().toUpperCase() + "\n" + 
				       "%m_      - Status: %w_" + TokenStatus + "\n" + 
				       ((TokenStatus == TOKEN_STATUS.EXISTS) ? 
				       "%m_      - Days: %w_" + ((Days == 99999) ? "Lifetime" : Days) + "\n": "") + 
				       "%m_      - Action: %w_" + ((request.getConfirm()) ? "Redeem Token" : "Check Token") + "\n" +
				       "%m_\n-----------------"+Utilities.getTimestamp() + "-----------------\n";
		Logger.log(print, client.getName(), client.getCPUKey(), client.getIP());
	}

}
