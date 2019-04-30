package com.changxiao.calendardemo;

/**
 * 日程提醒 model
 *
 * Created by Chang.Xiao on 2019/4/30.
 *
 * @version 1.0
 */
public class EventModel {

  private long id;
  private long calendarId;
  private String title;
  private String description;
  private String eventLocation;
  private long dtstart;
  private long dtend;
  private String eventTimezone;
  private boolean hasAlarm;
  private boolean allDay;
  private int availability;
  private int accessLevel;
  private int eventStatus;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getCalendarId() {
    return calendarId;
  }

  public void setCalendarId(long calendarId) {
    this.calendarId = calendarId;
  }

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

  public String getEventLocation() {
    return eventLocation;
  }

  public void setEventLocation(String eventLocation) {
    this.eventLocation = eventLocation;
  }

  public long getDtstart() {
    return dtstart;
  }

  public void setDtstart(long dtstart) {
    this.dtstart = dtstart;
  }

  public long getDtend() {
    return dtend;
  }

  public void setDtend(long dtend) {
    this.dtend = dtend;
  }

  public String getEventTimezone() {
    return eventTimezone;
  }

  public void setEventTimezone(String eventTimezone) {
    this.eventTimezone = eventTimezone;
  }

  public boolean isHasAlarm() {
    return hasAlarm;
  }

  public void setHasAlarm(boolean hasAlarm) {
    this.hasAlarm = hasAlarm;
  }

  public boolean isAllDay() {
    return allDay;
  }

  public void setAllDay(boolean allDay) {
    this.allDay = allDay;
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

  public int getEventStatus() {
    return eventStatus;
  }

  public void setEventStatus(int eventStatus) {
    this.eventStatus = eventStatus;
  }
}
