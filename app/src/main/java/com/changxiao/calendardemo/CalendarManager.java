package com.changxiao.calendardemo;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import java.util.Calendar;
import java.util.TimeZone;

/*
总结
在项目开发中，我们有预约提醒、定时提醒需求时，可以使用系统日历来辅助提醒；
通过向系统日历中写入事件、设置提醒方式（闹钟），实现到时间自动提醒的功能；

好处：由于提醒功能是交付给系统日历来做，不会出现应用被杀情况，会准时提醒；
坏处：系统日历在提醒时，不能直接再跳转回我们自己的app，只有在提醒文案中加入url，通过调用浏览器来中转；

一般来说实现向系统日历中读写事件：
1.需要有读写日历权限
2.如果没有日历账户需要先创建账户
3.实现日历事件增删改查、提醒功能

测试总结：
1. 如果只有开始时间无结束时间，会报错，java.lang.IllegalArgumentException: DTEND and DURATION cannot both be null for an event.
 */

/**
 * 系统日历、日程操作 manager
 *
 * 如向系统日历中添加日程事件
 *
 * 可以对系统日历添加、删除、更新日程事件：（实际是对日历app进行增删该查功能）
 * 1. 需要读写日历权限，android.permission.READ_CALENDAR、android.permission.WRITE_CALENDAR；
 * 2. 单个日程新建：日程标题、开始时间、结束时间、时区、重复方式、提醒方式、第二次提醒、备注，是否有闹钟提醒都可自定义
 *
 * 至此，我们就能向系统日历中 查找、添加账户，添加、删除、更新日程事件，添加提醒方式，来实现提醒用户的需求。
 * 再次说明一点，当系统日历弹出提醒的时候并不能直接跳转回自己的app，需要在设置description字段的文本中添加一个html，用户点击html时调用浏览器，由页面中转回自己得app。
 *
 * https://blog.csdn.net/zhaoshuyu111/article/details/53195142
 * https://blog.csdn.net/g984160547/article/details/55049666
 *
 * Created by Chang.Xiao on 2019/4/9.
 *
 * @version 1.0
 */
public class CalendarManager {

  // 系统calendar content provider相关的uri，以下为Android2.2版本以后的uri
  private static String CALANDER_URL = "content://com.android.calendar/calendars";
  private static String CALANDER_EVENT_URL = "content://com.android.calendar/events";
  private static String CALANDER_REMIDER_URL = "content://com.android.calendar/reminders";

  /**
   * 检查是否有存在的账户。有则返回账户id，否则返回-1
   * @param context
   * @return
   */
  private static int checkCalendarAccount(Context context) {
    Cursor userCursor = context.getContentResolver().query(Uri.parse(CALANDER_URL), null, null, null, null);
    try {
      if (userCursor == null) // 查询返回空值
        return -1;
      int count = userCursor.getCount();
      if (count > 0) { // 存在现有账户，取第一个账户的id返回
        userCursor.moveToFirst();
        return userCursor.getInt(userCursor.getColumnIndex(CalendarContract.Calendars._ID));
      } else {
        return -1;
      }
    } finally {
      if (userCursor != null) {
        userCursor.close();
      }
    }
  }

  private static String CALENDARS_NAME = "test";
  private static String CALENDARS_ACCOUNT_NAME = "test@gmail.com";
  private static String CALENDARS_ACCOUNT_TYPE = "com.android.exchange";
  private static String CALENDARS_DISPLAY_NAME = "测试账户";

