import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class CheckPwd {
  public static void main(String[] args) {
    String hash = "$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy";
    String[] candidates = {"123456", "admin", "lumen", "admin123", "12345678", "Aa123456", "password"};
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    for (String c : candidates) {
      System.out.println(c + "=" + encoder.matches(c, hash));
    }
  }
}
