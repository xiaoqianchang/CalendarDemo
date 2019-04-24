package com.changxiao.calendardemo;

/**
 * 日程提醒 model
 *
 * Created by Chang.Xiao on 2019/4/24.
 *
 * @version 1.0
 */
public class CalendarRemindModel {

  private static final int TYPE_FALSE = 0; // false
  private static final int TYPE_TRUE = 1; // true

  private String title; // 日程事件标题
  private String description; // 日程内容（备注、附注）
  private long startDate; // 日程事件的启动时间，使用从纪元开始的UTC毫秒计时
  private long endDate; // 日程事件的结束时间，使用从纪元开始的UTC毫秒计时

  private String location; // 日程事件发生的地点
  private String timeZone; // 时区
  private int hasAlarm; // 是否事件触发报警:0=false, 1=true（是否有闹钟提醒）
  private int allDay; // 是否全天事件：0=false, 1=true
  private int eventStatus; // 事件状态:暂定(0)，确认(1)或取消(2)
  private int availability; // 我的状态:0=忙碌，1=有空，2=我的状态可能改变但应该被认为是忙时间冲突
  private int accessLevel; // 访问权限：默认=0，机密=1，私有=2，公共（任何人都可以访问）=3

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getStartDate() {
    return startDate;
  }

  public void setStartDate(long startDate) {
    this.startDate = startDate;
  }

  public long getEndDate() {
    return endDate;
  }

  public void setEndDate(long endDate) {
    this.endDate = endDate;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  public int getHasAlarm() {
    return hasAlarm;
  }

  public void setHasAlarm(int hasAlarm) {
    this.hasAlarm = hasAlarm;
  }

  public int getAllDay() {
    return allDay;
  }

  public void setAllDay(int allDay) {
    this.allDay = allDay;
  }

  public int getEventStatus() {
    return eventStatus;
  }

  public void setEventStatus(int eventStatus) {
    this.eventStatus = eventStatus;
  }

  public int getAvailability() {
    return availability;
  }

  public void setAvailability(int availability) {
    this.availability = availability;
  }

  public int getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(int accessLevel) {
    this.accessLevel = accessLevel;
  }

  /**
   * 是否为全天
   * @return
   */
  public boolean isAllDay() {
    return allDay == TYPE_TRUE;
  }
}
