package classroom.view;
import classroom.app.ClassroomRentalApp;
import classroom.model.Application;
import java.util.Set;

import static classroom.app.ClassroomRentalApp.esc;

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
            html.append("<article class='application'><div class='rental-summary'>")
                    .append("<h3 class='rental-heading'>").append(esc(a.date)).append(" · ")
                    .append(a.start).append(" ~ ").append(a.end).append("</h3>")
                    .append("<dl class='rental-details'>")
                    .append("<dt>강의실</dt><dd>").append(esc(a.roomName)).append("</dd>")
                    .append("<dt>신청자</dt><dd>").append(esc(a.name)).append("</dd>")
                    .append("<dt>학번</dt><dd>").append(esc(a.studentId)).append("</dd>")
                    .append("<dt>이용 목적</dt><dd>").append(esc(a.purpose)).append("</dd>")
                    .append("<dt>추가 정보</dt><dd class='rental-notes'>").append(esc(a.details)).append("</dd>")
                    .append("<dt>처리 상태</dt><dd class='status'>").append(esc(a.status)).append("</dd>");
            if (!a.reason.isEmpty()) {
                html.append("<dt>반려 사유</dt><dd class='reason'>").append(esc(a.reason)).append("</dd>");
            }
            html.append("</dl></div>");
            if (a.status.equals("대기")) {
                if (allowDecision) {
                    html.append("<form method='post' action='/decision/" + a.id + "'><button name='choice' value='승인'>승인</button><label>반려 사유</label><input name='reason' placeholder='선택 입력'><button class='reject' name='choice' value='반려'>반려</button></form>");
                } else {
                    html.append("<form method='post' action='/cancel' class='rental-cancel'>")
                            .append("<input type='hidden' name='id' value='").append(a.id).append("'>")
                            .append("<button type='submit' class='reject'>신청 취소</button></form>");
                }
            }
            html.append("</article>");
        }
        return hasApplications ? html.toString() : "<p class='muted'>아직 신청 내역이 없습니다.</p>";
    
    }
}
