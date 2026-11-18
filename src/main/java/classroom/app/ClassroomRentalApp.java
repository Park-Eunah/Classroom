package classroom.app;

import classroom.data.RoomCatalog;
import classroom.data.Database;
import classroom.model.*;
import classroom.service.*;
import classroom.view.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ClassroomRentalApp {
    public static final List<Room> ROOMS = RoomCatalog.ROOMS;
    public static final Map<String, String> COURSE_LABELS = RoomCatalog.COURSE_LABELS;
    public static final Map<String, String> COURSE_PROFESSORS = RoomCatalog.COURSE_PROFESSORS;
    public static final List<String> ADMIN_OPTIONS = List.of("관리자1", "관리자2", "관리자3");
    public static final List<Application> APPLICATIONS = new ArrayList<>();
    public static int nextRoomManagerId = ROOMS.size() + 1;
    public static final Map<Integer, List<RoomManager>> ROOM_MANAGERS = createRoomManagerMap();
    public static int nextId = 1;

    public static void main(String[] args) throws IOException {
        Database.initialize();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", ClassroomRentalApp::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("강의실 대여 앱이 포트 " + port + "에서 실행 중입니다.");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/healthz")) { send(exchange, "ok"); return; }
            if (path.equals("/styles.css")) { sendCss(exchange); return; }
            if ("POST".equals(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                if (path.equals("/login")) { AuthService.login(exchange, form); return; }
                if (path.equals("/logout")) { AuthService.logout(exchange); return; }
                if ("/apply".equals(path) && "student".equals(AuthService.role(exchange))) { apply(form); redirect(exchange, "/student"); }
                else if (path.startsWith("/decision/") && "manager".equals(AuthService.role(exchange))) { RentalService.decide(path, form, AuthService.username(exchange)); redirect(exchange, "/manager"); }
                else if (path.equals("/admin/manager/add") && "admin".equals(AuthService.role(exchange))) { addRoomManager(form); redirect(exchange, adminManagerRedirect(form)); }
                else if (path.equals("/admin/manager/remove") && "admin".equals(AuthService.role(exchange))) { removeRoomManager(form); redirect(exchange, adminManagerRedirect(form)); }
                else redirect(exchange, "/");
                return;
            }
            if (path.equals("/logout")) { AuthService.logout(exchange); return; }
            send(exchange, page(path, exchange.getRequestURI().getRawQuery(), AuthService.role(exchange), AuthService.username(exchange)));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() == null ? "입력한 정보를 확인하고 다시 시도해 주세요." : e.getMessage();
            send(exchange, "<h1>요청을 처리할 수 없습니다</h1><p>" + esc(message) + "</p><a href='/'>로그인 화면으로 돌아가기</a>");
        }
    }

    private static void apply(Map<String, String> form) { RentalService.apply(form); }
    private static String page(String path, String query, String role, String username) {
        if (path.equals("/login")) return loginPage();
        if (path.equals("/student")) return "student".equals(role) ? studentPage() : loginPage();
        if (path.equals("/manager")) return "manager".equals(role) ? ManagerView.render(username) : loginPage();
        if (path.equals("/admin")) return "admin".equals(role) ? adminPage(query) : loginPage();
        return loginPage();
    }

    public static String shell(String title, String subtitle, String content) {
        boolean showNavigation = !"로그인".equals(title);
        String navigation = showNavigation
                ? "<a class='back' href='/'>← 메인 화면</a> <a class='back' style='float:right' href='/logout'>로그아웃</a>"
                : "";
        return """
                <!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>강의실 대여</title><link rel="stylesheet" href="/styles.css"><header>%%NAVIGATION%%<h1>%%TITLE%%</h1><div class="sub">%%SUBTITLE%%</div></header><main>%%CONTENT%%</main></html>
                """.replace("%%NAVIGATION%%", navigation).replace("%%TITLE%%", esc(title)).replace("%%SUBTITLE%%", esc(subtitle)).replace("%%CONTENT%%", content);
    }

    private static String homePage() {
        return loginPage();
    }

    private static String loginPage() { return LoginView.render(); }

    private static String studentPage() { return StudentView.render(); }
    private static String applicationsList(boolean allowDecision) { return ApplicationView.list(allowDecision); }
    private static String adminPage(String query) { return AdminView.render(query); }
    private static void addRoomManager(Map<String, String> form) { RoomManagerService.add(form); }
    private static void removeRoomManager(Map<String, String> form) { RoomManagerService.remove(form); }
    private static String adminManagerRedirect(Map<String, String> form) {
        if ("managers".equals(form.get("returnView")))
            return "/admin?view=managers&manager=" + java.net.URLEncoder.encode(form.getOrDefault("managerName", "관리자1"), StandardCharsets.UTF_8);
        return "/admin?roomId=" + required(form, "roomId");
    }
    private static Map<Integer, List<RoomManager>> createRoomManagerMap() {
        Map<Integer, List<RoomManager>> map = new LinkedHashMap<>();
        for (Room room : ROOMS) {
            map.put(room.id, new ArrayList<>(List.of(new RoomManager(room.id, "관리자1"))));
        }
        return map;
    }

    private static void appendApplications(StringBuilder html, boolean allowDecision) { html.append(applicationsList(allowDecision)); }

    public static Map<String, String> parseForm(String body) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }
    public static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            String label = switch (key) { case "name" -> "신청자 이름"; case "date" -> "대여 날짜"; case "time" -> "대여 시간"; case "purpose" -> "사용 목적"; case "choice" -> "처리 결과"; default -> "필수 항목"; };
            throw new IllegalArgumentException(label + "을(를) 입력하거나 선택해 주세요.");
        }
        return value.trim();
    }
    public static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    public static String js(String s) { return s.replace("\\", "\\\\").replace("'", "\\'").replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026").replace("\n", " ").replace("\r", " "); }
    public static String capacity(Room room) { return room.capacity > 0 ? "정원 " + room.capacity + "명" : "정원 미확인"; }
    public static boolean hasCourseConflict(Room room, LocalDate date, java.time.LocalTime start, java.time.LocalTime end) {
        return RoomCatalog.hasCourseConflict(room, date, start, end);
    }
    public static void send(HttpExchange e, String body) throws IOException {
        String document = body.startsWith("<!doctype html>") ? body
                : "<!doctype html><html lang='ko'><meta charset='utf-8'><body>" + body + "</body></html>";
        byte[] bytes = document.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); e.sendResponseHeaders(200, bytes.length); e.getResponseBody().write(bytes); e.close();
    }
    private static void sendCss(HttpExchange e) throws IOException {
        try (var css = ClassroomRentalApp.class.getResourceAsStream("/static/styles.css")) {
            if (css == null) { e.sendResponseHeaders(404, -1); e.close(); return; }
            byte[] bytes = css.readAllBytes();
            e.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
            e.getResponseHeaders().set("Cache-Control", "no-cache");
            e.sendResponseHeaders(200, bytes.length);
            e.getResponseBody().write(bytes);
            e.close();
        }
    }
    public static void redirect(HttpExchange e, String path) throws IOException { e.getResponseHeaders().set("Location", path); e.sendResponseHeaders(303, -1); e.close(); }

}
