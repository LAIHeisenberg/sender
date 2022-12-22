package com.laiyz.util;

import cn.hutool.core.codec.Base64;
import com.twmacinta.util.MD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 
 *
 * @author bbstone
 *
 */
@Slf4j
public class CipherUtil {
	public static String hex(byte[] bytes) {
		return MD5.asHex(bytes);
	}

	public static byte[] md5Bytes(String str) {
		if (StringUtils.isBlank(str))
			return null;
		MD5 md5 = new MD5();
		try {
			md5.Update(str, null);
		} catch (UnsupportedEncodingException e) {
			log.error("calculate string's md5 error.", e);
		}
		return md5.Final();
	}

	public static String md5(String str) {
		if (StringUtils.isBlank(str))
			return null;
		MD5 md5 = new MD5();
		try {
			md5.Update(str, null);
		} catch (UnsupportedEncodingException e) {
			log.error("calculate string's md5 error.", e);
		}
		String hash = md5.asHex();
		return hash;
	}

	public static String md5(byte[] bytes) {
		return md5(MD5.asHex(bytes));
	}

	public static byte[] md5Bytes(byte[] bytes) {
		return md5Bytes(MD5.asHex(bytes));
	}


	public static String md5(File file) {
		try {
			return MD5.asHex(MD5.getHash(file));
		} catch (IOException e) {
			log.error("calculate file md5 error.", e);
		}
		return null;
	}

	// Hutools encode/decode
	public static String encode(byte[] source) {
		return Base64.encode(source);
	}

	// Hutools encode/decode
	public static byte[] decode(String encodedStr) {
		return Base64.decode(encodedStr);
	}


	public static void main(String[] args) {
//		log.info("string.md5: {}", md5("hello"));
//		log.info("byte.md5: {}", MD5.asHex(md5Bytes("hello")));
//
//		log.info("server.file.checksum: {}", md5(new File("/Users/bbstone/test/2.1-4.27.xlsx")));
//		log.info("server.file.checksum: {}", md5(new File("/Users/bbstone/test/2.1-4.27.xlsx.part")));
//		log.info("client.file.checksum: {}", md5(new File("/Users/bbstone/test-bak/2.1-4.27.xlsx.part")));
//		log.info("client.file.checksum: {}", md5(new File("/Users/bbstone/test-bak/2.1-4.27.xlsx.part 2")));
//		File file = new File("/Users/bbstone/workdir/assets/assets_docs/00-profile/resumes/主简历-技术经理20190314(精简工作经验和删减项目经验, 工作经验的描述一定要简扼有力，用最精简的描述包括最多的内容，让人产生好奇有继续往下读的欲望）.doc");
//		log.info("abs.path: {}", file.getAbsolutePath());
//		log.info("file.md5: {}", md5(file));


		log.info("max.mem: {} MB", Runtime.getRuntime().maxMemory()/(1024*1024));
	}
}
