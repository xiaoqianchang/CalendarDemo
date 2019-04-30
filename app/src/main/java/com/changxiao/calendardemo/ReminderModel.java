package com.changxiao.calendardemo;

/**
 * 日程时间提醒 model
 *
 * Created by Chang.Xiao on 2019/4/30.
 *
 * @version 1.0
 */
public class ReminderModel {

  private long id;
  private long eventId;
  private int  minutes;
  private int method;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getEventId() {
    return eventId;
  }

  public void setEventId(long eventId) {
    this.eventId = eventId;
  }

  public int getMinutes() {
    return minutes;
  }

  public void setMinutes(int minutes) {
    this.minutes = minutes;
  }

  public int getMethod() {
    return method;
  }

  public void setMethod(int method) {
    this.method = method;
  }
}
