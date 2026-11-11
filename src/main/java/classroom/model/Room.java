package classroom.model;

/** 강의실 기본 정보와 요일별 고정 수업 비트맵. */
public final class Room {
    public final int id;
    public final String name;
    public final int capacity;
    public final String[] weekly;

    public Room(int id, String name, int capacity, String[] weekly) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.weekly = weekly;
    }
}
