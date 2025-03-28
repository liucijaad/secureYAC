package ie.dcu.secureYAC;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
*
    This class represents the Curve25519 elliptic curve. It is defined by the
    Montgomery-form equation y^2 = x^3 + 486662 * x^2 + x over the prime field
    mod 2^255 - 19. This curve is also birationally equivalent to the twisted
    Edwards curve 486664 * x^2 + y^2 = 1 + 486660 * x^2 * y^2.
    Implementation based on parameters specified in
    <a href="https://www.ietf.org/rfc/rfc7748">RFC7748</a> and
    <a href="https://martin.kleppmann.com/papers/curve25519.pdf">M. Kleppmann
    "Implementing Curve25519/X25519: A Tutorial on Elliptic Curve Cryptography"</a>.
*
* @author Liucija Paulina Adomaviciute */

public class X25519 {
    public static final BigInteger PRIME =
        new BigInteger("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFED", 16);
    public static final BigInteger BASE_POINT =
        new BigInteger("20AE19A1B8A086B4E01EDD2C7748D14C923D4D7E6D7C61B229E9C5A27ECED3D9", 16);
    
    public X25519() {}

    public static byte[] generatePrivateKey() {
        byte[] privateKey = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(privateKey);
        BigInteger keyInt = Util.byteArrayToBigInteger(privateKey).mod(PRIME);
        if(keyInt.equals(BigInteger.ZERO)) { //Generate new key if private key == 0.
            return generatePrivateKey();
        }
        privateKey = Util.bigIntToByteArray(keyInt, 256);
        privateKey = clamping(privateKey);
        return privateKey;
    }

    public static byte[] generatePublicKey(byte[] privateKey) {
        byte[] publicKey = new byte[32];
        BigInteger tmp = doubleAndAdd(privateKey);
        publicKey = Util.bigIntToByteArray(tmp, 256);
        return publicKey;
    }

    private static byte[] clamping(byte[] key) {
        byte tmp = key[0];
        //Clamp bits 0, 1 and 3 to 0.
        tmp &= ~(1 << 5);
        tmp &= ~(1 << 6);
        tmp &= ~(1 << 7);
        key[0] = tmp;
        tmp = key[31];
        //Clamp bit in 254 to 1 and bit 255 to 0.
        tmp |= 1 << 2;
        tmp &= ~(1 << 1);
        key[31] = tmp;
        return key;
    }

    /**
     * @param n scalar for multiplication.
     * @param v y of base point where x = 9.
     * @return result of n * v computed using double-and-add algorithm. */
    private static BigInteger doubleAndAdd(byte[] n) {
        BigInteger result = BigInteger.ZERO;
        BigInteger addEnd = BASE_POINT;
        for(int i = 31; i >= 0; i--) {
            for(int j = 0; j < 8; j++) {
                byte tmp = n[i];
                if(((tmp >> j) & 1) == 1) {
                    result = result.add(addEnd).mod(PRIME);
                }
                addEnd = addEnd.multiply(BigInteger.valueOf(2)).mod(PRIME);
            }
        }
        return result;
    }
}

