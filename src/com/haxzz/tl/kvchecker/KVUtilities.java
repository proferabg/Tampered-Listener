package com.haxzz.tl.kvchecker;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.haxzz.tl.utilities.Utilities;

public class KVUtilities {
	
	public static byte[] GenerateTimestamp2(){
		DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMddHHmmss'Z");
		return fmt.print(new DateTime(DateTimeZone.UTC)).getBytes();
	}
	
	public static byte[] GenerateTimestamp(){
		DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMddHHmmss'Z");
		ByteBuffer x = ByteBuffer.wrap(Utilities.hexToBytes("301aa011180f32303132313231323139303533305aa10502030b3543"));
		x.position(6);
		x.put(fmt.print(new DateTime(DateTimeZone.UTC)).getBytes());
		return x.array();
	}
	
	public static byte[] RC4HmacDecrypt(byte[] key, byte[] data, int Usage){
		try {
			SecretKeySpec key1 = new SecretKeySpec(key, "HMACMD5");
			Mac mac = Mac.getInstance("HMACMD5");
			mac.init(key1);
			
			byte[] key2 = mac.doFinal(Utilities.Reverse(Utilities.IntToByte(Usage)));

			SecretKeySpec key3 = new SecretKeySpec(key2, "HMACMD5");
			mac.init(key3);
			
			byte[] key4 = mac.doFinal(Arrays.copyOfRange(data, 0, 0x10));
			
			byte[] data1 = Arrays.copyOfRange(data, 0x10, data.length);

			SecretKeySpec key5 = new SecretKeySpec(key4, "RC4");
			Cipher cipher = Cipher.getInstance("RC4");
			cipher.init(Cipher.DECRYPT_MODE, key5);
			
			return cipher.doFinal(data1);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		
		
	}
	
	public static byte[] RC4HmacEncrypt(byte[] key, byte[] data, int Usage){
		try {
			SecretKeySpec key1 = new SecretKeySpec(key, "HMACMD5");
			Mac mac = Mac.getInstance("HMACMD5", "BC");
			mac.init(key1);
			
			byte[] key2 = mac.doFinal(Utilities.Reverse(Utilities.IntToByte(Usage)));
			
			ByteBuffer buffer = ByteBuffer.allocate(data.length + 8);
			buffer.put(Utilities.hexToBytes("9b6bfacb5c488190"), 0, 8);
			buffer.position(8);
			buffer.put(data);
			
			Key key3 = new SecretKeySpec(key2, "HMACMD5");
			mac.init(key3);
			
			byte[] key4 = mac.doFinal(buffer.array());
			byte[] key5 = mac.doFinal(key4);
			
			SecretKeySpec key6 = new SecretKeySpec(key5, "RC4");
			Cipher cipher = Cipher.getInstance("RC4");
			cipher.init(Cipher.ENCRYPT_MODE, key6);
			
			byte[] rc4d = cipher.doFinal(buffer.array());
			
			ByteBuffer finalBuffer = ByteBuffer.allocate(data.length + 0x18);
			finalBuffer.put(key4, 0, 0x10);
			buffer.position(0x10);
			finalBuffer.put(rc4d);
			
			return finalBuffer.array();
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	public static byte[] ComputeKdcNoonce(byte[] Key0) {
		try {
	        byte[] tmp = new byte[] { 0x73, 0x69, 0x67, 110, 0x61, 0x74, 0x75, 0x72, 0x65, 0x6b, 0x65, 0x79, 0 };
	        SecretKeySpec key1 = new SecretKeySpec(Key0, "HMACMD5");
			Mac mac = Mac.getInstance("HMACMD5");
			mac.init(key1);
			
			byte[] Key2 = mac.doFinal(tmp);
	        
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        md.update(new byte[]{ 0x2, 0x4, 0x0, 0x0});
	        md.update(new byte[]{ 0x0, 0x0, 0x0, 0x0});
	        byte[] Hash = md.digest();
	        
	        Key key3 = new SecretKeySpec(Key2, "HMACMD5");
	        mac.init(key3);
	        
	        return mac.doFinal(Hash);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
			return null;
		}
    }
	
	public static byte[] ComputeClientName(byte[] ConsoleID) {
		long b = Utilities.ByteToLong(ConsoleID);
		String c = "XE."+(b >> 4L)+""+(b & 15L)+"@xbox.com";
		if (c.length() != 0x18) {
            for (int a = 0; a < (0x18 - (c.length() - 1)); a++){
                c = c.substring(0, 3) + "0" + c.substring(3, c.length());
            }
        }
        return c.getBytes(StandardCharsets.US_ASCII);
    }
	
	public static byte[] GetTitleAuthData(byte[] Key, byte[] titleData){
		try {
			SecretKeySpec key = new SecretKeySpec(ComputeKdcNoonce(Key), "HmacSHA1");
			Mac mac = Mac.getInstance("HMACSHA1");
			mac.init(key);
			ByteBuffer dest = ByteBuffer.allocate(0x52);
			dest.position(0);
			dest.put(Arrays.copyOfRange(mac.doFinal(titleData), 0, 0x10));
			dest.position(0x10);
			dest.put(titleData);
			return dest.array();
		} catch (NoSuchAlgorithmException | InvalidKeyException e){
			e.printStackTrace();
			return null;
		}
	}
}
