package classroom.service;

import classroom.app.ClassroomRentalApp;
import classroom.data.RoomCatalog;
import classroom.model.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 신청 입력 검사, 시간 충돌 확인, 승인·반려 처리를 담당합니다. */
public final class RentalService {
    private RentalService() { }

    public static synchronized void apply(Map<String, String> form) {
        String name = ClassroomRentalApp.required(form, "name");
        String studentId = ClassroomRentalApp.required(form, "studentId");
        if (!studentId.matches("[0-9]{5,20}")) throw new IllegalArgumentException("학번은 숫자로 입력해 주세요.");
        int roomId;
        try { roomId = Integer.parseInt(ClassroomRentalApp.required(form, "roomId")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("강의실 선택을 확인해 주세요."); }
        Room room = ClassroomRentalApp.ROOMS.stream().filter(r -> r.id == roomId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("목록에 있는 강의실을 선택해 주세요."));
        LocalDate date;
        try { date = LocalDate.parse(ClassroomRentalApp.required(form, "date")); }
        catch (java.time.DateTimeException e) { throw new IllegalArgumentException("대여 날짜를 선택해 주세요."); }
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("오늘 이후 날짜를 선택해 주세요.");
        java.time.LocalTime start;
        try { start = java.time.LocalTime.parse(ClassroomRentalApp.required(form, "start")); }
        catch (java.time.DateTimeException e) { throw new IllegalArgumentException("대여 시작 시간을 다시 선택해 주세요."); }
        int duration;
        try { duration = Integer.parseInt(ClassroomRentalApp.required(form, "duration")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("이용 시간을 다시 선택해 주세요."); }
        if (duration < 30 || duration > 120 || duration % 30 != 0 || start.isBefore(java.time.LocalTime.of(9, 0))
                || start.plusMinutes(duration).isAfter(java.time.LocalTime.of(22, 0)))
            throw new IllegalArgumentException("이용 시간은 30분부터 최대 2시간, 09:00~22:00 사이로 선택해 주세요.");
        java.time.LocalTime end = start.plusMinutes(duration);
        if (date.equals(LocalDate.now()) && start.isBefore(java.time.LocalTime.now()))
            throw new IllegalArgumentException("오늘 이미 지난 시간은 선택할 수 없습니다.");
        String purposeType = ClassroomRentalApp.required(form, "purposeType");
        if (!List.of("동아리 모임", "팀 프로젝트 회의", "기타").contains(purposeType))
            throw new IllegalArgumentException("목록에서 이용 목적을 선택해 주세요.");
        String purpose = purposeType.equals("기타") ? ClassroomRentalApp.required(form, "otherPurpose") : purposeType;
        String details = ClassroomRentalApp.required(form, "details");
        if (ClassroomRentalApp.hasCourseConflict(room, date, start, end))
            throw new IllegalArgumentException("선택한 시간에 정규 수업이 있는 강의실입니다. 다른 시간이나 강의실을 선택해 주세요.");
        boolean occupied = ClassroomRentalApp.APPLICATIONS.stream().anyMatch(a -> a.roomId == roomId && a.date.equals(date.toString())
                && !a.status.equals("반려") && start.isBefore(a.end) && a.start.isBefore(end));
        if (occupied) throw new IllegalArgumentException("선택한 강의실과 시간에 다른 신청이 있습니다. 시간표를 다시 확인해 주세요.");
        ClassroomRentalApp.APPLICATIONS.add(new Application(ClassroomRentalApp.nextId++, name, studentId, room.id, room.name,
                date.toString(), start, end, purpose, details));
    }

    public static synchronized void decide(String path, Map<String, String> form, String managerName) {
        int id = Integer.parseInt(path.substring("/decision/".length()));
        String choice = ClassroomRentalApp.required(form, "choice");
        Application app = ClassroomRentalApp.APPLICATIONS.stream().filter(a -> a.id == id).findFirst().orElseThrow();
        boolean assigned = ClassroomRentalApp.ROOM_MANAGERS.getOrDefault(app.roomId, List.of()).stream()
                .anyMatch(manager -> manager.name.equals(managerName));
        if (!assigned) throw new IllegalArgumentException("이 강의실의 담당 관리자만 신청을 처리할 수 있습니다.");
        if (!app.status.equals("대기")) return;
        if (choice.equals("승인")) {
            Room room = ClassroomRentalApp.ROOMS.stream().filter(r -> r.id == app.roomId).findFirst().orElseThrow();
            if (ClassroomRentalApp.hasCourseConflict(room, LocalDate.parse(app.date), app.start, app.end)) {
                app.status = "반려"; app.reason = "정규 수업 시간과 겹쳐 승인할 수 없습니다."; return;
            }
            boolean conflict = ClassroomRentalApp.APPLICATIONS.stream().anyMatch(a -> a != app && a.roomId == app.roomId
                    && a.date.equals(app.date) && !a.status.equals("반려") && a.start.isBefore(app.end) && app.start.isBefore(a.end));
            if (conflict) { app.status = "반려"; app.reason = "처리 중 동일 시간에 다른 신청이 승인되었습니다."; return; }
            app.status = "승인";
        } else {
            app.status = "반려";
            app.reason = form.getOrDefault("reason", "관리자 검토 결과 반려되었습니다.").trim();
            if (app.reason.isEmpty()) app.reason = "관리자 검토 결과 반려되었습니다.";
        }
    }
}
