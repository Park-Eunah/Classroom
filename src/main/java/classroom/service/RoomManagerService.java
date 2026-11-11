package classroom.service;

import classroom.app.ClassroomRentalApp;
import classroom.data.RoomCatalog;
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
        for (String name : selected) managers.add(new RoomManager(ClassroomRentalApp.nextRoomManagerId++, name));
    }

    public static synchronized void remove(Map<String, String> form) {
        int roomId = roomId(form);
        int managerId;
        try { managerId = Integer.parseInt(ClassroomRentalApp.required(form, "managerId")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("삭제할 관리자 선택을 확인해 주세요."); }
        List<RoomManager> managers = ClassroomRentalApp.ROOM_MANAGERS.get(roomId);
        if (managers == null || !managers.removeIf(manager -> manager.id == managerId))
            throw new IllegalArgumentException("해당 강의실에서 관리자를 찾을 수 없습니다.");
    }

    private static int roomId(Map<String, String> form) {
        try { return Integer.parseInt(ClassroomRentalApp.required(form, "roomId")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("강의실 선택을 확인해 주세요."); }
    }
}
