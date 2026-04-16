import CryptoJS from "crypto-js";

const DEFAULT_ENCODE_KEY = "thanks,lumencloud";

function toWordArray(bytes) {
  const words = [];
  for (let i = 0; i < bytes.length; i += 1) {
    words[i >>> 2] |= bytes[i] << (24 - (i % 4) * 8);
  }
  return CryptoJS.lib.WordArray.create(words, bytes.length);
}

function normalizeKeyBytes(encodeKey = DEFAULT_ENCODE_KEY) {
  const encoded = new TextEncoder().encode(encodeKey);
  const normalized = new Uint8Array(16);
  normalized.set(encoded.slice(0, 16));
  return normalized;
}

export function encryptPassword(rawPassword, encodeKey = DEFAULT_ENCODE_KEY) {
  if (!rawPassword) {
    return rawPassword;
  }
  const keyBytes = normalizeKeyBytes(encodeKey);
  const key = toWordArray(keyBytes);
  const encrypted = CryptoJS.AES.encrypt(CryptoJS.enc.Utf8.parse(rawPassword), key, {
    iv: key,
    mode: CryptoJS.mode.CFB,
    padding: CryptoJS.pad.NoPadding
  });
  return encrypted.ciphertext.toString(CryptoJS.enc.Hex);
}

export function serializePassword(client, rawPassword) {
  if (!client?.encryptPassword) {
    return rawPassword;
  }
  return encryptPassword(rawPassword);
}

