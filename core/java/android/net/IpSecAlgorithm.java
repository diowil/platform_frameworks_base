/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.net;

import android.annotation.NonNull;
import android.annotation.StringDef;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.HexDump;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * This class represents a single algorithm that can be used by an {@link IpSecTransform}.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4301">RFC 4301, Security Architecture for the
 * Internet Protocol</a>
 */
public final class IpSecAlgorithm implements Parcelable {
    /**
     * AES-CBC Encryption/Ciphering Algorithm.
     *
     * <p>Valid lengths for this key are {128, 192, 256}.
     */
    public static final String CRYPT_AES_CBC = "cbc(aes)";

    /**
     * MD5 HMAC Authentication/Integrity Algorithm. <b>This algorithm is not recommended for use in
     * new applications and is provided for legacy compatibility with 3gpp infrastructure.</b>
     *
     * <p>Valid truncation lengths are multiples of 8 bits from 96 to (default) 128.
     */
    public static final String AUTH_HMAC_MD5 = "hmac(md5)";

    /**
     * SHA1 HMAC Authentication/Integrity Algorithm. <b>This algorithm is not recommended for use in
     * new applications and is provided for legacy compatibility with 3gpp infrastructure.</b>
     *
     * <p>Valid truncation lengths are multiples of 8 bits from 96 to (default) 160.
     */
    public static final String AUTH_HMAC_SHA1 = "hmac(sha1)";

    /**
     * SHA256 HMAC Authentication/Integrity Algorithm.
     *
     * <p>Valid truncation lengths are multiples of 8 bits from 96 to (default) 256.
     */
    public static final String AUTH_HMAC_SHA256 = "hmac(sha256)";

    /**
     * SHA384 HMAC Authentication/Integrity Algorithm.
     *
     * <p>Valid truncation lengths are multiples of 8 bits from 192 to (default) 384.
     */
    public static final String AUTH_HMAC_SHA384 = "hmac(sha384)";

    /**
     * SHA512 HMAC Authentication/Integrity Algorithm.
     *
     * <p>Valid truncation lengths are multiples of 8 bits from 256 to (default) 512.
     */
    public static final String AUTH_HMAC_SHA512 = "hmac(sha512)";

    /**
     * AES-GCM Authentication/Integrity + Encryption/Ciphering Algorithm.
     *
     * <p>Valid lengths for keying material are {160, 224, 288}.
     *
     * <p>As per <a href="https://tools.ietf.org/html/rfc4106#section-8.1">RFC4106 (Section
     * 8.1)</a>, keying material consists of a 128, 192, or 256 bit AES key followed by a 32-bit
     * salt. RFC compliance requires that the salt must be unique per invocation with the same key.
     *
     * <p>Valid ICV (truncation) lengths are {64, 96, 128}.
     */
    public static final String AUTH_CRYPT_AES_GCM = "rfc4106(gcm(aes))";

    /** @hide */
    @StringDef({
        CRYPT_AES_CBC,
        AUTH_HMAC_MD5,
        AUTH_HMAC_SHA1,
        AUTH_HMAC_SHA256,
        AUTH_HMAC_SHA512,
        AUTH_CRYPT_AES_GCM
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AlgorithmName {}

    private final String mName;
    private final byte[] mKey;
    private final int mTruncLenBits;

    /**
     * Creates an IpSecAlgorithm of one of the supported types. Supported algorithm names are
     * defined as constants in this class.
     *
     * @param algorithm name of the algorithm.
     * @param key key padded to a multiple of 8 bits.
     */
    public IpSecAlgorithm(@AlgorithmName String algorithm, @NonNull byte[] key) {
        this(algorithm, key, key.length * 8);
    }

    /**
     * Creates an IpSecAlgorithm of one of the supported types. Supported algorithm names are
     * defined as constants in this class.
     *
     * <p>This constructor only supports algorithms that use a truncation length. i.e.
     * Authentication and Authenticated Encryption algorithms.
     *
     * @param algorithm name of the algorithm.
     * @param key key padded to a multiple of 8 bits.
     * @param truncLenBits number of bits of output hash to use.
     */
    public IpSecAlgorithm(@AlgorithmName String algorithm, @NonNull byte[] key, int truncLenBits) {
        if (!isTruncationLengthValid(algorithm, truncLenBits)) {
            throw new IllegalArgumentException("Unknown algorithm or invalid length");
        }
        mName = algorithm;
        mKey = key.clone();
        mTruncLenBits = Math.min(truncLenBits, key.length * 8);
    }

    /** Get the algorithm name */
    public String getName() {
        return mName;
    }

    /** Get the key for this algorithm */
    public byte[] getKey() {
        return mKey.clone();
    }

    /** Get the truncation length of this algorithm, in bits */
    public int getTruncationLengthBits() {
        return mTruncLenBits;
    }

    /* Parcelable Implementation */
    public int describeContents() {
        return 0;
    }

    /** Write to parcel */
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mName);
        out.writeByteArray(mKey);
        out.writeInt(mTruncLenBits);
    }

    /** Parcelable Creator */
    public static final Parcelable.Creator<IpSecAlgorithm> CREATOR =
            new Parcelable.Creator<IpSecAlgorithm>() {
                public IpSecAlgorithm createFromParcel(Parcel in) {
                    return new IpSecAlgorithm(in);
                }

                public IpSecAlgorithm[] newArray(int size) {
                    return new IpSecAlgorithm[size];
                }
            };

    private IpSecAlgorithm(Parcel in) {
        mName = in.readString();
        mKey = in.createByteArray();
        mTruncLenBits = in.readInt();
    }

    private static boolean isTruncationLengthValid(String algo, int truncLenBits) {
        switch (algo) {
            case CRYPT_AES_CBC:
                return (truncLenBits == 128 || truncLenBits == 192 || truncLenBits == 256);
            case AUTH_HMAC_MD5:
                return (truncLenBits >= 96 && truncLenBits <= 128);
            case AUTH_HMAC_SHA1:
                return (truncLenBits >= 96 && truncLenBits <= 160);
            case AUTH_HMAC_SHA256:
                return (truncLenBits >= 96 && truncLenBits <= 256);
            case AUTH_HMAC_SHA384:
                return (truncLenBits >= 192 && truncLenBits <= 384);
            case AUTH_HMAC_SHA512:
                return (truncLenBits >= 256 && truncLenBits <= 512);
            case AUTH_CRYPT_AES_GCM:
                return (truncLenBits == 64 || truncLenBits == 96 || truncLenBits == 128);
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{mName=")
                .append(mName)
                .append(", mKey=")
                .append(Build.IS_DEBUGGABLE ? HexDump.toHexString(mKey) : "<hidden>")
                .append(", mTruncLenBits=")
                .append(mTruncLenBits)
                .append("}")
                .toString();
    }

    /** package */
    static boolean equals(IpSecAlgorithm lhs, IpSecAlgorithm rhs) {
        if (lhs == null || rhs == null) return (lhs == rhs);
        return (lhs.mName.equals(rhs.mName)
                && Arrays.equals(lhs.mKey, rhs.mKey)
                && lhs.mTruncLenBits == rhs.mTruncLenBits);
    }
};
