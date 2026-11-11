package classroom.service;

import classroom.app.ClassroomRentalApp;
import classroom.data.RoomCatalog;
import classroom.model.*;
import classroom.view.LoginView;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 역할별 테스트 계정과 쿠키 세션을 관리합니다. */
public final class AuthService {
    private static final Map<String, DemoAccount> ACCOUNTS = Map.of(
            "student01", new DemoAccount("student01", "student123", "student"),
            "관리자1", new DemoAccount("관리자1", "manager123", "manager"),
            "관리자2", new DemoAccount("관리자2", "manager123", "manager"),
            "관리자3", new DemoAccount("관리자3", "manager123", "manager"),
            "admin01", new DemoAccount("admin01", "admin123", "admin"));
    private static final Map<String, DemoAccount> SESSIONS = new ConcurrentHashMap<>();
    private AuthService() { }

    public static void login(HttpExchange e, Map<String, String> form) throws IOException {
        DemoAccount account = ACCOUNTS.get(form.get("username"));
        if (account == null || !account.password().equals(form.get("password"))) {
            LoginView.sendInvalidCredentials(e);
            return;
        }
        String token = UUID.randomUUID().toString();
        SESSIONS.put(token, account);
        e.getResponseHeaders().add("Set-Cookie", "SESSION=" + token + "; Path=/; HttpOnly; SameSite=Lax");
        ClassroomRentalApp.redirect(e, switch (account.role()) { case "student" -> "/student"; case "manager" -> "/manager"; default -> "/admin"; });
    }

    public static String role(HttpExchange e) {
        String cookie = e.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) return "";
        for (String part : cookie.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals("SESSION")) {
                DemoAccount account = SESSIONS.get(pair[1]);
                return account == null ? "" : account.role();
            }
        }
        return "";
    }

    public static String username(HttpExchange e) {
        String cookie = e.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) return "";
        for (String part : cookie.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals("SESSION")) {
                DemoAccount account = SESSIONS.get(pair[1]);
                return account == null ? "" : account.username();
            }
        }
        return "";
    }

    public static void logout(HttpExchange e) throws IOException {
        String cookie = e.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) for (String part : cookie.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals("SESSION")) SESSIONS.remove(pair[1]);
        }
        e.getResponseHeaders().add("Set-Cookie", "SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        ClassroomRentalApp.redirect(e, "/");
    }
}
