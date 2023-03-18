package org.wso2.carbon.esb.connector;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.net.ftp.FTPSClient;
import org.wso2.carbon.esb.connector.Item.Info;
import org.wso2.carbon.esb.connector.ZConnector.Constant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CAR02 {
	
	private static JsonArray listSites() throws Exception {
		JsonElement jsonSites = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/sites", "get", null, null);
		return jsonSites.getAsJsonObject().get("list").getAsJsonArray();
	}
	
	private static JsonArray listPGPs() throws Exception {
		JsonElement jsonPGPs = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/pgps", "get", null, null);
		return jsonPGPs.getAsJsonObject().get("list").getAsJsonArray();
	}
	
	private static JsonArray listConfigs() throws Exception {
		JsonElement jsonConfigs = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/configs", "get", null, null);
		return jsonConfigs.getAsJsonObject().get("list").getAsJsonArray();
	}
	
	public static boolean isScheduleTriggered(Timestamp time_____Now, Timestamp timePrevious, JsonObject plan) {
		
		try {
			
			Calendar calendarPrev = Calendar.getInstance();
			Calendar calendarNow = Calendar.getInstance();
			
			calendarPrev.setTime(timePrevious);
			calendarNow.setTime(time_____Now);
			
			long millisNow = calendarNow.getTimeInMillis();
			long millisPrev = calendarPrev.getTimeInMillis();
			long delta = millisNow - millisPrev;
			if (delta > 120 * 1000) {
				return false;
				//throw new Exception("Unacceptable difference between timeNow and timePrev.");
			}
			
			int hPrev = calendarPrev.get(Calendar.HOUR_OF_DAY);
			int mPrev = calendarPrev.get(Calendar.MINUTE);
			int sPrev = calendarPrev.get(Calendar.SECOND);
			int sDayPrev = 3600 * hPrev + 60 * mPrev + sPrev;

			int hNow = calendarNow.get(Calendar.HOUR_OF_DAY);
			int mNow = calendarNow.get(Calendar.MINUTE);
			int sNow = calendarNow.get(Calendar.SECOND);
			int sDayNow = 3600 * hNow + 60 * mNow + sNow;
			
			// day roll-over
			sDayPrev = sDayPrev > sDayNow ? sDayPrev - 3600 * 24 : sDayPrev;

			int dayOfWeek = calendarNow.get(Calendar.DAY_OF_WEEK);
			int currentDayOfMonth = calendarNow.get(Calendar.DAY_OF_MONTH);
			int indexMonthCurrent = calendarNow.get(Calendar.MONTH);
			
			if (plan.has("daily")) {
				JsonArray times = plan.get("daily").getAsJsonObject().get("times").getAsJsonArray();
				for (JsonElement time : times) {
					String HH_mm_ss = time.getAsString();
					int HH = Integer.parseInt(HH_mm_ss.substring(0, 2));
					int mm = Integer.parseInt(HH_mm_ss.substring(3, 5));
					int ss = Integer.parseInt(HH_mm_ss.substring(6, 8));
					int sDay = 3600 * HH + 60 * mm + ss;
					if (sDayPrev <= sDay && sDay < sDayNow) {
						return true;
					}
				}
			} else if (plan.has("weekly")) {
				JsonArray days = plan.get("weekly").getAsJsonObject().get("days").getAsJsonArray();
				for (JsonElement day : days) {
					String dayName = day.getAsJsonObject().get("name").getAsString();
					int dayIndex = 0;
					if (dayName.equalsIgnoreCase("MONDAY")) { dayIndex = Calendar.MONDAY; }
					else if (dayName.equalsIgnoreCase("TUESDAY")) { dayIndex = Calendar.TUESDAY; }
					else if (dayName.equalsIgnoreCase("WEDNESDAY")) { dayIndex = Calendar.WEDNESDAY; }
					else if (dayName.equalsIgnoreCase("THURSDAY")) { dayIndex = Calendar.THURSDAY; }
					else if (dayName.equalsIgnoreCase("FRIDAY")) { dayIndex = Calendar.FRIDAY; }
					else if (dayName.equalsIgnoreCase("SATURDAY")) { dayIndex = Calendar.SATURDAY; }
					else if (dayName.equalsIgnoreCase("SUNDAY")) { dayIndex = Calendar.SUNDAY; }
					else { throw new Exception("Unknown day of week \"" + dayName + "\"."); }
					if (dayIndex == dayOfWeek) {
						JsonArray times = day.getAsJsonObject().get("times").getAsJsonArray();
						for (JsonElement time : times) {
							String HH_mm_ss = time.getAsString();
							int HH = Integer.parseInt(HH_mm_ss.substring(0, 2));
							int mm = Integer.parseInt(HH_mm_ss.substring(3, 5));
							int ss = Integer.parseInt(HH_mm_ss.substring(6, 8));
							int sDay = 3600 * HH + 60 * mm + ss;
							if (sDayPrev <= sDay && sDay < sDayNow) {
								return true;
							}
						}
					}
				}
			} else if (plan.has("monthly")) {
				JsonArray months = plan.get("monthly").getAsJsonObject().get("months").getAsJsonArray();
				for (JsonElement month : months) {
					String monthName = month.getAsJsonObject().get("name").getAsString();
					int indexMonthPlanned = 0;
					if (monthName.equalsIgnoreCase("JANUARY")) { indexMonthPlanned = Calendar.JANUARY; }
					else if (monthName.equalsIgnoreCase("FEBRUARY")) { indexMonthPlanned = Calendar.FEBRUARY; }
					else if (monthName.equalsIgnoreCase("MARCH")) { indexMonthPlanned = Calendar.MARCH; }
					else if (monthName.equalsIgnoreCase("APRIL")) { indexMonthPlanned = Calendar.APRIL; }
					else if (monthName.equalsIgnoreCase("MAY")) { indexMonthPlanned = Calendar.MAY; }
					else if (monthName.equalsIgnoreCase("JUNE")) { indexMonthPlanned = Calendar.JUNE; }
					else if (monthName.equalsIgnoreCase("JULY")) { indexMonthPlanned = Calendar.JULY; }
					else if (monthName.equalsIgnoreCase("AUGUST")) { indexMonthPlanned = Calendar.AUGUST; }
					else if (monthName.equalsIgnoreCase("SEPTEMBER")) { indexMonthPlanned = Calendar.SEPTEMBER; }
					else if (monthName.equalsIgnoreCase("OCTOBER")) { indexMonthPlanned = Calendar.OCTOBER; }
					else if (monthName.equalsIgnoreCase("NOVEMBER")) { indexMonthPlanned = Calendar.NOVEMBER; }
					else if (monthName.equalsIgnoreCase("DECEMBER")) { indexMonthPlanned = Calendar.DECEMBER; }
					else { throw new Exception("Unknown month of year \"" + monthName + "\"."); }
					if (indexMonthCurrent == indexMonthPlanned) {
						JsonArray dates = month.getAsJsonObject().get("dates").getAsJsonArray();
						for (JsonElement date : dates) {
							int day = date.getAsJsonObject().get("day").getAsInt();
							if (currentDayOfMonth == day) {
								JsonArray times = date.getAsJsonObject().get("times").getAsJsonArray();
								for (JsonElement time : times) {
									String HH_mm_ss = time.getAsString();
									int HH = Integer.parseInt(HH_mm_ss.substring(0, 2));
									int mm = Integer.parseInt(HH_mm_ss.substring(3, 5));
									int ss = Integer.parseInt(HH_mm_ss.substring(6, 8));
									int sDay = 3600 * HH + 60 * mm + ss;
									if (sDayPrev <= sDay && sDay < sDayNow) {
										return true;
									}
								}
							}
						}
					}
				}
			} else {
				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private static boolean isFileToMove(Info info, ArrayList<Info> infos, Config config) throws Exception {
		// TODO: check sorted
		try {
			if (info.isDirectory) {
				return false;
			}
			if (config.fnIsFileNameToMove == null) {
				return true;
			}
			if (config.fnIsFileNameToMove.length() == 0) {
				return true;
			}
			return FileNaming.isFileNameToMove(info.name, config.fnIsFileNameToMove);
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	private static void generateItemsForPendingTasks(ArrayList<Task> pendingTasks, Timestamp now) throws Exception {
		
		Timestamp _1970 = Timestamp.valueOf("1970-01-01 00:00:00");
		
		for (Task task : pendingTasks) {
			
			Task.Status status = Task.Status.PENDING;
			
			try {
				
				// view transfer config
				JsonElement jsonConfig = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + task.workspace + "/configs/" + task.config, "get", null, null);
				Config config = Config.parse(jsonConfig);
				
				// list new items to be initiated
				ArrayList<String> itemsNew = new ArrayList<String>(); 
				JsonElement jsonItems = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + task.workspace + "/sites/" + task.source + "/objects", "get", null, null);
				JsonArray items = jsonItems.getAsJsonObject().get("objects").getAsJsonArray();
				ArrayList<Item.Info> infos = new ArrayList<Item.Info>();
				for (JsonElement item : items) { infos.add(Item.Info.parse(item)); }
				for (Item.Info info : infos) {
					if (isFileToMove(info, infos, config)) {
						itemsNew.add(task.workspace + ":" + task.id + ":" + info.name);
					}
				}
				
				// try initiate fresh items
				for (String itemName : itemsNew) {
					
					Item x = new Item();
					x.name = itemName;
					x.retryQuota = config.retryCount;
					x.retryRemaining = config.retryCount + 1;
					x.retryIntervalMs = config.retryIntervalMs;
					x.timeNextRetry = Timestamp.valueOf(new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(_1970));
					x.timeLatestRetry = Timestamp.valueOf(new SimpleDateFormat(ZConnector.Constant.DATEFORMAT).format(_1970));
					x.status = Item.Status.CREATED.toString();
					
					JsonElement jsonItem = Item.toJsonElement(x);
					
					ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + task.workspace + "/tasks/" + task.id + "/items", "post", null, jsonItem);
					
				}
				
				status = Task.Status.EXECUTED;
				
			} catch (Exception ex) {
				ex.printStackTrace();
				status = Task.Status.ERROR;
			}
			
			try {
				// set task status
				if (status != Task.Status.PENDING) {
					ClientLib.setTaskStatus(task, status);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private static JsonArray arrItemsForConstructQueueMessage = new JsonArray();
	
	private static ArrayList<Item> listItemsInRetryOrCreated(Timestamp now) throws Exception {
		
		// clear arrItemsForConstructQueueMessage
		for (;;) {
			int size = arrItemsForConstructQueueMessage.size();
			if (size == 0) {
				break;
			} else {
				arrItemsForConstructQueueMessage.remove(0);
			}
		}
		
		ArrayList<Item> ret = new ArrayList<Item>();
		ArrayList<String> itemNames = new ArrayList<String>();
		
		// ------------------------------------------------------------------------------------------------------------------------ //
		
		// list items created
		JsonElement jsonCreated = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/items?status=created", "get", null, null);
		JsonArray arrCreated = jsonCreated.getAsJsonObject().get("list").getAsJsonArray();
		System.out.println("items in status created: " + arrCreated.size());
		for (JsonElement e : arrCreated) {
			Item item = Item.parse(e);
			if (!itemNames.contains(item.name)) {
				arrItemsForConstructQueueMessage.add(e);
				itemNames.add(item.name);
				ret.add(item);
			}
		}
		
		
		// list items retry: TODO
		JsonElement jsonRetry = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/items?status=waiting_for_retry", "get", null, null);
		JsonArray arrRetry = jsonRetry.getAsJsonObject().get("list").getAsJsonArray();
		System.out.println("items in status retrying: " + arrRetry.size());
		for (JsonElement e : arrRetry) {
			Item item = Item.parse(e);
			
			// check and update quota, reject if exceed
			if (item.retryRemaining < 1) {
				continue;
			}
			
			if (item.timeNextRetry.after(now)) {
				continue;
			}
			
			if (!itemNames.contains(item.name)) {
				arrItemsForConstructQueueMessage.add(e);
				itemNames.add(item.name);
				ret.add(item);
				
				// this is very slow inside for-loop
				ClientLib.addItemLog(item, Log.Type.INFO, "Recovering Item", "Item \"" + item.name + "\" is marked as retrying and will be put to queue.");
			}
		}
		
		// ------------------------------------------------------------------------------------------------------------------------ //
		
		System.out.println("itemsToQueue: " + ret.size());
		return ret;
	}
	
	private static ArrayList<Task> listPendingTasks() throws Exception {
		ArrayList<Task> ret = new ArrayList<Task>();
		JsonElement jsonTasks = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/tasks?status=pending", "get", null, null);
		JsonArray arr = jsonTasks.getAsJsonObject().get("list").getAsJsonArray();
		for (JsonElement e : arr) {
			Task task = Task.parse(e);
			ret.add(task);
		}
		System.out.println("pendingTasks: " + ret.size());
		return ret;
	}
	
	private static void initiateTasks(ArrayList<Schedule> schedules, Timestamp now) {
		for (Schedule schedule : schedules) {
			try {
				if (schedule.enabled < 1) {
					continue;
				}
				
				/*if (now.before(schedule.validFrom)) {
					continue;
				}
				
				if (now.after(schedule.validUntil)) {
					continue;
				}*/
				
				if (isScheduleTriggered(now, schedule.previousCheckpoint, new Gson().fromJson(schedule.plan, JsonElement.class).getAsJsonObject())) {
					
					Task x = new Task();
					x.source = schedule.source;
					x.target = schedule.target;
					x.pgp = schedule.pgp;
					x.config = schedule.config;
					x.schedule = schedule.name;
					x.description = "Auto-scheduled by \"" + schedule.name + "\".";
					x.status = Task.Status.PENDING.toString();
					
					JsonElement jsonTask = Task.toJsonElement(x);
					
					ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + schedule.workspace + "/tasks", "post", null, jsonTask);
					
					System.out.println("triggered: " + schedule.name);
				}
				
				// update checkpoint
				ClientLib.updateScheduleCheckpoint(schedule, now);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private static ArrayList<Schedule> listAllEnabledSchedule() throws Exception{
		ArrayList<Schedule> schedules = new ArrayList<Schedule>();
		JsonElement jsonSchedules = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/enabled-schedules", "get", null, null);
		
		// plan, stringify
		JsonArray arr = jsonSchedules.getAsJsonObject().get("list").getAsJsonArray();
		for (JsonElement ei : arr) {
			JsonObject o = ei.getAsJsonObject();
			String planString = o.get("plan").toString();
			o.remove("plan");
			o.addProperty("plan", planString);
		}
		
		Schedule[] schs = Schedule.parse(jsonSchedules.getAsJsonObject().get("list").getAsJsonArray());
		for (Schedule s : schs) {
			schedules.add(s);
		}
		return schedules;
	}

	//private static ArrayList<Schedule> listSchedule(Workspace[] workspaces) throws Exception{
	//	ArrayList<Schedule> schedules = new ArrayList<Schedule>();
	//	for (Workspace workspace : workspaces) {
	//		JsonElement jsonSchedules = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + workspace.name + "/schedules", "get", null, null);
	//		
	//		// plan, stringify
	//		JsonArray arr = jsonSchedules.getAsJsonObject().get("list").getAsJsonArray();
	//		for (JsonElement ei : arr) {
	//			JsonObject o = ei.getAsJsonObject();
	//			String planString = o.get("plan").toString();
	//			o.remove("plan");
	//			o.addProperty("plan", planString);
	//		}
	//		
	//		Schedule[] schs = Schedule.parse(jsonSchedules.getAsJsonObject().get("list").getAsJsonArray());
	//		for (Schedule s : schs) {
	//			schedules.add(s);
	//		}
	//	}
	//	System.out.println("schedules: " + schedules.size());
	//	return schedules;
	//}
	
	private static Workspace[] listWorkspaces() throws Exception {
		JsonElement jsonWorkspaces = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces", "get", null, null);
		Workspace[] workspaces = Workspace.parse(jsonWorkspaces.getAsJsonObject().get("list").getAsJsonArray());
		System.out.println("workspaces: " + workspaces.length);
		return workspaces;
	}
	
	private static void packedAndQueue(Item item, JsonArray sites, JsonArray pgps, JsonArray configs, JsonArray distinctTasks) throws Exception {
		
		// find task
		Task task = null;
		for (JsonElement e : distinctTasks) {
			Task t = Task.parse(e);
			if (t.id == item.task) {
				task = t;
				break;
			}
		}
		
		// find site source
		JsonElement siteSource = null;
		JsonElement siteTarget = null;
		for (JsonElement e : sites) {
			
			if (siteSource != null && siteTarget != null) {
				break;
			}
			
			Site s = Site.parse(e);
			if (siteSource == null) {
				if (s.name.equals(task.source)) {
					siteSource = e;
				}
			}
			if (siteTarget == null) {
				if (s.name.equals(task.target)) {
					siteTarget = e;
				}
			}
		}
		
		// find config
		JsonElement config = null;
		for (JsonElement e : configs) {
			Config cfg = Config.parse(e);
			if (cfg.name.equals(task.config)) {
				config = e;
				break;
			}
		}
		
		// find pgp
		JsonElement pgp = null;
		if (task.pgp != null) {
			for (JsonElement e : pgps) {
				PGP p = PGP.parse(e);
				if (p.name.equals(task.pgp)) {
					pgp = e;
					break;
				}
			}
		}
		
		// get from arrItemsForConstructQueueMessage
		JsonElement eItem = null;
		for (JsonElement e : arrItemsForConstructQueueMessage) {
			Item x = Item.parse(e);
			if (x.name.equals(item.name)) {
				eItem = e;
				break;
			}
		}
		
		// enqueue
		String fileName = item.name.split(":")[2];
		String text = "{\"source\":" + siteSource + ",\"target\":" + siteTarget + ",\"pgp\":" + pgp + ",\"config\":" + config + ",\"fileName\":\"" + fileName + "\", \"item\":" + eItem + "}";
		String queueName = Site.parse(siteSource).name + "--to--" + Site.parse(siteTarget).name;
		try {
			Queue.Publisher.enqueue(queueName, text);
			ClientLib.setItemStatus(item, Item.Status.QUEUED);
		} catch (Exception ex) { ex.printStackTrace(); }
	}
	
	public static void enqueue(ArrayList<Item> itemsToQueue, JsonArray sites, JsonArray pgps, JsonArray configs) throws Exception {
		
		try {
			Queue.Publisher.open();
			
			String sep = "dVGwpG0k";
			ArrayList<String> al = new ArrayList<String>(); 
			for (Item item : itemsToQueue) {
				String s = item.workspace + sep + item.task;
				if (!al.contains(s)) {
					al.add(s);
				}
			}
			
			JsonArray distinctTasks = new JsonArray();
			for (String s : al) {
				try {
					String[] sp = s.split(sep);
					JsonElement jsonTask = ClientLib.getJsonResponse(ZWorker.WTRANSFER_API_ENDPOINT + "/workspaces/" + sp[0] + "/tasks/" + sp[1], "get", null, null);
					distinctTasks.add(jsonTask);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			for (Item item : itemsToQueue) {
				packedAndQueue(item, sites, pgps, configs, distinctTasks);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		Queue.Publisher.close();
	}
	
	public static ZConnector.ZResult getResult() {
		try {
			car02doWork();
			return ZConnector.ZResult.OK_200("{\"executed\": \"" + System.currentTimeMillis() + "\"}");
		} catch (Exception ex) {
			return ZConnector.ZResult.ERROR_500(ex);
		}
	}
	
	public static void car02doWork() throws Exception {
		
		// list all sites
		JsonArray sites = listSites();
		
		// list all pgps
		JsonArray pgps = listPGPs();
		
		// list all configs
		JsonArray configs = listConfigs();
		
		// for schedule trigger
		Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		
		// list workspaces
		//Workspace[] workspaces = listWorkspaces();
		
		// TODO: delete too old items in status created
		//cleanUnhealthyItems(workspaces);
		
		// list schedules on all workspaces
		ArrayList<Schedule> schedules = listAllEnabledSchedule();
		
		// initiate tasks from triggered schedules
		initiateTasks(schedules, now);
		
		// TODO: what to do with task error (totally failed, unable to connect site)
		//???
		
		// list pending tasks
		ArrayList<Task> pendingTasks = listPendingTasks();
		
		// create new items for new pending tasks -> also update tasks status
		generateItemsForPendingTasks(pendingTasks, now);
		
		// list items in retry mode or created mode
		ArrayList<Item> itemsToQueue = listItemsInRetryOrCreated(now);
		
		// enqueue those items
		enqueue(itemsToQueue, sites, pgps, configs);
	}
}