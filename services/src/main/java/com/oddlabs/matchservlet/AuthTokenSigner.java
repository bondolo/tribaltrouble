package com.oddlabs.matchservlet;

import jakarta.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * Generates and verifies cryptographic authentication tokens for matchmaking logins.
 */
@Singleton
public final class AuthTokenSigner {
    private static final String SIGN_ALGORITHM = "SHA1WithRSA";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 1024;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    AuthTokenSigner() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    byte[] createSignedToken(String username) {
        try {
            long timestamp = System.currentTimeMillis();
            ByteArrayOutputStream signBytes = new ByteArrayOutputStream();
            try (DataOutputStream signData = new DataOutputStream(signBytes)) {
                signData.writeUTF(username);
                signData.writeLong(timestamp);
            }

            Signature signer = Signature.getInstance(SIGN_ALGORITHM);
            signer.initSign(privateKey);
            signer.update(signBytes.toByteArray());
            byte[] signedBytes = signer.sign();

            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(outBytes)) {
                out.writeLong(timestamp);
                out.writeInt(signedBytes.length);
                out.write(signedBytes);
            }
            return outBytes.toByteArray();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to sign authentication token", e);
        }
    }

    boolean verifyToken(String username, long timestamp, byte[] signature) {
        try {
            ByteArrayOutputStream signBytes = new ByteArrayOutputStream();
            try (DataOutputStream signData = new DataOutputStream(signBytes)) {
                signData.writeUTF(username);
                signData.writeLong(timestamp);
            }
            Signature verifier = Signature.getInstance(SIGN_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(signBytes.toByteArray());
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IOException e) {
            return false;
        }
    }

    PublicKey getPublicKey() {
        return publicKey;
    }
}
