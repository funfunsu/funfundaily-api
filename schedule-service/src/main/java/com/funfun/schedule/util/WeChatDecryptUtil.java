package com.funfun.schedule.util;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.Security;

/**
 * 微信加密信息解密工具（AES-256-CBC）
 */
@Component
public class WeChatDecryptUtil {

    // 解密算法
    private static final String ALGORITHM = "AES/CBC/PKCS7Padding";
    // 加密密钥长度（AES-256 需 32 位密钥）
    private static final int KEY_LENGTH = 32;

    static {
        // 注册 BouncyCastle Provider（AES-256 解密必需）
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 解密微信 encryptedData
     * @param encryptedData 加密数据
     * @param sessionKey 微信 code2session 返回的 sessionKey
     * @param iv 加密向量
     * @return 解密后的用户信息（包含 nickname、avatarUrl 等）
     */
    public JSONObject decrypt(String encryptedData, String sessionKey, String iv) {
        try {
            // 1. Base64 解码 sessionKey、encryptedData、iv
            byte[] sessionKeyBytes = Base64.decodeBase64(sessionKey);
            byte[] encryptedDataBytes = Base64.decodeBase64(encryptedData);
            byte[] ivBytes = Base64.decodeBase64(iv);

            // 2. 校验 sessionKey 长度（AES-256 需 32 位）
            if (sessionKeyBytes.length != KEY_LENGTH) {
                throw new RuntimeException("sessionKey 长度错误，需 32 位");
            }

            // 3. 初始化加密算法
            SecretKeySpec keySpec = new SecretKeySpec(sessionKeyBytes, "AES");
            AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
            params.init(new IvParameterSpec(ivBytes));

            // 4. 解密
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
            byte[] decryptedBytes = cipher.doFinal(encryptedDataBytes);

            // 5. 转换为 JSON 对象（包含 nickname、avatarUrl、gender 等）
            String decryptedStr = new String(decryptedBytes, "UTF-8");
            return JSONObject.parseObject(decryptedStr);
        } catch (Exception e) {
            throw new RuntimeException("微信加密数据解密失败：" + e.getMessage(), e);
        }
    }
}