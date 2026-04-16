import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
public class TmpAes {
  public static void main(String[] args) throws Exception {
    byte[] keyBytes = Arrays.copyOf("thanks,lumencloud".getBytes(StandardCharsets.UTF_8), 16);
    Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(keyBytes));
    byte[] out = cipher.doFinal("123456".getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : out) sb.append(String.format("%02x", b));
    System.out.print(sb.toString());
  }
}
