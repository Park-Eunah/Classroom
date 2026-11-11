package classroom.view;
import classroom.app.ClassroomRentalApp;
import classroom.model.*;
import classroom.data.RoomCatalog;
import java.time.LocalDate;
import java.util.*;

public final class AdminView {
    private AdminView() { }
    public static String render(String query) {

        Map<String, String> params = query == null ? Map.of() : ClassroomRentalApp.parseForm(query);
        int selectedRoomId = 0;
        if (query != null) {
            try { selectedRoomId = Integer.parseInt(params.getOrDefault("roomId", "0")); }
            catch (NumberFormatException ignored) { selectedRoomId = 0; }
        }
        String selectedManager = params.getOrDefault("manager", ClassroomRentalApp.ADMIN_OPTIONS.get(0));
        if (!ClassroomRentalApp.ADMIN_OPTIONS.contains(selectedManager)) selectedManager = ClassroomRentalApp.ADMIN_OPTIONS.get(0);
        boolean showManagerPane = "managers".equals(params.get("view"));
        final String managerForForms = selectedManager;
        final int selectedId = selectedRoomId;
        StringBuilder html = new StringBuilder("<nav class='tabs'><button type='button' class='tab-button " + (!showManagerPane ? "active" : "") + "' data-pane='roomsPane'>강의실별 관리자 확인</button><button type='button' class='tab-button " + (showManagerPane ? "active" : "") + "' data-pane='managersPane'>관리자별 강의실 확인</button></nav><div class='tab-pane' id='roomsPane' " + (showManagerPane ? "hidden" : "") + "><section><h2>강의실 목록</h2><p class='muted'>강의실을 선택해 담당 관리자를 추가하거나 삭제하세요.</p><div class='cards admin-rooms'>");
        for (Room r : ClassroomRentalApp.ROOMS) {
            long managerCount = ClassroomRentalApp.ROOM_MANAGERS.getOrDefault(r.id, List.of()).size();
            html.append("<a class='room admin-room" + (r.id == selectedId ? " selected" : "") + "' href='/admin?roomId=" + r.id + "'><strong>" + ClassroomRentalApp.esc(r.name)
                    + "</strong><div class='muted'>" + ClassroomRentalApp.capacity(r) + " · 담당 관리자 " + managerCount + "명</div></a>");
        }
        html.append("</div></section>");
        Room selected = ClassroomRentalApp.ROOMS.stream().filter(r -> r.id == selectedId).findFirst().orElse(null);
        if (selected != null) {
            html.append("<section><h2>" + ClassroomRentalApp.esc(selected.name) + " · 담당 관리자</h2>");
            List<RoomManager> managers = ClassroomRentalApp.ROOM_MANAGERS.getOrDefault(selected.id, List.of());
            if (managers.isEmpty()) html.append("<p class='muted'>아직 지정된 관리자가 없습니다.</p>");
            for (RoomManager manager : managers) {
                html.append("<div class='application manager-row'><div><strong>" + ClassroomRentalApp.esc(manager.name) + "</strong></div>")
                        .append("<form method='post' action='/admin/manager/remove'><input type='hidden' name='roomId' value='" + selected.id + "'><input type='hidden' name='managerId' value='" + manager.id + "'><button class='reject'>삭제</button></form></div>");
            }
            List<String> assignedNames = managers.stream().map(manager -> manager.name).toList();
            List<String> availableNames = ClassroomRentalApp.ADMIN_OPTIONS.stream().filter(name -> !assignedNames.contains(name)).toList();
            html.append("<h3>관리자 추가</h3>");
            if (availableNames.isEmpty()) html.append("<p class='muted'>관리자1~관리자3이 모두 이 강의실에 지정되어 있습니다.</p>");
            else {
                html.append("<form method='post' action='/admin/manager/add' class='manager-add' id='managerAssignForm'><input type='hidden' name='roomId' value='" + selected.id + "'>")
                        .append("<div id='managerSelectRows'><div class='manager-select-row'><label for='managerName1'>관리자 선택</label><select id='managerName1' name='managerName1' required><option value=''>선택하세요</option>");
                for (String name : availableNames) html.append("<option value='" + ClassroomRentalApp.esc(name) + "'>" + ClassroomRentalApp.esc(name) + "</option>");
                html.append("</select></div></div><input type='hidden' name='selectionCount' id='selectionCount' value='1'>")
                        .append("<button type='button' class='light add-select' id='addManagerSelect'>선택 칸 추가</button><button>선택한 관리자 추가</button></form>")
                        .append("<script>let managerSelectCount=1;const managerOptions=" + availableNames.stream().map(name -> "'" + ClassroomRentalApp.js(name) + "'").collect(java.util.stream.Collectors.joining(",", "[", "]")) + ";")
                        .append("const rows=document.getElementById('managerSelectRows'),addSelect=document.getElementById('addManagerSelect');")
                        .append("function refreshManagerOptions(){const selects=[...rows.querySelectorAll('select')];const chosen=selects.map(s=>s.value).filter(Boolean);selects.forEach(current=>[...current.options].forEach(option=>{if(option.value)option.disabled=chosen.includes(option.value)&&current.value!==option.value}));addSelect.disabled=managerSelectCount>=managerOptions.length||managerSelectCount>=" + availableNames.size() + ";document.getElementById('selectionCount').value=managerSelectCount}")
                        .append("addSelect.onclick=()=>{if(managerSelectCount>=managerOptions.length||managerSelectCount>=" + availableNames.size() + ")return;managerSelectCount++;const wrapper=document.createElement('div');wrapper.className='manager-select-row';const label=document.createElement('label');label.htmlFor='managerName'+managerSelectCount;label.textContent='관리자 선택';const select=document.createElement('select');select.id='managerName'+managerSelectCount;select.name=select.id;select.required=true;const placeholder=document.createElement('option');placeholder.value='';placeholder.textContent='선택하세요';select.appendChild(placeholder);managerOptions.forEach(name=>{const option=document.createElement('option');option.value=name;option.textContent=name;select.appendChild(option)});select.onchange=refreshManagerOptions;wrapper.append(label,select);rows.appendChild(wrapper);refreshManagerOptions()};")
                        .append("rows.querySelectorAll('select').forEach(s=>s.onchange=refreshManagerOptions);refreshManagerOptions();</script>");
            }
            html.append("</section>");
        }
        html.append("</div><div class='tab-pane' id='managersPane' " + (!showManagerPane ? "hidden" : "") + "><section><h2>관리자별 담당 강의실</h2><p class='muted'>관리자를 선택해 담당 강의실을 확인하고 배정을 수정하세요.</p><div class='manager-choices'>");
        for (String managerName : ClassroomRentalApp.ADMIN_OPTIONS) {
            long count = ClassroomRentalApp.ROOMS.stream().filter(room -> ClassroomRentalApp.ROOM_MANAGERS.getOrDefault(room.id, List.of()).stream().anyMatch(manager -> manager.name.equals(managerName))).count();
            html.append("<a class='manager-choice" + (managerName.equals(selectedManager) ? " active" : "") + "' href='/admin?view=managers&amp;manager=" + ClassroomRentalApp.js(managerName) + "'>" + ClassroomRentalApp.esc(managerName) + "<small>담당 " + count + "개</small></a>");
        }
        html.append("</div><h3>" + ClassroomRentalApp.esc(managerForForms) + " 담당 강의실</h3><div class='manager-room-list'>");
        List<Room> assignedToManager = ClassroomRentalApp.ROOMS.stream().filter(room -> ClassroomRentalApp.ROOM_MANAGERS.getOrDefault(room.id, List.of()).stream().anyMatch(manager -> manager.name.equals(managerForForms))).toList();
        if (assignedToManager.isEmpty()) html.append("<p class='muted'>현재 담당 강의실이 없습니다.</p>");
        for (Room room : assignedToManager) {
            RoomManager assignment = ClassroomRentalApp.ROOM_MANAGERS.get(room.id).stream().filter(manager -> manager.name.equals(managerForForms)).findFirst().orElseThrow();
            html.append("<div class='application manager-row'><div><strong>" + ClassroomRentalApp.esc(room.name) + "</strong><div class='muted'>" + ClassroomRentalApp.capacity(room) + "</div></div><form method='post' action='/admin/manager/remove'><input type='hidden' name='roomId' value='" + room.id + "'><input type='hidden' name='managerId' value='" + assignment.id + "'><input type='hidden' name='returnView' value='managers'><input type='hidden' name='managerName' value='" + ClassroomRentalApp.esc(managerForForms) + "'><button class='reject'>배정 해제</button></form></div>");
        }
        List<Room> availableRooms = ClassroomRentalApp.ROOMS.stream().filter(room -> ClassroomRentalApp.ROOM_MANAGERS.getOrDefault(room.id, List.of()).stream().noneMatch(manager -> manager.name.equals(managerForForms))).toList();
        html.append("</div><form method='post' action='/admin/manager/add' class='manager-add manager-room-add'><h3>강의실 추가 배정</h3><input type='hidden' name='selectionCount' value='1'><input type='hidden' name='managerName1' value='" + ClassroomRentalApp.esc(managerForForms) + "'><input type='hidden' name='returnView' value='managers'><input type='hidden' name='managerName' value='" + ClassroomRentalApp.esc(managerForForms) + "'>");
        if (availableRooms.isEmpty()) html.append("<p class='muted'>배정 가능한 강의실이 없습니다.</p>");
        else {
            html.append("<label for='managerRoomId'>강의실 선택</label><select id='managerRoomId' name='roomId' required><option value=''>선택하세요</option>");
            for (Room room : availableRooms) html.append("<option value='" + room.id + "'>" + ClassroomRentalApp.esc(room.name) + "</option>");
            html.append("</select><button>강의실 배정</button>");
        }
        html.append("</form></section></div><script>document.querySelectorAll('.tab-button').forEach(button=>button.onclick=()=>{if(button.dataset.pane==='managersPane'){location.href='/admin?view=managers&manager=" + ClassroomRentalApp.js(selectedManager) + "'}else{location.href='/admin'}});</script>");
        html.append("<section><h2>운영 현황</h2><p>전체 대여 신청: " + ClassroomRentalApp.APPLICATIONS.size() + "건</p>");
        long pending = ClassroomRentalApp.APPLICATIONS.stream().filter(a -> a.status.equals("대기")).count();
        html.append("<p>처리 대기: " + pending + "건</p></section>");
        return ClassroomRentalApp.shell("시스템 관리자", "강의실별 담당 관리자를 지정하고 관리합니다.", html.toString());
    
    }
}
