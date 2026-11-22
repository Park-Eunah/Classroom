package classroom.service;

import classroom.app.ClassroomRentalApp;
import classroom.data.RoomCatalog;
import classroom.data.Database;
import classroom.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 강의실 관리자 지정과 해제를 담당합니다. */
public final class RoomManagerService {
    private RoomManagerService() { }

    public static synchronized void add(Map<String, String> form) {
        int roomId = roomId(form);
        if (ClassroomRentalApp.ROOMS.stream().noneMatch(r -> r.id == roomId)) throw new IllegalArgumentException("강의실을 선택해 주세요.");
        int count;
        try { count = Integer.parseInt(ClassroomRentalApp.required(form, "selectionCount")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("관리자 선택 수를 확인해 주세요."); }
        if (count < 1 || count > ClassroomRentalApp.ADMIN_OPTIONS.size()) throw new IllegalArgumentException("관리자는 목록에서 선택해 주세요.");
        List<RoomManager> managers = ClassroomRentalApp.ROOM_MANAGERS.get(roomId);
        List<String> selected = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String name = ClassroomRentalApp.required(form, "managerName" + i);
            if (!ClassroomRentalApp.ADMIN_OPTIONS.contains(name)) throw new IllegalArgumentException("관리자 목록에서 선택해 주세요.");
            if (selected.contains(name)) throw new IllegalArgumentException("같은 관리자를 중복 선택했습니다.");
            if (managers.stream().anyMatch(manager -> manager.name.equals(name))) throw new IllegalArgumentException(name + "은(는) 이미 이 강의실에 지정되어 있습니다.");
            selected.add(name);
        }
        List<RoomManager> added = new ArrayList<>();
        for (String name : selected) added.add(new RoomManager(roomId, name, ClassroomRentalApp.nextRoomManagerId + added.size()));
        saveAssignments(added);
    }

    public static synchronized void addSelected(Map<String, String> form) {
        String managerName = ClassroomRentalApp.required(form, "managerName");
        if (!ClassroomRentalApp.ADMIN_OPTIONS.contains(managerName))
            throw new IllegalArgumentException("관리자 목록에서 선택해 주세요.");

        List<RoomManager> added = new ArrayList<>();
        for (var entry : ClassroomRentalApp.ROOM_MANAGERS.entrySet()) {
            int roomId = entry.getKey();
            if (!"1".equals(form.get("room-" + roomId))) continue;
            if (entry.getValue().stream().anyMatch(manager -> manager.name.equals(managerName))) continue;
            added.add(new RoomManager(roomId, managerName, ClassroomRentalApp.nextRoomManagerId + added.size()));
        }
        if (!added.isEmpty()) saveAssignments(added);
    }

    private static void saveAssignments(List<RoomManager> added) {
        Database.insertRoomManagers(added);
        for (RoomManager assignment : added) {
            ClassroomRentalApp.ROOM_MANAGERS.get(assignment.roomId).add(assignment);
        }
        ClassroomRentalApp.nextRoomManagerId += added.size();
    }

    public static synchronized void remove(Map<String, String> form) {
        int roomId = roomId(form);
        int managerId;
        try { managerId = Integer.parseInt(ClassroomRentalApp.required(form, "managerId")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("삭제할 관리자 선택을 확인해 주세요."); }
        List<RoomManager> managers = ClassroomRentalApp.ROOM_MANAGERS.get(roomId);
        RoomManager target = managers == null ? null : managers.stream().filter(manager -> manager.id == managerId).findFirst().orElse(null);
        if (target == null)
            throw new IllegalArgumentException("해당 강의실에서 관리자를 찾을 수 없습니다.");
        Database.deleteRoomManager(roomId, managerId);
        managers.remove(target);
    }

    public static synchronized void removeSelected(Map<String, String> form) {
        String managerName = ClassroomRentalApp.required(form, "managerName");
        if (!ClassroomRentalApp.ADMIN_OPTIONS.contains(managerName))
            throw new IllegalArgumentException("관리자 목록에서 선택해 주세요.");

        List<RoomManager> selected = new ArrayList<>();
        for (var entry : ClassroomRentalApp.ROOM_MANAGERS.entrySet()) {
            String assignmentId = form.get("assignment-" + entry.getKey());
            if (assignmentId == null) continue;
            entry.getValue().stream()
                    .filter(manager -> manager.name.equals(managerName) && Integer.toString(manager.id).equals(assignmentId))
                    .findFirst().ifPresent(selected::add);
        }
        if (selected.isEmpty()) return;

        Database.deleteRoomManagers(managerName, selected);
        for (RoomManager assignment : selected) {
            ClassroomRentalApp.ROOM_MANAGERS.get(assignment.roomId).remove(assignment);
        }
    }

    private static int roomId(Map<String, String> form) {
        try { return Integer.parseInt(ClassroomRentalApp.required(form, "roomId")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("강의실 선택을 확인해 주세요."); }
    }
}
