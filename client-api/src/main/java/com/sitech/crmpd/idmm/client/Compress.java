package com.sitech.crmpd.idmm.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2014年11月12日 下午5:41:39
 */
public final class Compress {

	/**
	 * @param plaintext
	 * @return the bytes of ciphertext
	 * @throws IOException
	 */
	public static byte[] gzipToBytes(byte[] plaintext) throws IOException {
		if (plaintext == null || plaintext.length == 0) {
			return new byte[0];
		}
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(plaintext);
		gzip.close();
		return out.toByteArray();
	}

	/**
	 * @param ciphertext
	 * @return the bytes of plaintext
	 * @throws IOException
	 */
	public static byte[] gunzipFromBytes(byte[] ciphertext) throws IOException {
		if (ciphertext == null || ciphertext.length == 0) {
			return new byte[0];
		}

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ByteArrayInputStream in = new ByteArrayInputStream(ciphertext);
		final GZIPInputStream gunzip = new GZIPInputStream(in);
		final byte[] buffer = new byte[256];
		int n;
		while ((n = gunzip.read(buffer)) >= 0) {
			out.write(buffer, 0, n);
		}
		return out.toByteArray();
	}

}
