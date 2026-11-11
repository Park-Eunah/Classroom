package classroom.view;

import classroom.app.ClassroomRentalApp;
import classroom.model.*;
import classroom.data.RoomCatalog;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/** 로그인 관련 화면을 그립니다. */
public final class LoginView {
    private LoginView() { }
    public static String render() {
        String content = "<section><h2>로그인</h2><p class='muted'>아이디와 비밀번호를 입력하면 계정 역할에 맞는 화면으로 이동합니다.</p>"
                + "<form method='post' action='/login'><label for='username'>아이디</label><input id='username' name='username' required autocomplete='username' placeholder='아이디'>"
                + "<label for='password'>비밀번호</label><input id='password' type='password' name='password' required autocomplete='current-password' placeholder='비밀번호'><button>로그인</button></form></section>"
                + demoAccounts();
        return ClassroomRentalApp.shell("로그인", "계정에 따라 학생·강의실 관리자·시스템 관리자 화면으로 이동합니다.", content);
    }
    public static void sendInvalidCredentials(HttpExchange e) throws IOException {
        String content = "<section><h2>로그인</h2><p class='reason'>아이디 또는 비밀번호를 확인해 주세요.</p><form method='post' action='/login'>"
                + "<label>아이디</label><input name='username' required><label>비밀번호</label><input type='password' name='password' required><button>로그인</button></form></section>"
                + demoAccounts();
        ClassroomRentalApp.send(e, ClassroomRentalApp.shell("로그인", "아이디 또는 비밀번호를 확인해 주세요.", content));
    }

    private static String demoAccounts() {
        return "<section><h2>테스트 계정</h2><p class='muted'>역할을 누르면 아이디와 비밀번호가 입력됩니다.</p>"
                + "<div class='demo-accounts'>"
                + "<button type='button' class='light' data-username='student01' data-password='student123'>학생</button>"
                + "<button type='button' class='light' data-username='관리자1' data-password='manager123'>강의실 관리자1</button>"
                + "<button type='button' class='light' data-username='관리자2' data-password='manager123'>강의실 관리자2</button>"
                + "<button type='button' class='light' data-username='관리자3' data-password='manager123'>강의실 관리자3</button>"
                + "<button type='button' class='light' data-username='admin01' data-password='admin123'>시스템 관리자</button>"
                + "</div><script>document.querySelectorAll('.demo-accounts button').forEach(button=>button.addEventListener('click',()=>{"
                + "document.querySelector('[name=username]').value=button.dataset.username;"
                + "document.querySelector('[name=password]').value=button.dataset.password;"
                + "document.querySelector('[name=username]').focus();}));</script></section>";
    }
}
