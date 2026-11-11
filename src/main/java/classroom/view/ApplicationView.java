package classroom.view;
import classroom.app.ClassroomRentalApp;
import classroom.model.*;
import classroom.data.RoomCatalog;
import java.time.LocalDate;
import java.util.*;

public final class ApplicationView {
    private ApplicationView() { }
    public static String list(boolean allowDecision) {
        return list(allowDecision, null);
    }

    public static String list(boolean allowDecision, Set<Integer> roomIds) {

        StringBuilder html = new StringBuilder();
        boolean hasApplications = false;
        for (Application a : ClassroomRentalApp.APPLICATIONS) {
            if (roomIds != null && !roomIds.contains(a.roomId)) continue;
            hasApplications = true;
            html.append("<div class='application'><div><strong>" + ClassroomRentalApp.esc(a.roomName) + " · " + ClassroomRentalApp.esc(a.date) + " · " + a.start + "~" + a.end + "</strong>")
                    .append("<div>" + ClassroomRentalApp.esc(a.name) + " (" + ClassroomRentalApp.esc(a.studentId) + ") — " + ClassroomRentalApp.esc(a.purpose) + "</div><div>" + ClassroomRentalApp.esc(a.details) + "</div><div class='status'>" + ClassroomRentalApp.esc(a.status) + "</div>");
            if (!a.reason.isEmpty()) html.append("<div class='reason'>" + ClassroomRentalApp.esc(a.reason) + "</div>");
            html.append("</div>");
            if (allowDecision && a.status.equals("대기")) html.append("<form method='post' action='/decision/" + a.id + "'><button name='choice' value='승인'>승인</button><label>반려 사유</label><input name='reason' placeholder='선택 입력'><button class='reject' name='choice' value='반려'>반려</button></form>");
            html.append("</div>");
        }
        return hasApplications ? html.toString() : "<p class='muted'>아직 신청 내역이 없습니다.</p>";
    
    }
}
