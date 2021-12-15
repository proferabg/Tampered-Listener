package com.haxzz.tl.kvchecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import com.haxzz.tl.enums.KV_CHECK;
import com.haxzz.tl.objects.ClientData;
import com.haxzz.tl.utilities.Logger;
import com.haxzz.tl.utilities.Utilities;

public class KVLogic {
	
	private static byte[] XMacsPublicKey = new byte[0x110];
	private static byte[] XMacsRequest = new byte[0x493];
	private static byte[] APRequest1 = new byte[0x16B];
	private static byte[] APRequest2 = new byte[0x187];
	private static byte[] TGSRequest = new byte[0x405];
	private static byte[] AuthData = new byte[0x81];
	private static byte[] ServiceRequest = new byte[0x7E];
	
	public static KV_CHECK CheckKV(byte[] KV, ClientData client){
		try {
			long StartMS = System.currentTimeMillis();
			//test kv
			String Serial = new String(Utilities.trimZeros(Arrays.copyOfRange(KV, 0xB0, 0xB0 + 0xC)), StandardCharsets.UTF_8);
			if(!Serial.matches("[0-9]{12}")) {
				Logger.logWithTime("%r_" + client.getName() + "'s KV Check Failed: KV Invalid\r\n", client.getName(), client.getCPUKey(), client.getIP());
				return KV_CHECK.INVALID;
			}
			//get xmacs
			byte[] XMacsLogonKey = GetXMacsLogonKey(KV);
			//null check xmacs failed
			if(XMacsLogonKey == null){
				Logger.logWithTime("%r_" + client.getName() + "'s KV Check Failed: Stage 1\r\n", client.getName(), client.getCPUKey(), client.getIP());
				return KV_CHECK.ERROR;
			}

			//get kv data
			byte[] ConsoleID = Arrays.copyOfRange(KV, 0x9CA, 0x9CA + 0x5);
			byte[] XeConCrtHash = MessageDigest.getInstance("SHA1").digest(Arrays.copyOfRange(KV, 0x9C8, 0x9C8 + 0xA8));
			byte[] ClientName = KVUtilities.ComputeClientName(ConsoleID);
			byte[] Timestamp = KVUtilities.GenerateTimestamp();
			
			ByteBuffer APModifiedRequest1 = ByteBuffer.wrap(Arrays.copyOfRange(APRequest1, 0x0, 0x16B));
			APModifiedRequest1.position(0x102);
			APModifiedRequest1.put(ClientName);
			APModifiedRequest1.position(0x24);
			APModifiedRequest1.put(XeConCrtHash);
			APModifiedRequest1.position(0xB0);
			APModifiedRequest1.put(Arrays.copyOfRange(KVUtilities.RC4HmacEncrypt(XMacsLogonKey, Timestamp, 1), 0, 0x34));

			DatagramSocket clientSocket = new DatagramSocket(); 
		    clientSocket.setSoTimeout(5000);
		    InetAddress IPAddress = InetAddress.getByName("XEAS.gtm.XBOXLIVE.COM");
		    clientSocket.connect(IPAddress, 0x58);
		    clientSocket.send(new DatagramPacket(APModifiedRequest1.array(), APModifiedRequest1.array().length, IPAddress, 0x58));

		    byte[] Receive = new byte[120];
		    DatagramPacket receivePacket = new DatagramPacket(Receive, 120);
		    long st = System.currentTimeMillis();
		    while(true){
		    	try {
		    		if(System.currentTimeMillis()-st > 15000) return KV_CHECK.ERROR; //15 second timeout
				    clientSocket.receive(receivePacket);
				    clientSocket.close();
				    break;
		    	} catch (SocketTimeoutException e){
		    		clientSocket.send(new DatagramPacket(APModifiedRequest1.array(), APModifiedRequest1.array().length, IPAddress, 0x58));
		    	}
		    }
		    //clientSocket.receive(receivePacket);
		    //clientSocket.close();

		    byte[] Timestamp2 = KVUtilities.GenerateTimestamp();
		    
		    if(Receive[0x2C] != 0x19){
				Logger.logWithTime("%r_" + client.getName() + "'s KV Check Failed: Stage 2\r\n", client.getName(), client.getCPUKey(), client.getIP());
				return KV_CHECK.ERROR;
		    }

		    ByteBuffer APModifiedRequest2 = ByteBuffer.wrap(Arrays.copyOfRange(APRequest2, 0x0, 0x187));
		    APModifiedRequest2.position(0x11E);
		    APModifiedRequest2.put(ClientName);
		    APModifiedRequest2.position(0x24);
		    APModifiedRequest2.put(XeConCrtHash);
		    APModifiedRequest2.position(0xCC);
		    APModifiedRequest2.put(Arrays.copyOfRange(KVUtilities.RC4HmacEncrypt(XMacsLogonKey, Timestamp2, 1), 0, 0x34));
		    APModifiedRequest2.position(0x44);
		    APModifiedRequest2.put(Arrays.copyOfRange(Receive, 0x68, 0x68 + 0x10));
		    

		    DatagramSocket clientSocket1 = new DatagramSocket(); 
		    clientSocket1.setSoTimeout(5000);
		    InetAddress IPAddress1 = InetAddress.getByName("XEAS.XBOXLIVE.COM");
		    clientSocket1.send(new DatagramPacket(APModifiedRequest2.array(), APModifiedRequest2.array().length, IPAddress1, 0x58));

			byte[] APResponse = new byte[0x2E9];
		    
		    DatagramPacket receivePacket1 = new DatagramPacket(APResponse, 745, IPAddress1, 0x58);
		    st = System.currentTimeMillis();
		    while(true){
		    	try {
		    		if(System.currentTimeMillis()-st > 15000) return KV_CHECK.ERROR; //15 second timeout
				    clientSocket1.receive(receivePacket1);
				    clientSocket1.close();
				    break;
		    	} catch (SocketTimeoutException e){
		    		clientSocket1.send(new DatagramPacket(APModifiedRequest2.array(), APModifiedRequest2.array().length, IPAddress1, 0x58));
		    	}
		    }
		    //clientSocket1.receive(receivePacket1);
		    //clientSocket1.close();

		    int x = Utilities.ByteToInt(APResponse);
			if(x == 0x7E723070) {
				Logger.logWithTime("%r_" + client.getName() + "'s KV Check Failed: Stage 3\r\n", client.getName(), client.getCPUKey(), client.getIP());
				return KV_CHECK.ERROR;
			}
		    
		    ByteBuffer TGSModifiedRequest = ByteBuffer.wrap(Arrays.copyOfRange(TGSRequest, 0x0, 0x405));
		    TGSModifiedRequest.position(0x1B5);
		    TGSModifiedRequest.put(Arrays.copyOfRange(APResponse, 0xA8, 0xA8 + 0x159));
		    
		    ByteBuffer AuthBuffer = ByteBuffer.wrap(Arrays.copyOfRange(AuthData, 0x0, 0x81));
		    AuthBuffer.position(40);
		    AuthBuffer.put(Arrays.copyOfRange(ClientName, 0, 15));
		    AuthBuffer.position(0x6D);
		    AuthBuffer.put(KVUtilities.GenerateTimestamp2());
		    AuthBuffer.position(0x52);
		    AuthBuffer.put(Arrays.copyOfRange(MessageDigest.getInstance("MD5").digest(Arrays.copyOfRange(TGSModifiedRequest.array(), 0x3BA, 0x3BA + 0x4B)), 0, 0x10));
			
		    byte[] tmp1 = Arrays.copyOfRange(KVUtilities.RC4HmacDecrypt(XMacsLogonKey, Arrays.copyOfRange(APResponse, 0x217, 0x217 + 0xD2), 8), 0x1B, 0x1B + 0x10);
		    byte[] keyf = KVUtilities.ComputeKdcNoonce(tmp1);
	
		    TGSModifiedRequest.position(0x31F);
		    TGSModifiedRequest.put(Arrays.copyOfRange(KVUtilities.RC4HmacEncrypt(tmp1, AuthBuffer.array(), 7), 0, 0x99));
		    TGSModifiedRequest.position(0x37);
		    TGSModifiedRequest.put(Arrays.copyOfRange(KVUtilities.RC4HmacEncrypt(keyf, Arrays.copyOfRange(ServiceRequest, 0x0, 0x7E), 0x4B1), 0, 150));
		    TGSModifiedRequest.position(0xDD);
		    TGSModifiedRequest.put(KVUtilities.GetTitleAuthData(tmp1, Arrays.copyOfRange(APModifiedRequest2.array(), 0x74, 0x74 + 0x42)));
		    
		    DatagramSocket clientSocket2 = new DatagramSocket(); 
		    clientSocket2.setSoTimeout(5000);
		    InetAddress IPAddress2 = InetAddress.getByName("XETGS.XBOXLIVE.COM");
		    clientSocket2.send(new DatagramPacket(TGSModifiedRequest.array(), TGSModifiedRequest.array().length, IPAddress2, 0x58));

		    byte[] TGSResponse = new byte[0x395];
		    
		    DatagramPacket receivePacket2 = new DatagramPacket(TGSResponse, 917, IPAddress2, 0x58);
		    st = System.currentTimeMillis();
		    while(true){
		    	try {
		    		if(System.currentTimeMillis()-st > 15000) return KV_CHECK.ERROR; //15 second timeout
				    clientSocket2.receive(receivePacket2);
				    clientSocket2.close();
				    break;
		    	} catch (SocketTimeoutException e){
		    		clientSocket2.send(new DatagramPacket(TGSModifiedRequest.array(), TGSModifiedRequest.array().length, IPAddress2, 0x58));
		    	}
		    }
		    //clientSocket2.receive(receivePacket2);
		    //clientSocket2.close();
		    
		    byte[] tmp2 = KVUtilities.RC4HmacDecrypt(keyf, Arrays.copyOfRange(TGSResponse, 0x32, 0x32 + 0x54), 0x4B2);
		    
		    //byte[] resp = Utilities.RC4HmacDecrypt(keyf, Arrays.copyOfRange(TGSResponse, 0x3A, 0x3A + 0xD0), 0x4B2);
		    
		    int status = Utilities.ByteToInt(Utilities.Reverse(Arrays.copyOfRange(tmp2, 0x8, 0x8 + 0x4)));

		    String check = "";
		    long NowMS = System.currentTimeMillis();
		    if(NowMS - StartMS > 1000){
				check = "%g_Check Took " + (NowMS - StartMS)/1000L + " s\r\n";
		    } else {
		    	check = "%g_Check Took " + (NowMS - StartMS) + " ms\r\n";
		    }
		    
		    if(status !=  0x8015190D){
				Logger.logWithTime("%g_" + client.getName() + "'s KV is Unbanned!", client.getName(), client.getCPUKey(), client.getIP());
				Logger.logWithTime(check, client.getName(), client.getCPUKey(), client.getIP());
		    	return KV_CHECK.UNBANNED;
		    } else {
				Logger.logWithTime("%r_" + client.getName() + "'s KV is Banned!", client.getName(), client.getCPUKey(), client.getIP());
				Logger.logWithTime(check, client.getName(), client.getCPUKey(), client.getIP());
		    	return KV_CHECK.BANNED;
		    }
		} catch (IOException | NoSuchAlgorithmException | IllegalStateException e){
			Logger.error(e, "KVLogic.checkKV()");
			return KV_CHECK.ERROR;
		}
	}
	
