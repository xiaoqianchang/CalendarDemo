package com.changxiao.calendardemo;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private final String TAG = "CalendarManager";

  private Context mContext;

  // 系统calendar content provider相关的uri，以下为Android2.2版本以后的uri
  private String CALANDER_URL = "content://com.android.calendar/calendars";
  private String CALANDER_EVENT_URL = "content://com.android.calendar/events";
  private String CALANDER_REMIDER_URL = "content://com.android.calendar/reminders";
  private String CALANDER_ATTENDEE_URL = "content://com.android.calendar/attendees";

  /**
   * 使用以下Uri时，Android版本>=14;
   * 注意引用包路径：android.provider.CalendarContract下的；
   **/
  private Uri calendarsUri = CalendarContract.Calendars.CONTENT_URI;
  private Uri eventsUri = CalendarContract.Events.CONTENT_URI;
  private Uri remindersUri = CalendarContract.Reminders.CONTENT_URI;
  private Uri attendeesUri = CalendarContract.Attendees.CONTENT_URI;

  /** Calendars table columns */
  public static final String[] CALENDARS_COLUMNS = new String[] {
      CalendarContract.Calendars._ID,                           // 0
      CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
      CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
      CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
  };

  /** Events table columns */
  public static final String[] EVENTS_COLUMNS = new String[] {
      CalendarContract.Events._ID,
      CalendarContract.Events.CALENDAR_ID,
      CalendarContract.Events.TITLE,
      CalendarContract.Events.DESCRIPTION,
      CalendarContract.Events.EVENT_LOCATION,
      CalendarContract.Events.DTSTART,
      CalendarContract.Events.DTEND,
      CalendarContract.Events.EVENT_TIMEZONE,
      CalendarContract.Events.HAS_ALARM,
      CalendarContract.Events.ALL_DAY,
      CalendarContract.Events.AVAILABILITY,
      CalendarContract.Events.ACCESS_LEVEL,
      CalendarContract.Events.STATUS,
  };

  /** Reminders table columns */
  public static final String[] REMINDERS_COLUMNS = new String[] {
      CalendarContract.Reminders._ID,
      CalendarContract.Reminders.EVENT_ID,
      CalendarContract.Reminders.MINUTES,
      CalendarContract.Reminders.METHOD,
  };

  /** Attendees table columns */
  public static final String[] ATTENDEES_COLUMNS = new String[] {
      CalendarContract.Attendees._ID,
      CalendarContract.Attendees.ATTENDEE_NAME,
      CalendarContract.Attendees.ATTENDEE_EMAIL,
      CalendarContract.Attendees.ATTENDEE_STATUS
  };

  private static class InnerHolder {
    private static final CalendarManager instance = new CalendarManager();
  }

  public static CalendarManager getInstance() {
    return InnerHolder.instance;
  }

  private CalendarManager() {
  }

  public void init(Context context) {
    if (context != null) {
      mContext = context.getApplicationContext();
    } else {
      // TODO: 2019/4/24  这里需要application
//      mContext = context;
    }
  }

  /**
   * 获取时区
   * @return
   */
  public String[] getTimeZones() {
    return TimeZone.getAvailableIDs();
  }

  /**
   * 更新日程提醒
   * @return Uri
   * @throws Exception
   */
  public boolean updateReminder(Context context, ReminderModel model) {
    if (null == model) {
      return false;
    }
    if (!isExistReminder(model.getId())) {
      return false;
    }
    ContentValues reminderVal = new ContentValues();

    reminderVal.put(Reminders.MINUTES, model.getMinutes()); // 提醒在事件前多少分钟后发出
    //提醒方法
    int methodType = Reminders.METHOD_DEFAULT;
    int method = model.getMethod();
    if (method == 1) {
      methodType = Reminders.METHOD_ALERT;
    } else if (method == 2) {
      methodType = Reminders.METHOD_EMAIL;
    } else if (method == 3) {
      methodType = Reminders.METHOD_SMS;
    }
    reminderVal.put(Reminders.METHOD, methodType);

    try{
      context.getContentResolver().update(Uri.parse(CALANDER_REMIDER_URL), reminderVal, Reminders._ID + "=" + model.getId() , null);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * 更新日程事件
   * @param model
   * @return
   */
  public boolean updateEvent(Context context, EventModel model) {
    if (model == null) {
      return false;
    }
    if (!isExistEvent(model.getId())) {
      return false;
    }

    ContentValues values = new ContentValues();

    values.put(Events.CALENDAR_ID, model.getCalendarId());
    values.put(Events.TITLE, model.getTitle());
    values.put(Events.DESCRIPTION, model.getDescription());
    values.put(Events.EVENT_LOCATION, model.getEventLocation());

    //计算开始、结束时间，全部用Date也可以。
    Calendar startCld 	= Calendar.getInstance();
    Calendar endCld 	= Calendar.getInstance();
    //如果是全天事件的话，取开始时间的那一整天
    if (model.isAllDay()) {
      values.put(Events.ALL_DAY, true);
      values.put(Events.DTSTART, model.getDtstart());
      values.put(Events.DTEND, model.getDtend());
    } else {
      values.put(Events.DTSTART, model.getDtstart());
      values.put(Events.DTEND, model.getDtend());
    }
    values.put(Events.EVENT_TIMEZONE, model.getEventTimezone());
    values.put(Events.HAS_ALARM, model.isHasAlarm());
    values.put(Events.AVAILABILITY, model.getAvailability());
    values.put(Events.ACCESS_LEVEL, model.getAccessLevel());
    try {
      context.getContentResolver().update(Uri.parse(CALANDER_EVENT_URL), values, Events._ID + "=" + model.getId(), null);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * 查询日程(事件、提醒、参与人)
   * @param model
   * @return
   */
  public List<EventModel> queryEvents(Context context, EventModel model) {
    List<EventModel> result = new ArrayList<>();

    StringBuilder selection = new StringBuilder();
    List<String> selectionArgs = new ArrayList<>();
    if (model != null) {
      selection.append(" 1=1 ");

      String calendarId 	= String.valueOf(model.getCalendarId());
      String eventId 		= String.valueOf(model.getId());
      String title 		= model.getTitle();
      String description 	= model.getDescription();
      String location 	= model.getEventLocation();
      String startDate	= String.valueOf(model.getDtstart());
      String endDate		= String.valueOf(model.getDtend());
      String status		= String.valueOf(model.getEventStatus());

      selection.append(" AND " + Events.CALENDAR_ID + "=? ");
      selectionArgs.add(calendarId);
      selection.append(" AND " + Events._ID + "=? ");
      selectionArgs.add(eventId);
      selection.append(" AND " + Events.TITLE + " LIKE ? ");
      selectionArgs.add("%" + title + "%");
      selection.append(" AND " + Events.DESCRIPTION + " LIKE ? ");
      selectionArgs.add("%" + description + "%");
      selection.append(" AND " + Events.EVENT_LOCATION + " LIKE ? ");
      selectionArgs.add("%" + location + "%");
      selection.append(" AND " + Events.STATUS + " =? ");
      selectionArgs.add(status);

      selection.append(" AND " + Events.DTSTART + " >=? ");
      selectionArgs.add(startDate + "");
      selection.append(" AND " + Events.DTEND + " <=? ");
      selectionArgs.add(endDate + "");
    }
//		EVENTS_COLUMNS 换成 null 查询所有字段
    Cursor eventsCursor = context.getContentResolver().query(
        Uri.parse(CALANDER_EVENT_URL),
        EVENTS_COLUMNS,
        selection.length() == 0 ? null : selection.toString(),
        selectionArgs.size() == 0 ?	null : selectionArgs.toArray(new String[]{}),
        null);

    Map<String, Object> event = new HashMap<>();
    EventModel eventModel = new EventModel();
    while (eventsCursor.moveToNext()) {
      //以下字段解释，在添加事件里可查看addEvents()
      String eid 		= eventsCursor.getString(eventsCursor.getColumnIndex(Events._ID));
      String calendarId 	= eventsCursor.getString(eventsCursor.getColumnIndex(Events.CALENDAR_ID));
      String title 		= eventsCursor.getString(eventsCursor.getColumnIndex(Events.TITLE));
      String description 	= eventsCursor.getString(eventsCursor.getColumnIndex(Events.DESCRIPTION));
      String location 	= eventsCursor.getString(eventsCursor.getColumnIndex(Events.EVENT_LOCATION));
      long startDate 		= eventsCursor.getLong(eventsCursor.getColumnIndex(Events.DTSTART));
      long endDate 		= eventsCursor.getLong(eventsCursor.getColumnIndex(Events.DTEND));
      String timeZone 	= eventsCursor.getString(eventsCursor.getColumnIndex(Events.EVENT_TIMEZONE));
      String hasAlarm 	= eventsCursor.getString(eventsCursor.getColumnIndex(Events.HAS_ALARM));
      String allDay 		= eventsCursor.getString(eventsCursor.getColumnIndex(Events.ALL_DAY));
      String availability = eventsCursor.getString(eventsCursor.getColumnIndex(Events.AVAILABILITY));
      String accessLevel 	= eventsCursor.getString(eventsCursor.getColumnIndex(Events.ACCESS_LEVEL));
      String status 		= eventsCursor.getString(eventsCursor.getColumnIndex(Events.STATUS));

      Calendar calendar = Calendar.getInstance();

//      eventModel.setId(eid);
      event.put("calendarId",		calendarId);
      event.put("title",			title);
      event.put("description",	description);
      event.put("location",		location);

      calendar.setTimeInMillis(startDate);
//      event.put("startDate",		Utils.getFormatCld(calendar));

      calendar.setTimeInMillis(endDate);
//      event.put("endDate",		Utils.getFormatCld(calendar));

      event.put("timeZone",		timeZone);
      event.put("hasAlarm",		hasAlarm);
      event.put("allDay",			allDay);
      event.put("availability",	availability);
      event.put("accessLevel",	accessLevel);
      event.put("status",			status);
      //查询提醒
      Cursor remindersCursor = context.getContentResolver().query(
          Uri.parse(CALANDER_REMIDER_URL),
          REMINDERS_COLUMNS,
          Reminders.EVENT_ID + "=?",
          new String[]{ eid },
          null);

      List<Map<String, Object>> reminders = new ArrayList<>();
      while (remindersCursor.moveToNext()) {
        Map<String, Object> reminder = new HashMap<>();

        String rid 		= remindersCursor.getString(remindersCursor.getColumnIndex(Reminders._ID));
        String eventId 	= remindersCursor.getString(remindersCursor.getColumnIndex(Reminders.EVENT_ID));
        String minutes 	= remindersCursor.getString(remindersCursor.getColumnIndex(Reminders.MINUTES));
        String method	= remindersCursor.getString(remindersCursor.getColumnIndex(Reminders.METHOD));

        reminder.put("id", 		rid);
        reminder.put("eventId", eventId);
        reminder.put("minutes", minutes);
        reminder.put("method", 	method);
        reminders.add(reminder);
      }
      remindersCursor.close();
      event.put("reminders", reminders);
      //查询参与人
      Cursor attendeesCursor = context.getContentResolver().query(
          Uri.parse(CALANDER_ATTENDEE_URL),
          ATTENDEES_COLUMNS,
          Attendees.EVENT_ID + "=?",
          new String[]{ eid },
          null);

      List<Map<String, Object>> attendees = new ArrayList<Map<String,Object>>();
      while (attendeesCursor.moveToNext()) {
        Map<String, Object> attendee = new HashMap<String, Object>();
        String rid 		= attendeesCursor.getString(attendeesCursor.getColumnIndex(Attendees._ID));
        String name 	= attendeesCursor.getString(attendeesCursor.getColumnIndex(Attendees.ATTENDEE_NAME));
        String email 	= attendeesCursor.getString(attendeesCursor.getColumnIndex(Attendees.ATTENDEE_EMAIL));
        String _status	= attendeesCursor.getString(attendeesCursor.getColumnIndex(Attendees.ATTENDEE_STATUS));

        attendee.put("id", 		rid);
        attendee.put("name", 	name);
        attendee.put("email", 	email);
        attendee.put("status", 	_status);
        attendees.add(attendee);
      }
      attendeesCursor.close();
      event.put("attendees", reminders);

//      result.add(event);
    }
    eventsCursor.close();

    return result;
  }

  /**
   * 添加日历事件、日程
   * @param calendarRemindModel
   */
  public Uri insertEvent(CalendarRemindModel calendarRemindModel) {
    if (null == mContext || null == calendarRemindModel) {
      return null;
    }
    // 获取日历账户的id
    int calId = checkAndAddCalendarAccount(mContext);
    if (calId < 0) {
      // 获取账户id失败直接返回，添加日历事件失败
      return null;
    }

    try {
      ContentValues event = new ContentValues();
      // 插入账户的id
      event.put(CalendarContract.Events.CALENDAR_ID, calId); // 日历事件属于的Calendars#_ID，必须有
      event.put(CalendarContract.Events.TITLE, calendarRemindModel.getTitle()); // 事件的标题
      event.put(CalendarContract.Events.DESCRIPTION, calendarRemindModel.getDescription()); // 事件的备注
      Calendar calendar = Calendar.getInstance();
      Calendar startCalendar = Calendar.getInstance();
      Calendar endCalendar = Calendar.getInstance();
      // 如果是全天事件的话，取开始时间的那一整天
      if (calendarRemindModel.isAllDay()) {
        // 开始时间
        startCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        // 结束时间
        endCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 24, 0, 0);
      } else {
        // 开始时间
        startCalendar.setTimeInMillis(calendarRemindModel.getStartDate()); // 设置开始时间
        // 结束时间
        endCalendar.setTimeInMillis(calendarRemindModel.getEndDate()); // 设置终止时间
      }
      event.put(CalendarContract.Events.ALL_DAY, calendarRemindModel.isAllDay());
      long start = startCalendar.getTimeInMillis();
      long end = endCalendar.getTimeInMillis();
      event.put(CalendarContract.Events.DTSTART, start); // 事件的启动时间，使用从纪元开始的UTC毫秒计时，必须有
      event.put(CalendarContract.Events.DTEND, end); // 事件的结束时间，使用从纪元开始的UTC毫秒计时，对于非重复发生的事件，必须有

      event.put(CalendarContract.Events.STATUS, 1); // 事件状态:暂定(0)，确认(1)或取消(2)
      event.put(CalendarContract.Events.HAS_ALARM, calendarRemindModel.getHasAlarm()); // 设置有闹钟提醒
      String timeZone = calendarRemindModel.getTimeZone();
      event.put(CalendarContract.Events.EVENT_TIMEZONE, TextUtils.isEmpty(timeZone) ? "Asia/Shanghai" : timeZone); // 事件所针对的时区，必须有
      String location = calendarRemindModel.getLocation();
      // 如果地址不为空插入地址
      if (!TextUtils.isEmpty(location)) {
        event.put(CalendarContract.Events.EVENT_LOCATION, location);
      }
      // 设置我的状态
      event.put(CalendarContract.Events.AVAILABILITY, calendarRemindModel.getAvailability());
      // 设置隐私
      event.put(CalendarContract.Events.ACCESS_LEVEL, calendarRemindModel.getAccessLevel());
      // 添加事件（这里如果calId不正确不会崩溃）
      return mContext.getContentResolver().insert(Uri.parse(CALANDER_EVENT_URL), event);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 批量插入日程
   * @param calendars
   * @return Map<String, Object>
   */
//  public boolean insertEvents(List<CalendarRemindModel> calendars) {
//    if (calendars == null || calendars.isEmpty()) {
//      return false;
//    }
//    ArrayList<ContentProviderOperation> ops = null;
//
//    for (int i = 0; i < calendars.size(); i++) {
//      //获得日程
//      CalendarRemindModel calendar = calendars.get(i);
//      //插入事件
//      Uri eUri = null;
//      try {
//        eUri = insertEvent(calendar);
//      } catch (Exception e) {
//        addResult.add("第" + (i + 1) + "条日程，添加事件失败：" + e.getMessage());
//      }
//      //如果事件插入成功，则插入提醒和参与者
//      if (!Utils.isEmpty(eUri)) {
//        String eventId = eUri.getLastPathSegment();
//        //存入插入事件的结果
//        addResult.add(eUri.toString());
//
//        ops = new ArrayList<ContentProviderOperation>();
//        //插入提醒，可以添加多个提醒
//        Map<Object, Map<String, String>> reminders = (Map<Object, Map<String, String>>) calendar.get("reminders");
//        if (!Utils.isEmpty(reminders)) {
//          for (Object key : reminders.keySet()) {
//            reminders.get(key).put("eventId", eventId);
//            try {
//              insertReminder(reminders.get(key), ops);
//            } catch (Exception e) {
//              Log.i(Const.APPTAG, e.getMessage());
//            }
//          }
//        }
//        //插入参与者，可以添加多个参与者
//        Map<Object, Map<String, String>> attendees = (Map<Object, Map<String, String>>) calendar.get("attendees");
//        if (!Utils.isEmpty(attendees)) {
//          for (Object key : attendees.keySet()) {
//            attendees.get(key).put("eventId", eventId);
//            try {
//              insertAttendee(attendees.get(key), ops);
//            } catch (Exception e) {
//              Log.i(Const.APPTAG, e.getMessage());
//            }
//          }
//        }
//        if (!Utils.isEmpty(ops)) {
//          //执行批量插入
//          try {
//            ContentProviderResult[] cps = resolver.applyBatch(CalendarContract.AUTHORITY, ops);
//            //event表插入返回的Uri集合
//            for (ContentProviderResult cp : cps) {
//              Log.i(Const.APPTAG, cp.toString());
//              addResult.add(cp.uri.toString());
//            }
//          } catch (Exception e) {
//            Log.i(Const.APPTAG, e.getMessage());
//            addResult.add("第" + (i + 1) + "条日程，添加(提醒和参与者)失败:" + e.getMessage());
//          }
//        }
//      }
//    }
//    result.put("result", "1");
//    result.put("obj", addResult);
//
//    return result;
//  }
  /**
   * 插入日程参与者，如果参数ops不为空，则不执行插入，添加到ops里执行批量插入
   * @param attendees
   * @param ops
   * @return Uri
   * @throws Exception
   */
//  public Uri insertAttendee(Map<String, String> attendees, ArrayList<ContentProviderOperation> ops) throws Exception{
//    if (Utils.isEmpty(attendees)) {
//      return null;
//    }
//    try {
//      String eventId		= attendees.get("eventId");//外键事件id
//      String name 		= attendees.get("name");//参与者姓名
//      //如果时间id、或者参与姓名为空，不添加参与者
//      if (!isExistEvent(Long.parseLong(eventId)) || Utils.isEmpty(name)) {
//        return null;
//      }
//      String email		= attendees.get("email");//参与者电子邮件
//
//      /** 没明白具体什么意思，暂时用默认值  */
//      int relationship	= Attendees.RELATIONSHIP_ATTENDEE;//与会者与事件的关系
//      int type			= Attendees.TYPE_OPTIONAL;//与会者的类型
//      int status			= Attendees.ATTENDEE_STATUS_INVITED;//与会者的状态
//      if (ops == null) {
//        ContentValues attendeesVal = new ContentValues();
//        attendeesVal.put(Attendees.EVENT_ID, eventId);
//        attendeesVal.put(Attendees.ATTENDEE_NAME, name);
//        if (!Utils.isEmpty(email)) {
//          attendeesVal.put(Attendees.ATTENDEE_EMAIL, email);//参与者 email
//        }
//
//        attendeesVal.put(Attendees.ATTENDEE_RELATIONSHIP, relationship);//关系
//        attendeesVal.put(Attendees.ATTENDEE_TYPE, type);//类型
//        attendeesVal.put(Attendees.ATTENDEE_STATUS, status);//状态
//
//        Uri uri = resolver.insert(attendeesUri, attendeesVal);
//        return uri;
//      } else {
//        Builder builder = ContentProviderOperation.newInsert(attendeesUri)
//            .withYieldAllowed(true)
//            .withValue(Attendees.EVENT_ID, eventId)
//            .withValue(Attendees.ATTENDEE_NAME, name)
//            .withValue(Attendees.ATTENDEE_EMAIL, email)
//            .withValue(Attendees.ATTENDEE_RELATIONSHIP, relationship)
//            .withValue(Attendees.ATTENDEE_TYPE, type)
//            .withValue(Attendees.ATTENDEE_STATUS, status);
//        if (!Utils.isEmpty(email)) {
//          builder.withValue(Attendees.ATTENDEE_EMAIL, email);
//        }
//        ops.add(builder.build());
//      }
//    }catch (Exception e) {
//      throw e;
//    }
//    return null;
//  }
  /**
   * 插入日程提醒，如果参数ops不为空，则不执行插入。
   * @param reminders
   * @param ops
   * @return Uri
   */
//  public Uri insertReminder(Map<String, String> reminders, ArrayList<ContentProviderOperation> ops)
//      throws Exception {
//    //---------------------------Reminders表的数据------------------------------------
//    //插入提醒，可以添加多个提醒
//    if (!Utils.isEmpty(reminders)) {
//      try {
//        String eventId		= reminders.get("eventId");//外键事件id
//        //如果时间id为空，不添加提醒
//        if (!isExistEvent(Long.parseLong(eventId))) {
//          return null;
//        }
//
//        String mimutes 		= reminders.get("mimutes");//提醒在事件前几分钟后发出
//        String method		= reminders.get("method");//提醒方法:METHOD_DEFAULT:0,*_ALERT:1,*_EMAIL:2,*_SMS:3
//
//        //提醒方法
//        int methodType = Reminders.METHOD_DEFAULT;
//        if (method.equals("1")) {
//          methodType = Reminders.METHOD_ALERT;
//        } else if (method.equals("2")) {
//          methodType = Reminders.METHOD_EMAIL;
//        } else if (method.equals("3")) {
//          methodType = Reminders.METHOD_SMS;
//        }
//        //提醒时间
//        int m = Utils.isNumber(mimutes) ? Integer.parseInt(mimutes) : 0;
//
//        if (ops == null) {
//          ContentValues alarmVal = new ContentValues();
//          alarmVal.put(Reminders.EVENT_ID, eventId);
//          alarmVal.put(Reminders.MINUTES, m);//提醒在事件前多少分钟后发出
//          alarmVal.put(Reminders.METHOD, methodType);
//
//          Uri uri = resolver.insert(remindersUri, alarmVal);
//          return uri;
//        } else {
//          ContentProviderOperation op = ContentProviderOperation.newInsert(remindersUri)
//              .withYieldAllowed(true)
//              .withValue(Reminders.EVENT_ID, eventId)
//              .withValue(Reminders.MINUTES, m)
//              .withValue(Reminders.METHOD, methodType)
//              .build();
//          ops.add(op);
//        }
//      } catch (Exception e) {
//        throw e;
//      }
//    }
//    return null;
//  }

  /**
   * 查询event是否存在
   * @param id
   * @return
   */
  public boolean isExistEvent(long id) {
    Cursor cursor = mContext.getContentResolver().query(
        Uri.parse(CALANDER_EVENT_URL),
        new String[]{ Events._ID},
        Events._ID + "=" + id,
        null,
        null);
    if (cursor.moveToFirst()) {
      return true;
    }
    return false;
  }

  /**
   * 查询Reminder是否存在
   * @param id
   * @return
   */
  public boolean isExistReminder(long id) {
    Cursor cursor = mContext.getContentResolver().query(
        Uri.parse(CALANDER_REMIDER_URL),
        new String[]{ Reminders._ID},
        Reminders._ID + "=" + id,
        null,
        null);
    if (cursor.moveToFirst()) {
      return true;
    }
    return false;
  }

  /**
   * 查询Attendee是否存在
   * @param id
   * @return
   */
  public boolean isExistAttendee(long id) {
    Cursor cursor = mContext.getContentResolver().query(
        Uri.parse(CALANDER_ATTENDEE_URL),
        new String[]{ Attendees._ID},
        Attendees._ID + "=" + id,
        null,
        null);
    if (cursor.moveToFirst()) {
      return true;
    }
    return false;
  }

  /**
   * 删除event表里数据
   * @return
   */
//  public Map<String, String> delEvents(List<String> ids, String calendarId, boolean delAll){
//    Map<String, String> result = new HashMap<String, String>();
//
//    String selection = null;
//
//    if (delAll) {
//      selection = Events._ID + " > 0";
//    } else if (Utils.isNumber(calendarId)) {
//      selection = Events.CALENDAR_ID + "=" + calendarId;
//    } else if(Utils.isEmpty(ids)){
//      result.put("result", "0");
//      result.put("obj", "要删除日程事件的id为空！");
//      return result;
//    } else {
//      String where = "";
//      for (String id : ids) {
//        if (Utils.isNumber(id)) {
//          where += id + ",";
//        }
//      }
//      selection = Events._ID + " in(" + where.substring(0, where.length() - 1) + ")";
//    }
//
//    try {
//      Log.i(Const.APPTAG, "====：" + selection);
//      int n = resolver.delete(
//          eventsUri,
//          selection,
//          null);
//
//      result.put("result", "1");
//      result.put("obj", n + "");
//
//    } catch (Exception e) {
//      result.put("result", "-1");
//      result.put("obj", "删除错误：" + e.toString());
//    }
//    return result;
//  }

  /**
   * 更新日历的名称
   * @param param
   * @return Map
   */
//  public Map<String, String> updateCalendars(Map<String, String> param){
//    Map<String, String> result = new HashMap<String, String>();
//    if (Utils.isEmpty(param)) {
//      result.put("false", "更新参数不能为空！");
//      return result;
//    }
//
//    String calendarId = param.get("calendarId");
//    String displayName = param.get("displayName");
//
//    if (Utils.isEmpty(calendarId) && Utils.isNumber(calendarId)) {
//      result.put("false", "日历id不合法！");
//      return result;
//    }
//    if (Utils.isEmpty(displayName)) {
//      result.put("false", "日历名称不能为空！");
//      return result;
//    }
//
//    ContentValues values = new ContentValues();
//    values.put(Calendars.CALENDAR_DISPLAY_NAME, displayName);
//    Uri uri = ContentUris.withAppendedId(calendarsUri, Long.parseLong(calendarId));
//    int n = resolver.update(uri, values, null, null);
//    result.put("true", n + "");
//
//    return result;
//  }

  /**
   * 根据账户查询账户日历
   * @param param Map<String, String>
   * @return List
   */
//  public List<Map<String, String>> queryCalendars(Map<String, String> param){
//    String accountName = null;
//    String accountType = null;
//    String ownerAccount = null;
//
//    if (!Utils.isEmpty(param)) {
//      accountName = param.get("accountName");//账户名称
//      accountType = param.get("accountType");//账户类型
//      ownerAccount = param.get("ownerAccount");//拥有者账户
//    }
//
//    List<Map<String, String>> calendars = new ArrayList<Map<String,String>>();
//
//    Cursor cursor = null;
//    StringBuffer selection = new StringBuffer(" 1 = 1 ");
//    List<String> selectionArgs = new ArrayList<String>();
//    //本地帐户查询：ACCOUNT_TYPE_LOCAL是一个特殊的日历账号类型，它不跟设备账号关联。这种类型的日历不同步到服务器
//    //如果是谷歌的账户是可以同步到服务器的
//    if (Utils.isEmpty(accountName) && Utils.isEmpty(accountType) && Utils.isEmpty(ownerAccount)) {
//      selection.append(" AND " + Calendars.ACCOUNT_TYPE + " = ? ");
//      selectionArgs.add("LOCAL");
//    } else {
//      if (!Utils.isEmpty(accountName)) {
//        selection.append(" AND " + Calendars.ACCOUNT_NAME + " = ? ");
//        selectionArgs.add(accountName);
//      }
//      if (!Utils.isEmpty(accountType)) {
//        selection.append(" AND " + Calendars.ACCOUNT_TYPE + " = ? ");
//        selectionArgs.add(accountType);
//      }
//      if (!Utils.isEmpty(ownerAccount)) {
//        selection.append(" AND " + Calendars.OWNER_ACCOUNT + " = ? ");
//        selectionArgs.add(ownerAccount);
//      }
//    }
//    cursor = resolver.query(calendarsUri, CALENDARS_COLUMNS, selection.toString(),
//        selectionArgs.toArray(new String[]{}), null);
//    while (cursor.moveToNext()) {
//      Map<String, String> calendar = new HashMap<String, String>();
//      // Get the field values
//      calendar.put("calendarId", cursor.getString(0));
//      calendar.put("accountName", cursor.getString(1));
//      calendar.put("displayName", cursor.getString(2));
//      calendar.put("ownerAccount", cursor.getString(3));
//      Log.i(Const.APPTAG, "查询到日历：" + calendar);
//      calendars.add(calendar);
//    }
//    return calendars;
//  }





  /**
   * 检查是否有存在的账户。有则返回账户id，否则返回-1
   * @param context
   * @return
   */
  private int checkCalendarAccount(Context context) {
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
  private long addCalendarAccount(Context context) {
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
  private int checkAndAddCalendarAccount(Context context) {
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
  public boolean addCalendarEvent(Context context, CalendarRemindModel calendarRemindModel) {
    if (null == context || null == calendarRemindModel) {
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
    Calendar calendar = Calendar.getInstance();
    Calendar startCalendar = Calendar.getInstance();
    Calendar endCalendar = Calendar.getInstance();
    // 如果是全天事件的话，取开始时间的那一整天
    if (calendarRemindModel.isAllDay()) {
      // 开始时间
      startCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
      // 结束时间
      endCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 24, 0, 0);
    } else {
      // 开始时间
      startCalendar.setTimeInMillis(calendarRemindModel.getStartDate()); // 设置开始时间
      // 结束时间
      endCalendar.setTimeInMillis(calendarRemindModel.getEndDate()); // 设置终止时间
    }
    event.put(CalendarContract.Events.ALL_DAY, calendarRemindModel.isAllDay());
    long start = startCalendar.getTimeInMillis();
    long end = endCalendar.getTimeInMillis();
    event.put(CalendarContract.Events.DTSTART, start); // 事件的启动时间，使用从纪元开始的UTC毫秒计时，必须有
    event.put(CalendarContract.Events.DTEND, end); // 事件的结束时间，使用从纪元开始的UTC毫秒计时，对于非重复发生的事件，必须有

//    event.put(CalendarContract.Events.STATUS, 1); // 事件状态:暂定(0)，确认(1)或取消(2)
    event.put(CalendarContract.Events.HAS_ALARM, calendarRemindModel.getHasAlarm()); // 设置有闹钟提醒
    String timeZone = calendarRemindModel.getTimeZone();
    event.put(CalendarContract.Events.EVENT_TIMEZONE, TextUtils.isEmpty(timeZone) ? "Asia/Shanghai" : timeZone); // 事件所针对的时区，必须有
    String location = calendarRemindModel.getLocation();
    // 如果地址不为空插入地址
    if (!TextUtils.isEmpty(location)) {
      event.put(CalendarContract.Events.EVENT_LOCATION, location);
    }
    // 设置我的状态
//    event.put(CalendarContract.Events.AVAILABILITY, calendarRemindModel.getAvailability());
    // 设置隐私
//    event.put(CalendarContract.Events.ACCESS_LEVEL, calendarRemindModel.getAccessLevel());
    // 添加事件（这里如果calId不正确不会崩溃）
    Uri newEvent = context.getContentResolver().insert(Uri.parse(CALANDER_EVENT_URL), event);
    if (newEvent == null) {
      // 添加日历事件失败直接返回
      return false;
    }

    // 事件提醒的设定，Reminders表（如果没有下面的代码，那么提醒选项将会是无）
    ContentValues values = new ContentValues();
    values.put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(newEvent));
    // 提醒：提前10分钟有提醒（若注释掉或者设为0表示日程发生时）（注释掉将不会有闹铃提醒）（无论HAS_ALARM是没有设置、设为1或是设为0，都会有闹铃）
    values.put(CalendarContract.Reminders.MINUTES, 0); // 应该在几分钟之前触发事件。
    // 在服务上设置的报警的方法，下列设置之一：
    // 1.  METHOD_ALERT
    // 2.  METHOD_DEFAULT
    // 3.  METHOD_EMAIL
    // 4.  METHOD_SMS
    values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
    // 这里会有SQLiteException（比如CALENDAR_ID不对时会抛出此异常）
    Uri uri = context.getContentResolver().insert(Uri.parse(CALANDER_REMIDER_URL), values);
    if(uri == null) {
      // 添加闹钟提醒失败直接返回
      return false;
    }
    return true;
  }

  public boolean hasCalendarEvent(Context context, String title) {
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
  public boolean deleteCalendarEvent(Context context, String title) {
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
