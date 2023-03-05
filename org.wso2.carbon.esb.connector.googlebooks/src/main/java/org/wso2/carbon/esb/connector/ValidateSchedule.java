package org.wso2.carbon.esb.connector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ValidateSchedule {
	
	public static void validate_schedule_description(String description) {
		
	}
	
	public static void validate_schedule_source(String source) {
		
	}
	
	public static void validate_schedule_target(String target) {
		
	}
	
	public static void validate_schedule_config(String config) {
		
	}
	
	public static void validate_schedule_pgp(String pgp) {
		
	}
	
	public static void validate_schedule_validFrom(String validFrom) {
		
	}
	
	public static void validate_schedule_validUntil(String validUntil) {
		
	}
	
	public static String plan2 = null;
	public static void validate_daily_weekly_monthly(JsonElement daily, JsonElement weekly, JsonElement monthly) throws Exception {

		
		
		int nNil = 0;
		nNil = daily == null ? nNil: nNil + 1;

		nNil = weekly == null ? nNil: nNil + 1;
		if (nNil > 1) {
			throw new Exception("Only one of [daily, weekly, monthly] can be assigned at a time.");
		}

		nNil = monthly == null ? nNil: nNil + 1;
		if (nNil > 1) {
			throw new Exception("Only one of [daily, weekly, monthly] can be assigned at a time.");
		}
		
		if (nNil == 0) {
			throw new Exception("Requires schedule time in on of [daily, weekly, monthly].");
		}
		
		plan2 = null;

		// validate daily schedule
		if (daily != null) {
			JsonArray ar = ZAPI.getArray(daily, "times");
			for (JsonElement ari : ar) {
				String time = ari.getAsString();
				String[] sp = time.split(":");
				int HH = Integer.parseInt(sp[0]);
				int mm = Integer.parseInt(sp[1]);
				int ss = Integer.parseInt(sp[2]);
				int secondsOfDay = 3600 * HH + 60 * mm + ss;
				if (!(0 <= secondsOfDay && secondsOfDay <= 86400)) {
					throw new Exception("Error when calculate secondsOfDay: value must be between 0 and 86400.");
				}
			}
			plan2 = daily.toString();
		}

		// validate weekly schedule
		if (weekly != null) {
			String[] availDays = new String[] { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
			JsonArray days = ZAPI.getArray(weekly, "days");
			for (JsonElement day : days) {
				String dayName = ZAPI.getString(day, "name");
				boolean isAvaiDay = false;
				for (String avd : availDays) {
					if (avd.equalsIgnoreCase(dayName)) {
						isAvaiDay = true;
						break;
					}
				}
				if (!isAvaiDay) {
					throw new Exception("Invalid day: \"" + dayName + "\" not found in [monday, tuesday, wednesday, thursday, friday, saturday, sunday].");
				}
				JsonArray times = ZAPI.getArray(day, "times");
				for (JsonElement ari : times) {
					String time = ari.getAsString();
					String[] sp = time.split(":");
					int HH = Integer.parseInt(sp[0]);
					int mm = Integer.parseInt(sp[1]);
					int ss = Integer.parseInt(sp[2]);
					int secondsOfDay = 3600 * HH + 60 * mm + ss;
					if (!(0 <= secondsOfDay && secondsOfDay <= 86400)) {
						throw new Exception("Error when calculate secondsOfDay: value must be between 0 and 86400.");
					}
				}
			}
			plan2 = weekly.toString();
		}

		// validate monthly schedule
		if (monthly != null) {
			String[] availMonths = new String[] { "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december" };
			JsonArray months = ZAPI.getArray(monthly, "months");
			for (JsonElement month : months) {
				String monthName = ZAPI.getString(month, "name");
				boolean isAvaiMonth = false;
				for (String avm : availMonths) {
					if (avm.equalsIgnoreCase(monthName)) {
						isAvaiMonth = true;
						break;
					}
				}
				if (!isAvaiMonth) {
					throw new Exception("Invalid month: \"" + monthName + "\" not found in [january, february, march, april, may, june, july, august, september, october, november, december].");
				}
				JsonArray dates = ZAPI.getArray(month, "dates");
				for (JsonElement date : dates) {
					int day = Integer.parseInt(ZAPI.getString(date, "day"));
					if(day < 1 || day > 31) {
						throw new Exception("Invalid day: \"" + day + "\".");
					}
					JsonArray times = ZAPI.getArray(date, "times");
					for (JsonElement ari : times) {
						String time = ari.getAsString();
						String[] sp = time.split(":");
						int HH = Integer.parseInt(sp[0]);
						int mm = Integer.parseInt(sp[1]);
						int ss = Integer.parseInt(sp[2]);
						int secondsOfDay = 3600 * HH + 60 * mm + ss;
						if (!(0 <= secondsOfDay && secondsOfDay <= 86400)) {
							throw new Exception("Error when calculate secondsOfDay: value must be between 0 and 86400.");
						}
					}
				}
			}
			plan2 = monthly.toString();
		}
		
		if (plan2 == null) {
			throw new Exception("Missing schedule configuration.");
		}
	}
	
	public static void validateV2(JsonObject plan) throws Exception {
		JsonElement daily = null;
		JsonElement weekly = null;
		JsonElement monthly = null;
		
		if (plan.has("daily")) {
			daily = plan.get("daily");
		}
		if (plan.has("weekly")) {
			weekly = plan.get("weekly");
		}
		if (plan.has("monthly")) {
			monthly = plan.get("monthly");
		}
		
		validate_daily_weekly_monthly(daily, weekly, monthly);
	}
}