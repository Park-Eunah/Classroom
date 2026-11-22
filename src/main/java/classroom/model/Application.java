package classroom.model;

import java.time.LocalTime;

/** 강의실 대여 신청과 검토 상태. */
public final class Application {
    public final int id;
    public final int roomId;
    public final String name;
    public final String studentId;
    public final String roomName;
    public final String date;
    public final String purpose;
    public final String details;
    public final LocalTime start;
    public final LocalTime end;
    public volatile String status = "대기";
    public String reason = "";

    public boolean blocksTimeSlot() {
        String currentStatus = status;
        return currentStatus.equals("대기") || currentStatus.equals("승인");
    }

    public Application(int id, String name, String studentId, int roomId, String roomName, String date,
                LocalTime start, LocalTime end, String purpose, String details) {
        this.id = id;
        this.name = name;
        this.studentId = studentId;
        this.roomId = roomId;
        this.roomName = roomName;
        this.date = date;
        this.start = start;
        this.end = end;
        this.purpose = purpose;
        this.details = details;
    }
}
