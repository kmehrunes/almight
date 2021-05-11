package com.nexblocks.authguard.jwt;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * A class of helper functions to convert asymmetric keys from base64
 * into a {@link KeyPair} instance containing both the public and
 * private keys. To load the actual keys from PEM files, use {@link KeyLoader}.
 */
public class AsymmetricKeys {
    public static KeyPair rsaFromBase64Keys(final String base64Public, final String base64Private) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return fromBase64Keys(keyFactory, base64Public, base64Private);
    }

    public static KeyPair ecdsaFromBase64Keys(final String base64Public, final String base64Private) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory keyFactory = KeyFactory.getInstance("EC");

        return fromBase64Keys(keyFactory, base64Public, base64Private);
    }

    public static KeyPair eciesFromBase64Keys(final String base64Public, final String base64Private) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory keyFactory = KeyFactory.getInstance("EC");

        return fromBase64Keys(keyFactory, base64Public, base64Private);
    }

    public static KeyPair rsaFromBase64Keys(final byte[] base64Public, final byte[] base64Private) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return fromBase64Keys(keyFactory, base64Public, base64Private);
    }

    public static KeyPair ecdsaFromBase64Keys(final byte[] base64Public, final byte[] base64Private) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory keyFactory = KeyFactory.getInstance("EC");

        return fromBase64Keys(keyFactory, base64Public, base64Private);
    }

    public static KeyPair eciesFromBase64Keys(final byte[] base64Public, final byte[] base64Private) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory keyFactory = KeyFactory.getInstance("EC");

        return fromBase64Keys(keyFactory, base64Public, base64Private);
    }

    public static KeyPair fromBase64Keys(final KeyFactory keyFactory, final String base64Public,
                                         final String base64Private) throws InvalidKeySpecException {
        final byte[] publicKey = Base64.getDecoder().decode(base64Public);
        final byte[] privateKey = Base64.getDecoder().decode(base64Private);

        return fromBase64Keys(keyFactory, publicKey, privateKey);
    }

    public static KeyPair fromBase64Keys(final KeyFactory keyFactory, final byte[] base64Public,
                                         final byte[] base64Private) throws InvalidKeySpecException {
        final X509EncodedKeySpec publicKeySpecs = new X509EncodedKeySpec(base64Public);
        final PKCS8EncodedKeySpec privateKeySpecs = new PKCS8EncodedKeySpec(base64Private);

        return new KeyPair(keyFactory.generatePublic(publicKeySpecs), keyFactory.generatePrivate(privateKeySpecs));
    }
}
