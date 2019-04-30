package com.changxiao.calendardemo;

/**
 * 日历 model
 *
 * Created by Chang.Xiao on 2019/4/30.
 *
 * @version 1.0
 */
public class CalendarModel {

  private long id;
  private String accountName;
  private String calendarDisplayName;
  private String ownerAccount;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getCalendarDisplayName() {
    return calendarDisplayName;
  }

  public void setCalendarDisplayName(String calendarDisplayName) {
    this.calendarDisplayName = calendarDisplayName;
  }

  public String getOwnerAccount() {
    return ownerAccount;
  }

  public void setOwnerAccount(String ownerAccount) {
    this.ownerAccount = ownerAccount;
  }
}