  /**
   * 添加账户。账户创建成功则返回账户id，否则返回-1
   * @param context
   * @return
   */
  private static long addCalendarAccount(Context context) {
    TimeZone timeZone = TimeZone.getDefault();
    ContentValues value = new ContentValues();
    value.put(CalendarContract.Calendars.NAME, CALENDARS_NAME);
    value.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME);
    value.put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDARS_ACCOUNT_TYPE);
    value.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDARS_DISPLAY_NAME);
    value.put(CalendarContract.Calendars.VISIBLE, 1);
    value.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.BLUE);
    value.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
    value.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
    value.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone.getID());
    value.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDARS_ACCOUNT_NAME);
    value.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0);

    Uri calendarUri = Uri.parse(CALANDER_URL);
    calendarUri = calendarUri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDARS_ACCOUNT_TYPE)
        .build();

    Uri result = context.getContentResolver().insert(calendarUri, value);
    long id = result == null ? -1 : ContentUris.parseId(result);
    return id;
  }

  /**
   * 获取账户。如果账户不存在则先创建账户，账户存在获取账户id；获取账户成功返回账户id，否则返回-1
   * 检查是否已经添加了日历账户，如果没有添加先添加一个日历账户再查询
   * @param context
   * @return
   */
  private static int checkAndAddCalendarAccount(Context context) {
    int oldId = checkCalendarAccount(context);
    if (oldId >= 0) {
      return oldId;
    } else {
      long addId = addCalendarAccount(context);
      if (addId >= 0) {
        return checkCalendarAccount(context);
      } else {
        return -1;
      }
    }
  }

  /**
   * 添加日历事件、日程
   * @param context
   */
  public static boolean addCalendarEvent(Context context, CalendarRemindModel calendarRemindModel) {
    if (null == calendarRemindModel) {
      return false;
    }
    // 获取日历账户的id
    int calId = checkAndAddCalendarAccount(context);
    if (calId < 0) {
      // 获取账户id失败直接返回，添加日历事件失败
      return false;
    }

    ContentValues event = new ContentValues();
    // 插入账户的id
    event.put(CalendarContract.Events.CALENDAR_ID, calId); // 日历事件属于的Calendars#_ID，必须有
    event.put(CalendarContract.Events.TITLE, calendarRemindModel.getTitle()); // 事件的标题
    event.put(CalendarContract.Events.DESCRIPTION, calendarRemindModel.getDescription()); // 事件的备注

    Calendar mCalendar = Calendar.getInstance();
    mCalendar.setTimeInMillis(calendarRemindModel.getStartDate()); // 设置开始时间
//    mCalendar.set(2019, 4, 22, 18, 0);
//    mCalendar.getTimeInMillis();
    long start = mCalendar.getTime().getTime();
    mCalendar.setTimeInMillis(calendarRemindModel.getEndDate()); // 设置终止时间
    long end = mCalendar.getTime().getTime();

    event.put(CalendarContract.Events.DTSTART, start); // 事件的启动时间，使用从纪元开始的UTC毫秒计时，必须有
    event.put(CalendarContract.Events.DTEND, end); // 事件的结束时间，使用从纪元开始的UTC毫秒计时，对于非重复发生的事件，必须有
    event.put(CalendarContract.Events.STATUS, 1); // 事件状态:暂定(0)，确认(1)或取消(2)
    event.put(CalendarContract.Events.HAS_ALARM, calendarRemindModel.getHasAlarm()); // 设置有闹钟提醒
    String timeZone = calendarRemindModel.getTimeZone();
    event.put(CalendarContract.Events.EVENT_TIMEZONE, TextUtils.isEmpty(timeZone) ? "Asia/Shanghai" : timeZone); // 事件所针对的时区，必须有
    // 添加事件
    Uri newEvent = context.getContentResolver().insert(Uri.parse(CALANDER_EVENT_URL), event);
    if (newEvent == null) {
      // 添加日历事件失败直接返回
      return false;
    }
    // 事件提醒的设定，Reminders表
    ContentValues values = new ContentValues();
    values.put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(newEvent));
    // 提醒：提前10分钟有提醒（若注释掉或者设为0表示日程发生时）
//    values.put(CalendarContract.Reminders.MINUTES, 10); // 应该在几分钟之前触发事件。
    // 在服务上设置的报警的方法，下列设置之一：
    // 1.  METHOD_ALERT
    // 2.  METHOD_DEFAULT
    // 3.  METHOD_EMAIL
    // 4.  METHOD_SMS
    values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
    // 这里会有SQLiteException（比如Account不对时会抛出此异常）
    Uri uri = context.getContentResolver().insert(Uri.parse(CALANDER_REMIDER_URL), values);
    if(uri == null) {
      // 添加闹钟提醒失败直接返回
      return false;
    }
    return true;
  }

  public static boolean hasCalendarEvent(Context context, String title) {
    Cursor eventCursor = context.getContentResolver().query(Uri.parse(CALANDER_EVENT_URL), null, null, null, null);
    try {
      if (eventCursor == null) // 查询返回空值
        return false;
      if (eventCursor.getCount() > 0) {
        // 遍历所有事件，找到title跟需要查询的title一样的项
        for (eventCursor.moveToFirst(); !eventCursor.isAfterLast(); eventCursor.moveToNext()) {
          String eventTitle = eventCursor.getString(eventCursor.getColumnIndex("title"));
          if (!TextUtils.isEmpty(title) && title.equals(eventTitle)) {
            return true;
          }
        }
      }
      return false;
    } finally {
      if (eventCursor != null) {
        eventCursor.close();
      }
    }
  }

  /**
   * 删除日历事件、日程
   * 根据设置的title来查找并删除
   * @param context
   * @param title
   */
  public static boolean deleteCalendarEvent(Context context, String title) {
    Cursor eventCursor = context.getContentResolver().query(Uri.parse(CALANDER_EVENT_URL), null, null, null, null);
    try {
      if (eventCursor == null) // 查询返回空值
        return false;
      if (eventCursor.getCount() > 0) {
        // 遍历所有事件，找到title跟需要查询的title一样的项
        for (eventCursor.moveToFirst(); !eventCursor.isAfterLast(); eventCursor.moveToNext()) {
          String eventTitle = eventCursor.getString(eventCursor.getColumnIndex("title"));
          if (!TextUtils.isEmpty(title) && title.equals(eventTitle)) {
            int id = eventCursor.getInt(eventCursor.getColumnIndex(CalendarContract.Calendars._ID)); // 取得id
            Uri deleteUri = ContentUris.withAppendedId(Uri.parse(CALANDER_EVENT_URL), id);
            int rows = context.getContentResolver().delete(deleteUri, null, null);
            if (rows == -1) {
              // 事件删除失败
              return false;
            }
          }
        }
      }
      return true;
    } finally {
      if (eventCursor != null) {
        eventCursor.close();
      }
    }
  }
}
