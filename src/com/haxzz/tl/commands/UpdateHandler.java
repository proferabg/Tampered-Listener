package com.haxzz.tl.commands;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.joda.time.DateTime;

import com.haxzz.tl.TamperedListener;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.requests.UpdateRequest;
import com.haxzz.tl.responses.UpdateResponse;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class UpdateHandler {
	
	Socket socket;
	ClientData client;
	UpdateRequest request;
	UpdateResponse response;
	
	public UpdateHandler(Socket socket, byte[] Buffer){
		this.socket = socket;
		request = new UpdateRequest(Buffer);
		client = new ClientData(request.getCPUKey());
		response = new UpdateResponse();
		DoResponse();
	}
	
	public void DoResponse(){
		client.setIP(socket.getRemoteSocketAddress().toString().replaceAll("/", "").split(":")[0]);
		client.setVersion(request.getVersion());
		client.setXEXHash(request.getXEXHash());
		client.setLastPing((DateTime.now().getMillis()/1000L));
		
		WriteOutput();
		client.updateMinorData();
		Log();
	}
	
	public void WriteOutput() {
		try {
			//download new xex
			long x = System.currentTimeMillis();
			Logger.logWithTime("%y_Downloading XEX.", "Global", "", "");
			File file = new File("xex/" + client.getCPUKey() + "_" + TamperedListener.getXEXName());
			if(file.exists()) file.delete();
			URL website = new URL(TamperedListener.getXEXUrl());
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos = new FileOutputStream(file);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			rbc.close();
			Logger.logWithTime("%y_XEX Downloaded. Took " + (System.currentTimeMillis() - x) + " ms\r\n", "Global", "", "");
			//read xex downloaded
			FileInputStream fis = new FileInputStream(file);
			int Size = fis.available();
			byte[] XEXBuffer = new byte[Size];
			fis.read(XEXBuffer);
			fis.close();
			//set length
			response.setLength(Size);
			//write output
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			byte[] EncryptedBuffer = TamperedListener.getRC4Cipher().doFinal(response.getBuffer());
			output.write(EncryptedBuffer);
			byte[] EncryptedXEX = TamperedListener.getRC4Cipher().doFinal(XEXBuffer);
			Thread.sleep(50);
			for (int i = 0; i * 2048 < Size; i++) {
				byte[] block = Arrays.copyOfRange(EncryptedXEX, i * 2048, (i * 2048) + 2048);
				output.write(block);
				//Thread.sleep(100);
			}
			output.flush();
			output.close();
			file.delete();
		} catch (IOException | IllegalBlockSizeException | BadPaddingException | InterruptedException e) {
			Logger.error(e, "UpdateHandler.WriteOutput()");
		}
		
	}
	
	public void Log(){
		String print = "%b_\n\n-----------------"+Utilities.getTimestamp() + "-----------------\n\n" +
					   "%b_   Update Command:" + "\n" +
					   "%b_      - IP: %w_" + client.getIP() + "\n" +
				       "%b_      - CPUKey: %w_"+ client.getCPUKey() + "\n" + 
				       "%b_      - Name: %w_"+ client.getName() + "\n" + 
					   "%b_      - Hash: %w_"+ client.getXEXHash()+"\n" + 
					   "%b_      - Session: %w_"+request.getSession()+"\n" +
					   "%b_      - Genealogy: %w_"+Utilities.getGeneologyHash(Utilities.hexToBytes(request.getGenealogyHash()), client.getCPUKey())+"\n" +  
				       "%b_      - Version: %w_"+ client.getVersion() + "\n" +
					   "%b_\n-----------------"+Utilities.getTimestamp() + "-----------------\n";
		Logger.log(print, client.getName(), client.getCPUKey(), client.getIP());
	}
}
