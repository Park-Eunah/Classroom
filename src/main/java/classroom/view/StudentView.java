package classroom.view;
import classroom.app.ClassroomRentalApp;
import classroom.model.Application;
import classroom.model.Room;
import java.util.Map;

import static classroom.app.ClassroomRentalApp.esc;

public final class StudentView {
    private StudentView() { }
    public static String render(Map<String, String> params) {
        return render(params, Map.of(), "");
    }

    public static String renderFailedApplication(Map<String, String> form, String error) {
        return render(Map.of(), form, error == null ? "입력한 정보를 확인하고 다시 시도해 주세요." : error);
    }

    private static String render(Map<String, String> params, Map<String, String> form, String error) {

        StringBuilder roomsJson = new StringBuilder("[");
        for (Room r : ClassroomRentalApp.ROOMS) {
            if (roomsJson.length() > 1) roomsJson.append(',');
            roomsJson.append("{id:").append(r.id).append(",name:'").append(ClassroomRentalApp.js(r.name)).append("',capacity:'").append(ClassroomRentalApp.js(ClassroomRentalApp.capacity(r))).append("',weekly:['")
                    .append(ClassroomRentalApp.js(r.weekly[0])).append("','").append(ClassroomRentalApp.js(r.weekly[1])).append("','").append(ClassroomRentalApp.js(r.weekly[2])).append("','")
                    .append(ClassroomRentalApp.js(r.weekly[3])).append("','").append(ClassroomRentalApp.js(r.weekly[4])).append("']}");
        }
        roomsJson.append(']');
        StringBuilder bookingsJson = new StringBuilder("[");
        for (Application a : ClassroomRentalApp.APPLICATIONS) {
            if (!a.blocksTimeSlot()) continue;
            if (bookingsJson.length() > 1) bookingsJson.append(',');
            bookingsJson.append("{roomId:").append(a.roomId).append(",date:'").append(a.date).append("',start:'")
                    .append(a.start).append("',end:'").append(a.end).append("'}");
        }
        bookingsJson.append(']');
        String historyNotice = switch (params.getOrDefault("cancelled", "")) {
            case "0" -> "<p class='reason' role='status'>승인 대기 중인 신청만 취소할 수 있습니다. 최신 처리 상태를 확인해 주세요.</p>";
            default -> "1".equals(params.get("submitted"))
                    ? "<p class='application-receipt' role='status'>신청이 접수되었습니다. 담당 관리자 승인 대기 중입니다.</p>"
                    : "";
        };
        boolean showHistory = !historyNotice.isEmpty();
        String cancelReceipt = "1".equals(params.get("cancelled"))
                ? "<p class='application-receipt' role='status'>신청이 취소되었습니다.</p>" : "";
        boolean failed = !error.isEmpty();
        String formError = failed ? "<p class='application-error' role='alert' tabindex='-1' id='applicationError'>"
                + esc(error) + "</p>" : "";
        String purpose = form.get("purposeType");
        String content = cancelReceipt + "<nav class='tabs'>"
                + "<button type='button' class='tab-button" + (showHistory ? "" : " active") + "' data-pane='applyPane'>대여 신청</button>"
                + "<button type='button' class='tab-button" + (showHistory ? " active" : "") + "' data-pane='applicationsPane'>대여 신청 확인</button></nav>"
                + "<div class='tab-pane' id='applyPane'" + (showHistory ? " hidden" : "") + ">" + formError + """
                <section><h2>1. 날짜와 시간 선택</h2>
                  <div class="calendar-head"><button type="button" class="light" id="prevWeek">이전 주</button><strong id="weekLabel"></strong><button type="button" class="light" id="nextWeek">다음 주</button></div>
                  <div id="calendar" class="calendar"></div>
                  <label for="floor">층 선택</label><select id="floor"><option value="all">전체 층</option><option value="2">2층</option><option value="3">3층</option><option value="4">4층</option><option value="5">5층</option></select>
                  <label>이용 시간 선택 (09:00~22:00)</label><p class="muted">바의 30분 칸을 누르세요. 바로 옆 칸을 이어서 누르면 시간이 늘어나며, 최대 2시간까지 선택할 수 있습니다.</p>
                  <div id="timeBar" class="timebar"></div><div class="time-end">선택 시간: <strong id="timeLabel">날짜와 시간을 선택해 주세요.</strong></div>
                </section>
                <section><h2>2. 신청 가능한 강의실</h2><p class="muted">선택한 날짜와 시간에 이용 가능한 강의실만 표시합니다.</p><div id="rooms" class="cards"></div></section>
                <section id="applySection"%s><h2>3. 신청 정보 입력</h2><p id="selectedRoom" class="selected-room">신청할 강의실을 선택해 주세요.</p>
                  <form method="post" action="/apply" id="applyForm">
                    <input type="hidden" name="roomId" id="roomId" value="%s"><input type="hidden" name="date" id="date" value="%s"><input type="hidden" name="start" id="start" value="%s"><input type="hidden" name="duration" id="durationValue" value="%s">
                    <label for="name">신청자 이름</label><input id="name" name="name" required maxlength="40" placeholder="이름" value="%s">
                    <label for="studentId">학번</label><input id="studentId" name="studentId" required inputmode="numeric" pattern="[0-9]{5,20}" maxlength="20" placeholder="숫자만 입력" value="%s">
                    <label for="purposeType">이용 목적</label><select id="purposeType" name="purposeType"><option>동아리 모임</option><option%s>팀 프로젝트 회의</option><option%s>기타</option></select>
                    <div id="otherWrap"%s><label for="otherPurpose">기타 목적</label><input id="otherPurpose" name="otherPurpose" maxlength="80" placeholder="이용 목적을 입력하세요" value="%s"%s></div>
                    <label for="details">추가 정보</label><textarea id="details" name="details" required maxlength="300" placeholder="동아리명과 활동 내용 또는 프로젝트명을 포함해 구체적으로 입력하세요.">
                %s</textarea>
                    <button id="submitApplication" disabled>최종 신청하기</button>
                  </form>
                </section></div>
                """.formatted(failed ? "" : " hidden", field(form, "roomId"), field(form, "date"),
                        field(form, "start"), field(form, "duration"), field(form, "name"), field(form, "studentId"),
                        "팀 프로젝트 회의".equals(purpose) ? " selected" : "",
                        "기타".equals(purpose) ? " selected" : "",
                        "기타".equals(purpose) ? "" : " hidden", field(form, "otherPurpose"),
                        "기타".equals(purpose) ? " required" : "", field(form, "details"))
                + "<div class='tab-pane' id='applicationsPane'" + (showHistory ? "" : " hidden") + ">"
                + historyNotice + "<section><h2>대여 신청 확인</h2><p class='muted'>현재 전체 대여 신청 목록을 표시합니다.</p>"
                + ApplicationView.list(false) + "</section></div>";
        String scripts = """
                <script>
                document.querySelectorAll('.rental-cancel').forEach(form=>form.addEventListener('submit',event=>{
                  if(!window.confirm('이 대여 신청을 취소할까요?'))event.preventDefault();
                }));
                document.querySelectorAll('.tab-button').forEach(button=>button.onclick=()=>{document.querySelectorAll('.tab-button').forEach(b=>b.classList.toggle('active',b===button));document.querySelectorAll('.tab-pane').forEach(p=>p.hidden=p.id!==button.dataset.pane)});
                const rooms = %%ClassroomRentalApp.ROOMS%%, bookings = %%BOOKINGS%%;
                const calendar = document.getElementById('calendar'), timeBar = document.getElementById('timeBar');
                const floor = document.getElementById('floor');
                const form = document.getElementById('applyForm');
                const applySection = document.getElementById('applySection');
                const submitButton = document.getElementById('submitApplication');
                const applicationError = document.getElementById('applicationError');
                const today = new Date(); today.setHours(0,0,0,0);
                let selectedDate = null, weekOffset = 0, selectedStart = null, selectedEnd = null, selectedRoom = null;
                const pad = n => String(n).padStart(2,'0');
                const iso = d => d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate());
                const weekdays = ['일','월','화','수','목','금','토'];
                function drawCalendar(){
                  const start = new Date(today); start.setDate(today.getDate()-((today.getDay()+6)%7)+weekOffset*7);
                  const friday=new Date(start);friday.setDate(start.getDate()+4);
                  document.getElementById('weekLabel').textContent = `${start.getMonth()+1}/${start.getDate()} ~ ${friday.getMonth()+1}/${friday.getDate()} (월~금)`;
                  calendar.innerHTML='';
                  for(let i=0;i<5;i++){const d=new Date(start);d.setDate(start.getDate()+i);const past=d<today;const b=document.createElement('button');b.type='button';b.className='day'+(past?' past':'')+(selectedDate&&iso(d)===iso(selectedDate)?' active':'');b.disabled=past;b.innerHTML=`<small>${weekdays[d.getDay()]}</small><strong>${d.getMonth()+1}/${d.getDate()}</strong>`;b.onclick=()=>{selectedDate=d;selectedStart=null;selectedEnd=null;drawCalendar();drawBar();renderRooms()};calendar.appendChild(b)}
                  document.getElementById('prevWeek').disabled=weekOffset===0;
                }
                function drawBar(){
                  timeBar.innerHTML='';
                  const nowMinutes=new Date().getHours()*60+new Date().getMinutes();
                  for(let m=540;m<1320;m+=30){const b=document.createElement('button');b.type='button';const past=selectedDate&&iso(selectedDate)===iso(today)&&m<nowMinutes;b.disabled=past;b.className='slot'+(selectedStart!==null&&m>=selectedStart&&m<selectedEnd?' active':'')+(past?' past':'');b.title=`${pad(Math.floor(m/60))}:${pad(m%60)}~${pad(Math.floor((m+30)/60))}:${pad((m+30)%60)}`;b.innerHTML=(m%60===0?`<span>${pad(m/60)}:00</span>`:'');b.onclick=()=>selectTime(m);timeBar.appendChild(b)}
                  document.getElementById('timeLabel').textContent=selectedStart===null?'시간 바에서 이용 시간을 선택해 주세요.':`${pad(Math.floor(selectedStart/60))}:${pad(selectedStart%60)} ~ ${pad(Math.floor(selectedEnd/60))}:${pad(selectedEnd%60)}`;
                }
                function selectTime(m){
                  if(selectedStart===null){selectedStart=m;selectedEnd=m+30}
                  else if(m===selectedEnd&&selectedEnd-selectedStart<120){selectedEnd+=30}
                  else if(m===selectedStart&&selectedEnd-selectedStart>30){selectedStart+=30}
                  else if(m+30===selectedEnd&&selectedEnd-selectedStart>30){selectedEnd-=30}
                  else if(m+30===selectedStart&&selectedEnd-selectedStart<120){selectedStart=m}
                  else if(m===selectedStart){selectedEnd=m+30}
                  else {selectedStart=m;selectedEnd=m+30}
                  drawBar();renderRooms();
                }
                function renderRooms(){
                  selectedRoom=null;
                  form.elements.roomId.value='';
                  submitButton.disabled=true;
                  applySection.hidden=!applicationError;
                  document.getElementById('selectedRoom').textContent='신청할 강의실을 선택해 주세요.';
                  const list=document.getElementById('rooms');list.innerHTML='';
                  if(!selectedDate||selectedStart===null){list.innerHTML='<p class="muted">날짜와 이용 시간을 선택하면 신청 가능한 강의실을 보여드립니다.</p>';return []}
                  const start=selectedStart, end=selectedEnd, day=iso(selectedDate);
                  const weekday=(selectedDate.getDay()+6)%7;
                  const available=rooms.filter(r=>floor.value==='all'||String(r.name[0])===floor.value).filter(r=>{
                    const classConflict=weekday<5&&r.weekly[weekday].split('').some((occupied,i)=>occupied==='1'&&start<540+i*60+50&&540+i*60<end);
                    const bookingConflict=bookings.some(b=>b.roomId===r.id&&b.date===day&&start<Number(b.end.slice(0,2))*60+Number(b.end.slice(3))&&Number(b.start.slice(0,2))*60+Number(b.start.slice(3))<end);
                    return !classConflict&&!bookingConflict;
                  });
                  available.forEach(r=>{const b=document.createElement('button');b.type='button';b.className='room room-button';b.innerHTML=`<strong>${r.name}</strong><div class="muted">${r.capacity}</div>`;b.onclick=()=>chooseRoom(r);list.appendChild(b)});
                  if(!list.children.length)list.innerHTML='<p class="muted">조건에 맞는 신청 가능 강의실이 없습니다.</p>';
                  return available;
                }
                function chooseRoom(r, scroll=true){
                  selectedRoom=r;
                  document.getElementById('selectedRoom').textContent=`${r.name} · ${iso(selectedDate)} · ${document.getElementById('timeLabel').textContent}`;
                  form.elements.roomId.value=r.id;
                  form.elements.date.value=iso(selectedDate);
                  form.elements.start.value=pad(Math.floor(selectedStart/60))+':'+pad(selectedStart%60);
                  form.elements.duration.value=selectedEnd-selectedStart;
                  applySection.hidden=false;
                  submitButton.disabled=false;
                  if(scroll)applySection.scrollIntoView({behavior:'smooth'});
                }
                function restoreSchedule(){
                  const dateValue=form.elements.date.value;
                  const date=new Date(dateValue+'T00:00:00');
                  if(iso(date)!==dateValue||date<today||date.getDay()===0||date.getDay()===6)return;
                  selectedDate=date;
                  const currentMonday=new Date(today);
                  currentMonday.setDate(today.getDate()-((today.getDay()+6)%7));
                  const selectedMonday=new Date(date);
                  selectedMonday.setDate(date.getDate()-(date.getDay()-1));
                  weekOffset=Math.round((selectedMonday-currentMonday)/(7*86400000));
                  const time=form.elements.start.value;
                  if(!/^[0-9]{2}:[0-9]{2}$/.test(time))return;
                  const [hour,minute]=time.split(':').map(Number);
                  const start=hour*60+minute, duration=Number(form.elements.duration.value);
                  const now=new Date(), nowMinutes=now.getHours()*60+now.getMinutes();
                  if(![0,30].includes(minute)||start<540||start+duration>1320||![30,60,90,120].includes(duration))return;
                  if(iso(date)===iso(today)&&start<nowMinutes)return;
                  selectedStart=start;
                  selectedEnd=start+duration;
                }
                document.getElementById('prevWeek').onclick=()=>{if(weekOffset===0)return;weekOffset--;selectedDate=null;selectedStart=null;selectedEnd=null;drawCalendar();drawBar();renderRooms()};document.getElementById('nextWeek').onclick=()=>{weekOffset++;selectedDate=null;selectedStart=null;selectedEnd=null;drawCalendar();drawBar();renderRooms()};
                floor.onchange=renderRooms;
                document.getElementById('purposeType').onchange=e=>{const other=e.target.value==='기타';document.getElementById('otherWrap').hidden=!other;document.getElementById('otherPurpose').required=other};
                form.onsubmit=e=>{if(!selectedRoom)e.preventDefault()};
                const previousRoomId=form.elements.roomId.value;
                if(applicationError)restoreSchedule();
                drawCalendar();drawBar();
                const previousRoom=renderRooms().find(room=>String(room.id)===previousRoomId);
                if(previousRoom)chooseRoom(previousRoom,false);
                if(applicationError)applicationError.focus();
                </script>
                """.replace("%%ClassroomRentalApp.ROOMS%%", roomsJson.toString()).replace("%%BOOKINGS%%", bookingsJson.toString());
        String styled = content + scripts;
        return ClassroomRentalApp.shell("학생 화면", "날짜와 시간을 고른 뒤 이용 가능한 강의실을 선택해 신청하세요.", styled);
    
    }

    private static String field(Map<String, String> form, String name) {
        return esc(form.getOrDefault(name, ""));
    }
}
