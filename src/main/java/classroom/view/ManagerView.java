package classroom.view;
import classroom.app.ClassroomRentalApp;
import classroom.model.*;
import classroom.data.RoomCatalog;
import java.time.LocalDate;
import java.util.*;

public final class ManagerView {
    private ManagerView() { }
    public static String render(String managerName) {

        StringBuilder roomOptions = new StringBuilder();
        List<Room> assignedRooms = ClassroomRentalApp.ROOMS.stream().filter(r -> ClassroomRentalApp.ROOM_MANAGERS
                .getOrDefault(r.id, List.of()).stream().anyMatch(manager -> manager.name.equals(managerName))).toList();
        for (Room r : assignedRooms) roomOptions.append("<button type='button' class='room-option' data-room='" + r.id + "'>" + ClassroomRentalApp.esc(r.name) + "</button>");
        StringBuilder roomData = new StringBuilder("[");
        for (Room r : assignedRooms) {
            if (roomData.length() > 1) roomData.append(',');
            roomData.append("{id:").append(r.id).append(",name:'").append(ClassroomRentalApp.js(r.name)).append("',weekly:['")
                    .append(r.weekly[0]).append("','").append(r.weekly[1]).append("','").append(r.weekly[2]).append("','")
                    .append(r.weekly[3]).append("','").append(r.weekly[4]).append("'],labels:[");
            for (int d = 0; d < 5; d++) {
                if (d > 0) roomData.append(',');
                roomData.append('[');
                for (int p = 1; p <= 14; p++) {
                    if (p > 1) roomData.append(',');
                    roomData.append('\'').append(ClassroomRentalApp.js(ClassroomRentalApp.COURSE_LABELS.getOrDefault(r.id + "-" + d + "-" + p, "강의명 판독 필요"))).append('\'');
                }
                roomData.append(']');
            }
            roomData.append("],professors:[");
            for (int d = 0; d < 5; d++) {
                if (d > 0) roomData.append(',');
                roomData.append('[');
                for (int p = 1; p <= 14; p++) {
                    if (p > 1) roomData.append(',');
                    roomData.append('\'').append(ClassroomRentalApp.js(ClassroomRentalApp.COURSE_PROFESSORS.getOrDefault(r.id + "-" + d + "-" + p, "담당교수 판독 필요"))).append('\'');
                }
                roomData.append(']');
            }
            roomData.append("]}");
        }
        roomData.append(']');
        StringBuilder eventData = new StringBuilder("[");
        for (Application a : ClassroomRentalApp.APPLICATIONS) {
            if (assignedRooms.stream().noneMatch(room -> room.id == a.roomId)) continue;
            if (a.status.equals("반려")) continue;
            if (eventData.length() > 1) eventData.append(',');
            eventData.append("{id:").append(a.id).append(",roomId:").append(a.roomId).append(",date:'").append(a.date).append("',start:'").append(a.start)
                    .append("',end:'").append(a.end).append("',status:'").append(ClassroomRentalApp.js(a.status)).append("',name:'").append(ClassroomRentalApp.js(a.name)).append("',studentId:'").append(ClassroomRentalApp.js(a.studentId))
                    .append("',purpose:'").append(ClassroomRentalApp.js(a.purpose)).append("',details:'").append(ClassroomRentalApp.js(a.details)).append("',reason:'")
                    .append(ClassroomRentalApp.js(a.reason)).append("'}");
        }
        eventData.append(']');
        String content = "<nav class='tabs'><button type='button' class='tab-button active' data-pane='roomsPane'>강의실 목록</button><button type='button' class='tab-button' data-pane='reviewPane'>대여 신청 검토</button></nav>"
                + "<div class='tab-pane' id='roomsPane'><section><h2>강의실 선택</h2><p class='muted'>강의실을 선택하면 해당 강의실의 주간 시간표를 확인할 수 있습니다.</p><div class='room-list'>"
                + roomOptions + "</div></section><section id='scheduleSection' hidden><div class='calendar-head'><button type='button' class='light' id='managerPrev'>이전 주</button><strong id='managerWeek'></strong><button type='button' class='light' id='managerNext'>다음 주</button></div>"
                + "<div class='legend'><span><i class='swatch class-color'></i>고정 수업</span><span><i class='swatch pending-color'></i>신청 대기</span><span><i class='swatch approved-color'></i>대여 확정</span></div>"
                + "<div class='week-wrap'><table class='week-table'><thead id='weekHead'></thead><tbody id='weekBody'></tbody></table></div><section id='eventDetail' hidden></section></section></div>"
                + "<div class='tab-pane' id='reviewPane' hidden><section><h2>대여 신청 검토</h2><p class='muted'>대기 중인 신청을 승인하거나 반려할 수 있습니다.</p>"
                + ApplicationView.list(true, assignedRooms.stream().map(room -> room.id).collect(java.util.stream.Collectors.toSet())) + "</section></div>";
        String scripts = """
                <script>
                const rooms=%%ClassroomRentalApp.ROOMS%%, events=%%EVENTS%%;let currentRoom=null,weekShift=0;
                const base=new Date('%%TODAY%%T00:00:00');const dayNames=['월','화','수','목','금'];
                const pad=n=>String(n).padStart(2,'0');const iso=d=>d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate());
                function drawWeek(){
                  if(!currentRoom)return;const monday=new Date(base);monday.setDate(base.getDate()-((base.getDay()+6)%7)+weekShift*7);const friday=new Date(monday);friday.setDate(monday.getDate()+4);
                  document.getElementById('managerWeek').textContent=`${monday.getFullYear()}.${pad(monday.getMonth()+1)}.${pad(monday.getDate())} ~ ${pad(friday.getMonth()+1)}.${pad(friday.getDate())} (월~금)`;
                  const head=document.getElementById('weekHead');head.innerHTML='<tr><th class="hour">시간</th>'+dayNames.map((n,i)=>{const d=new Date(monday);d.setDate(monday.getDate()+i);return `<th>${n}<br>${d.getMonth()+1}/${d.getDate()}</th>`}).join('')+'</tr>';
                  const body=document.getElementById('weekBody');body.innerHTML='';
                  for(let m=540;m<1320;m+=30){const tr=document.createElement('tr');const label=pad(Math.floor(m/60))+':'+pad(m%60);tr.innerHTML=`<th class="hour">${label}</th>`;
                    for(let d=0;d<5;d++){const date=new Date(monday);date.setDate(monday.getDate()+d);const dateKey=iso(date);const cellStart=m,cellEnd=m+30;const cell=document.createElement('td');
                      const fixed=d<5&&currentRoom.weekly[d].split('').some((v,i)=>v==='1'&&cellStart<540+i*60+50&&540+i*60<cellEnd);
                      const event=events.find(e=>e.roomId===currentRoom.id&&e.date===dateKey&&cellStart<Number(e.end.slice(0,2))*60+Number(e.end.slice(3))&&Number(e.start.slice(0,2))*60+Number(e.start.slice(3))<cellEnd);
                      if(event){const eventStart=Number(event.start.slice(0,2))*60+Number(event.start.slice(3));const eventEnd=Number(event.end.slice(0,2))*60+Number(event.end.slice(3));cell.className=(event.status==='대기'?'pending-cell':'approved-cell')+(eventStart>=cellStart&&eventStart<cellEnd?' event-start-cell':'')+(eventEnd>cellStart&&eventEnd<=cellEnd?' event-end-cell':'');cell.dataset.eventId=event.id;cell.title=`${event.name} · ${event.purpose} · ${event.status}`;if(eventStart>=cellStart&&eventStart<cellEnd){const label=document.createElement('span');const name=document.createElement('strong');name.className='booking-name';name.textContent=event.name;const purpose=document.createElement('small');purpose.className='booking-purpose';purpose.textContent=event.purpose;label.append(name,purpose);cell.appendChild(label)}cell.onclick=()=>showEvent(event)}
                      else if(fixed){const period=Math.floor((m-540)/60);const subject=currentRoom.labels[d][period]||'강의명 판독 필요';const professor=currentRoom.professors[d][period]||'담당교수 판독 필요';cell.className='class-cell'+(m%60===0?' class-start-cell':'')+(m%60===30?' class-last-slot':'');cell.title=`${subject} · ${professor}`;if(m%60===0)cell.innerHTML=`<span><strong>${subject}</strong><small>${professor}</small></span>`}
                      tr.appendChild(cell);
                    }body.appendChild(tr);
                  }
                }
                function showEvent(event){
                  document.querySelectorAll('.week-table td.event-selected').forEach(c=>c.classList.remove('event-selected'));
                  document.querySelectorAll(`.week-table td[data-event-id="${event.id}"]`).forEach(c=>c.classList.add('event-selected'));
                  const panel=document.getElementById('eventDetail');panel.hidden=false;panel.innerHTML='<h2>대여 신청 상세</h2><div class="detail-card" id="eventInfo"></div><div id="eventActions" class="detail-actions"></div>';
                  const info=document.getElementById('eventInfo');const rows=[['대여자 이름',event.name],['학번',event.studentId],['강의실',currentRoom.name],['이용 날짜',event.date],['이용 시간',event.start+' ~ '+event.end],['이용 목적',event.purpose],['추가 정보',event.details],['상태',event.status]];
                  if(event.reason)rows.push(['반려 사유',event.reason]);
                  rows.forEach(([label,value])=>{const row=document.createElement('div');row.className='detail-row';const title=document.createElement('strong');title.textContent=label;const text=document.createElement('div');text.textContent=value;row.append(title,text);info.appendChild(row)});
                  const actions=document.getElementById('eventActions');
                  if(event.status==='대기'){const form=document.createElement('form');form.method='post';form.action='/decision/'+event.id;const approve=document.createElement('button');approve.name='choice';approve.value='승인';approve.textContent='승인';const label=document.createElement('label');label.textContent='반려 사유';const reason=document.createElement('input');reason.name='reason';reason.placeholder='반려 사유를 입력하세요';const reject=document.createElement('button');reject.className='reject';reject.name='choice';reject.value='반려';reject.textContent='반려';form.append(approve,label,reason,reject);actions.appendChild(form)}
                  panel.scrollIntoView({behavior:'smooth',block:'nearest'});
                }
                document.querySelectorAll('.tab-button').forEach(button=>button.onclick=()=>{document.querySelectorAll('.tab-button').forEach(b=>b.classList.toggle('active',b===button));document.querySelectorAll('.tab-pane').forEach(p=>p.hidden=p.id!==button.dataset.pane)});
                document.querySelectorAll('.room-option').forEach(button=>button.onclick=()=>{currentRoom=rooms.find(r=>r.id===Number(button.dataset.room));document.querySelectorAll('.room-option').forEach(b=>b.classList.toggle('active',b===button));document.getElementById('scheduleSection').hidden=false;document.getElementById('eventDetail').hidden=true;drawWeek()});
                document.getElementById('managerPrev').onclick=()=>{weekShift--;document.getElementById('eventDetail').hidden=true;drawWeek()};document.getElementById('managerNext').onclick=()=>{weekShift++;document.getElementById('eventDetail').hidden=true;drawWeek()};
                </script>
                """.replace("%%ClassroomRentalApp.ROOMS%%",roomData.toString()).replace("%%EVENTS%%",eventData.toString()).replace("%%TODAY%%",LocalDate.now().toString());
        return ClassroomRentalApp.shell("강의실 관리자", "강의실을 선택해 고정 수업과 대여 신청 일정을 확인합니다.", content + scripts);
    
    }
}