	public static void LoadFiles(){
		try {
			File xmacs = new File("kvbins/XMacsPubKey.bin");
			FileInputStream fis = new FileInputStream(xmacs);
			fis.read(XMacsPublicKey);
			fis.close();
			
			File xmacs2 = new File("kvbins/XMacsRequest.bin");
			FileInputStream fis1 = new FileInputStream(xmacs2);
			fis1.read(XMacsRequest);
			fis1.close();
			
			File apreq1 = new File("kvbins/APRequest1.bin");
			FileInputStream fis2 = new FileInputStream(apreq1);
			fis2.read(APRequest1);
			fis2.close();
			
			File apreq2 = new File("kvbins/APRequest2.bin");
			FileInputStream fis3 = new FileInputStream(apreq2);
			fis3.read(APRequest2);
			fis3.close();
			
			File tgsreq = new File("kvbins/TGSRequest.bin");
			FileInputStream fis4 = new FileInputStream(tgsreq);
			fis4.read(TGSRequest);
			fis4.close();
			
			File auth = new File("kvbins/AuthData.bin");
			FileInputStream fis5 = new FileInputStream(auth);
			fis5.read(AuthData);
			fis5.close();
			
			File serv = new File("kvbins/ServiceRequest.bin");
			FileInputStream fis6 = new FileInputStream(serv);
			fis6.read(ServiceRequest);
			fis6.close();
			
			
			
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static byte[] GetXMacsLogonKey(byte[] KV){
		try {
			//get cipher
			Cipher XMacsPublicCipher = LoadXMacsKey();
			//generate random buffer
			byte[] tmp = new byte[0x10];
			new Random(System.currentTimeMillis()).nextBytes(tmp);
			//encrypt buffer
			byte[] tmp1 = XMacsPublicCipher.doFinal(tmp);
			//reverse it
			byte[] tmp2 = Utilities.Reverse(tmp1);

			//load xmacsreq
			ByteBuffer XMacsModifiedRequest = ByteBuffer.wrap(Arrays.copyOf(XMacsRequest, 0x493));
			
			XMacsModifiedRequest.position(0x2C);
			XMacsModifiedRequest.put(Arrays.copyOfRange(tmp2, 0, 0x100));
			
			byte[] ConsoleSerial = Arrays.copyOfRange(KV, 0xB0, 0xB0 + 0xC);
			byte[] XeConsoleCertificate = Arrays.copyOfRange(KV, 0x9C8, 0x9C8 + 0x1A8);
			byte[] ConPrivKeyExp = Arrays.copyOfRange(KV, 0x29C, 0x29C + 0x4);
			byte[] ConPrivKeyParams = Arrays.copyOfRange(KV, 0x2A8, 0x2A8 + 0x1C0);
			byte[] ConsoleID = Arrays.copyOfRange(KV, 0x9CA, 0x9CA + 0x5);
			
			byte[] ClientName = KVUtilities.ComputeClientName(ConsoleID);
			
			//RSAPrivateCrtKeyParameters ConsolePrivKey = LoadConsolePrivateKey(ConPrivKeyExp, ConPrivKeyParams);
			
			ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		    buffer.putLong(System.nanoTime() + (11644473600L*1000000L));
		    byte[] Time = Utilities.Reverse(buffer.array());
		    
		    byte[] Timestamp = KVUtilities.GenerateTimestamp();
		    
		    /* PURE TESTING HERE 
		    
		    byte[] ts = Utilities.HexToBytes("301AA011180F32303136303832393133353534375AA10502030B3543");
		    byte[] testingkey = Utilities.HexToBytes("D30F99EBF9E41DD8F267B3BAC5F97679"); //47D5671F5BE76A944EAB344DC145EFA7B7C76C740660DA140A88C4548E79F7C6F11B58EF674762617150");
		    //73FC6AFA9102ED137CD45A7E1BB30DF0
		    
		    byte[] tmp3 = Utilities.RC4HmacEncrypt(testingkey, ts, 1);
		    Utilities.WriteFile("TS.bin", ts);
		    Utilities.WriteFile("TSEnc.bin", tmp3);
		    
		    byte[]tmp99 = Utilities.RC4HmacDecrypt(testingkey, tmp3, 1);
		    Utilities.WriteFile("TSDec.bin", tmp99);
		    
		    DONE TESTING NOW */
		    
		    
		    //this is the original
		    byte[] tmp3 = KVUtilities.RC4HmacEncrypt(tmp, Timestamp, 1);
		    
		    byte[] tmp4 = MessageDigest.getInstance("SHA1").digest(tmp);
		    
		    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		    sha1.update(Time, 0, 8);
		    sha1.update(ConsoleSerial, 0, 12);
		    sha1.update(tmp4, 0, 20);
		    byte[] Hash = sha1.digest();
		    
		    
		    /*Signature signer = Signature.getInstance("SHA1withRSA", "BC");
		    signer.initSign(ConsolePrivKey);
		    signer.update(Hash);
		    byte[] tmp5 = signer.sign();*/
		    
		    RSAPrivateCrtKeyParameters privKeyParams = LoadConsolePrivateKey(ConPrivKeyExp, ConPrivKeyParams);
		    AsymmetricBlockCipher rsaEngine = new PKCS1Encoding(new RSABlindedEngine());
		    rsaEngine.init(true, privKeyParams);

		    DigestInfo dInfo = new DigestInfo(new AlgorithmIdentifier(X509ObjectIdentifiers.id_SHA1, DERNull.INSTANCE), Hash);
		    byte[] tmp5 = dInfo.getEncoded(ASN1Encoding.DER);

		    tmp5 = rsaEngine.processBlock(tmp5, 0, tmp5.length);

		    XMacsModifiedRequest.position(0x12C);
		    XMacsModifiedRequest.put(Time);
		    XMacsModifiedRequest.position(0x134);
		    XMacsModifiedRequest.put(ConsoleSerial);
		    XMacsModifiedRequest.position(0x140);
		    XMacsModifiedRequest.put(Utilities.Reverse(tmp5));
		    XMacsModifiedRequest.position(0x1C0);
		    XMacsModifiedRequest.put(XeConsoleCertificate);
		    XMacsModifiedRequest.position(0x3E0);
		    XMacsModifiedRequest.put(Arrays.copyOfRange(tmp3, 0, 0x34));
		    XMacsModifiedRequest.position(0x430);
		    XMacsModifiedRequest.put(Arrays.copyOfRange(ClientName, 0, 15));
		    
		    DatagramSocket client = new DatagramSocket(); 
		    client.setSoTimeout(5000);
		    DatagramPacket packet = new DatagramPacket(XMacsModifiedRequest.array(), XMacsModifiedRequest.array().length, InetAddress.getByName("XEAS.XBOXLIVE.COM"), 0x58);
		    client.send(packet);

		    byte[] Receive = new byte[750];
		    packet = new DatagramPacket(Receive, 750, InetAddress.getByName("XEAS.XBOXLIVE.COM"), 0x58);
		    long st = System.currentTimeMillis();
		    while(true){
		    	try {
		    		if(System.currentTimeMillis()-st > 15000) return null; //15 second timeout
				    client.receive(packet);
				    client.close();
				    break;
		    	} catch (SocketTimeoutException e){
		    		client.send(packet);
		    	}
		    }

			int x = Utilities.ByteToInt(Receive);
			if(x == 0x7E793077) {
				return null;
			}
		    
		    byte[] tmp6 = Arrays.copyOfRange(Receive, 0x35, 0x35 + 0x6C);
		    
		    byte[] tmp7 = KVUtilities.RC4HmacDecrypt(KVUtilities.ComputeKdcNoonce(tmp), tmp6, 0x4B3);

			return Arrays.copyOfRange(tmp7, 0x4C, 0x4C + 0x10);
			
		} catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | /*InvalidKeyException | SignatureException |*/ IOException | /*NoSuchProviderException |*/ DataLengthException | CryptoException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static Cipher LoadXMacsKey(){
		try {
			//get generator values
			String ExponentBytes = Utilities.bytesToHex(Arrays.copyOfRange(XMacsPublicKey, 0x4, 0x4 + 0x4));
			String ModulusBytes = Utilities.bytesToHex(Utilities.Reverse8(Arrays.copyOfRange(XMacsPublicKey, 0x10, 0x10 + 0x100)));
		
			BigInteger Modulus = new BigInteger(ModulusBytes, 16);
			BigInteger Exponent = new BigInteger(ExponentBytes, 16);
		
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		
			RSAPublicKeySpec rsaKeySpec = new RSAPublicKeySpec(Modulus, Exponent);
			PublicKey publicKey = keyFactory.generatePublic(rsaKeySpec);
			
			Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		
			return cipher;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | NoSuchProviderException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static RSAPrivateCrtKeyParameters LoadConsolePrivateKey(byte[] ExponentBytes, byte[] KeyParams){
		try {
			BigInteger Exponent = new BigInteger(Utilities.bytesToHex(ExponentBytes), 16);
			BigInteger Modulus = new BigInteger(Utilities.bytesToHex(Utilities.Reverse8(Arrays.copyOfRange(KeyParams, 0, 0x80))), 16);
			BigInteger P = new BigInteger(Utilities.bytesToHex(Utilities.Reverse8(Arrays.copyOfRange(KeyParams, 0x80, 0x80 + 0x40))), 16);
			BigInteger Q = new BigInteger(Utilities.bytesToHex(Utilities.Reverse8(Arrays.copyOfRange(KeyParams, 0xC0, 0xC0 + 0x40))), 16);
			BigInteger DP = new BigInteger(Utilities.bytesToHex(Utilities.Reverse8(Arrays.copyOfRange(KeyParams, 0x100, 0x100 + 0x40))), 16);
			BigInteger DQ = new BigInteger(Utilities.bytesToHex(Utilities.Reverse8(Arrays.copyOfRange(KeyParams, 0x140, 0x140 + 0x40))), 16);
			BigInteger InvQ = new BigInteger(Utilities.bytesToHex(Utilities.Reverse8(Arrays.copyOfRange(KeyParams, 0x180, 0x180 + 0x40))), 16);
			byte[] tmp = new byte[0x80];
			new Random(System.currentTimeMillis()).nextBytes(tmp);
			BigInteger  D = new BigInteger(Utilities.bytesToHex(tmp), 16);

			//KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			
			//RSAPrivateCrtKeySpec rsaKeySpec = new RSAPrivateCrtKeySpec(Modulus, Exponent, D, P, Q, DP, DQ, InvQ);
			//return keyFactory.generatePrivate(rsaKeySpec);
			return new RSAPrivateCrtKeyParameters(Modulus, Exponent, D, P, Q, DP, DQ, InvQ);
		} catch (/*NoSuchAlgorithmException | InvalidKeySpecException*/ Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
