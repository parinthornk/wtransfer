package org.wso2.carbon.esb.connector;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.wso2.carbon.esb.connector.ZConnector.Constant;
import org.wso2.carbon.esb.connector.ZConnector.ZObject;
import org.wso2.carbon.esb.connector.ZConnector.ZResult;

public class ZAPI {
	
	public static String getString(JsonElement e, String fieldName) {
		try {
			return e.getAsJsonObject().get(fieldName).getAsString();
		} catch (Exception ex) {
			//ex.printStackTrace();
			return null;
		}
	}
	
	public static JsonElement getElement(JsonElement e, String fieldName) {
		try {
			return e.getAsJsonObject().get(fieldName);
		} catch (Exception ex) {
			return null;
		}
	}
	
	public static JsonArray getArray(JsonElement e, String fieldName) {
		try {
			return e.getAsJsonObject().get(fieldName).getAsJsonArray();
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static int getInt(JsonElement e, String fieldName, int defVal) {
		try {
			return Integer.parseInt(e.getAsJsonObject().get(fieldName).getAsString());
		} catch (Exception ex) {
			return defVal;
		}
	}
	
	private static String getFromJsonBodyAsString(byte[] raw, String fieldName) {
		try {
			String jsonBody = new String(raw, StandardCharsets.UTF_8);
			JsonParser parser = new JsonParser();
			JsonObject neoJsonElement = (JsonObject) parser.parse(jsonBody);
			return neoJsonElement.get(fieldName).getAsString();
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static boolean getFromJsonBodyAsBoolean(byte[] raw, String fieldName, boolean defaultValue) {
		try {
			String text = getFromJsonBodyAsString(raw, fieldName);
			return text.equalsIgnoreCase("true") || text.equalsIgnoreCase("1");			
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static int getFromJsonBodyAsInteger(byte[] raw, String fieldName, int defaultValue) {
		try {
			return Integer.parseInt(getFromJsonBodyAsString(raw, fieldName));
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static java.sql.Timestamp getFromJsonBodyAsTimestamp(byte[] raw, String fieldName) {
		try {
			return java.sql.Timestamp.valueOf(getFromJsonBodyAsString(raw, fieldName));
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static long getFromJsonBodyAsLong(byte[] raw, String fieldName, long defaultValue) {
		try {
			return Long.parseLong(getFromJsonBodyAsString(raw, fieldName));
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static String getFromPathParamsAsString(String path, int position) {
		try {
			
			String val = path.split("/")[1 + position];
			try {
				val = java.net.URLDecoder.decode(val, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
			    // not going to happen - value came from JDK's own StandardCharsets
			}
			return val;
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static int getFromPathParamsAsInteger(String path, int position, int defaultValue) {
		try {
			return Integer.parseInt(path.split("/")[1 + position]);
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	private static boolean matchPath(String requested, String template) {
		String[] splitRequested = requested.split("/");
		String[] splitTemplate = template.split("/");
		if (splitRequested.length != splitTemplate.length) {
			return false;
		}
		for (int i=0;i<splitRequested.length;i++) {
			if (!splitTemplate[i].equalsIgnoreCase("*")) {
				if (!splitTemplate[i].equalsIgnoreCase(splitRequested[i])) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static boolean match(String path, String method, String target) {
		String[] sept = target.split(", ");
		String targetPath = sept[0];
		String targetMethod = sept[1];
		if (!matchPath(path, targetPath)) {
			return false;
		}
		if (!method.equalsIgnoreCase(targetMethod)) {
			return false;
		}
		return true;
	}
	
	private static ArrayList<Object> schedule_remove_plan_field(ResultSet resultSet) throws Exception {
		ArrayList<Object> ls1 = new ArrayList<Object>();
		while (resultSet.next()) {
			Map<String, Object> m1 = new HashMap<String, Object>();
			m1.put("workspace", resultSet.getString("workspace"));
			m1.put("name", resultSet.getString("name"));
			m1.put("description", resultSet.getString("description"));
			m1.put("source", resultSet.getString("source"));
			m1.put("target", resultSet.getString("target"));
			m1.put("pgp", resultSet.getString("pgp"));
			m1.put("config", resultSet.getString("config"));
			//m1.put("plan", resultSet.getString("plan"));
			
			try {
				m1.put("validfrom", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("validfrom")));
				m1.put("validuntil", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("validuntil")));
				m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
				m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
			} catch (Exception ex) {
				ex.printStackTrace();
				/*Map<String, Object> m2 = new HashMap<String, Object>();
				m2.put("error", "Failed to call resultSet.getTimestamp, make sure [validfrom, validuntil, created, modified] are valid in database.");
				ZResult ret = new ZResult();
				ret.content = ZConnector.ConvertToJsonString(m2);
				ret.statusCode = 500;
				return ret;*/
				throw new Exception("Failed to call resultSet.getTimestamp, make sure [validfrom, validuntil, created, modified] are valid in database.");
			}
			
			
			// remove field "plan", add field [daily, weekly, monthly] // ---------------------------------------------------------------- //
			
			String planString = resultSet.getString("plan");
			Map<String, Object> mt = new HashMap<String, Object>();

			Gson gson = new Gson();
			JsonElement element = gson.fromJson (planString, JsonElement.class);
			JsonObject jsonObj = element.getAsJsonObject();
			if (jsonObj.has("times")) {
				mt.put("times", jsonObj.get("times"));
				m1.put("daily", mt);
			}
			if (jsonObj.has("days")) {
				mt.put("days", jsonObj.get("days"));
				m1.put("weekly", mt);
			}
			if (jsonObj.has("months")) {
				mt.put("months", jsonObj.get("months"));
				m1.put("monthly", mt);
			}
			
			//System.out.println("jsonObj: " + jsonObj);
			
			// --------------------------------------------------------------------------------------------------------------------------- //
			
			
			ls1.add(m1);
		}
		
		return ls1;
	}
	
	public static ZConnector.ZResult process(String path, String method, HashMap<String, String> query, HashMap<String, String> header, byte[] bodyRaw) {
		try {
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			if (match(path, method, "/workspaces, get")) {
				return ZConnector.T_WKS.list();
			}
			
			if (match(path, method, "/workspaces, post")) {
				String name = getFromJsonBodyAsString(bodyRaw, "name");
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				return ZConnector.T_WKS.create(name, description);
			}
			
			if (match(path, method, "/workspaces/*, get")) {
				String name = getFromPathParamsAsString(path, 1);
				return ZConnector.T_WKS.get(name);
			}
			
			if (match(path, method, "/workspaces/*, patch")) {
				String name = getFromPathParamsAsString(path, 1);
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				HashMap<String, Object> map = new HashMap<String , Object>();
				if( description != null ) { map.put("description", description); }
				return ZConnector.T_WKS.update(name, map);
			}
			
			if (match(path, method, "/workspaces/*, delete")) {
				String name = getFromPathParamsAsString(path, 1);
				return ZConnector.T_WKS.delete(name);
			}
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			// TODO
			if (match(path, method, "/workspaces/*/schedules, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				//System.out.println(workspace);
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_SCH where workspace = '" + workspace + "'";
						resultSet = statement.executeQuery(sql);
						try {
							Map<String, Object> m2 = new HashMap<String, Object>();
							m2.put("list", schedule_remove_plan_field(resultSet));
							ZResult ret = new ZResult();
							ret.content = ZConnector.ConvertToJsonString(m2);
							ret.statusCode = 200;
							return ret;
						} catch (Exception ex) {
							Map<String, Object> m2 = new HashMap<String, Object>();
							m2.put("error", ex + "");
							ZResult ret = new ZResult();
							ret.content = ZConnector.ConvertToJsonString(m2);
							ret.statusCode = 500;
							return ret;
						}
					}
				});
			}
			
			// TODO
			if (match(path, method, "/workspaces/*/schedules, post")) {
				String plan2 = "";
				String workspace = getFromPathParamsAsString(path, 1);
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				
				
				// ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- // validate schedule begin
				try {
					
					String jsonIn = null;//new String(bodyRaw);
					try {
						jsonIn = new String(bodyRaw);
					} catch(Exception ex) {
						throw new Exception("Failed to parse JSON body.");
					}
					
					JsonElement e = null;
					
					try {
						e = new Gson().fromJson (jsonIn, JsonElement.class);
					} catch (Exception ex) {
						throw new Exception("Unable to parse request body as a JSON.");
					}
					
					String name = getString(e, "name"); if (name == null) { throw new Exception("JSON field \"name\" cannot be null."); }
					String descrption = getString(e, "descrption");
					String source = getString(e, "source"); if (source == null) { throw new Exception("JSON field \"source\" cannot be null."); }
					String target = getString(e, "target"); if (target == null) { throw new Exception("JSON field \"target\" cannot be null."); }
					String pgp = getString(e, "pgp");
					String config = getString(e, "config"); if (config == null) { throw new Exception("JSON field \"config\" cannot be null."); }
					String validFrom = getString(e, "validFrom"); if (validFrom == null) { throw new Exception("JSON field \"validFrom\" cannot be null."); }
					String validUntil = getString(e, "validUntil"); if (validUntil == null) { throw new Exception("JSON field \"validUntil\" cannot be null."); }
					
					Timestamp tValidFrom = null;
					Timestamp tValidUntil = null;
					
					try {
						tValidFrom = Timestamp.valueOf(validFrom);
					} catch (Exception ex) {
						throw new Exception("Invalid value of \"validFrom\", make sure it is in format \"yyyy-MM-dd HH:mm:ss\" and is a valid datetime.");
					}
					
					try {
						tValidUntil = Timestamp.valueOf(validUntil);
					} catch (Exception ex) {
						throw new Exception("Invalid value of \"validUntil\", make sure it is in format \"yyyy-MM-dd HH:mm:ss\" and is a valid datetime.");
					}
					
					if ("".length() != 0) {
						System.out.println(tValidFrom + "" + tValidUntil);
					}
					
					

					JsonElement daily = getElement(e, "daily");
					JsonElement weekly = getElement(e, "weekly");
					JsonElement monthly = getElement(e, "monthly");
					ValidateSchedule.validate_daily_weekly_monthly(daily, weekly, monthly);
					
					plan2 = ValidateSchedule.plan2;
					
					System.out.println("name: " + name);
					System.out.println("descrption: " + descrption);
					System.out.println("source: " + source);
					System.out.println("target: " + target);
					System.out.println("pgp: " + pgp);
					System.out.println("config: " + config);
					System.out.println("validFrom: " + validFrom);
					System.out.println("validUntil: " + validUntil);
					System.out.println("plan: " + plan2);
					
					// some variables need to be final for whatever reasons
					String plan = plan2;
					Timestamp ftValidFrom = tValidFrom;
					Timestamp ftValidUntil = tValidUntil;
					
					return ZObject.execute(new ZObject() {
						@Override
						public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
							java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
							Map<String, Object> mh = new HashMap<String, Object>();
							if (workspace != null) { mh.put("workspace", workspace); }
							if (name != null) { mh.put("name", name); }
							if (description != null) { mh.put("description", description); }
							if (source != null) { mh.put("source", source); }
							if (target != null) { mh.put("target", target); }
							if (pgp != null) { mh.put("pgp", pgp); }
							if (config != null) { mh.put("config", config); }
							if (plan != null) { mh.put("plan", plan); }
							mh.put("validFrom", ftValidFrom);
							mh.put("validUntil", ftValidUntil);

							Set<String> keys = mh.keySet();
							String skeys = "";
							String svalues = "";
							for (String key : keys) {
								skeys += key;
								svalues += "'" + mh.get(key) + "'";
								skeys += ", ";
								svalues += ", ";
							}
							String sql = "insert into " + Constant.SCHEMA + ".T_SCH ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning name";
							System.out.println(sql);
							resultSet = statement.executeQuery(sql);
							String rName = "";
							while (resultSet.next()) {
								String name = resultSet.getString("name");
								rName += name;
							}
							ZResult ret = new ZResult();
							HashMap<String, Object> m1 = new HashMap<String, Object>();
							m1.put("name", rName);
							ret.content = ZConnector.ConvertToJsonString(m1);
							ret.statusCode = 201;
							return ret;
						}
					});
					
				} catch (Exception e) {
					e.printStackTrace();

					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("error", "" + e);
					ret.content = ZConnector.ConvertToJsonString(m1);
					ret.statusCode = 400;
					return ret;
				}
				// ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- // validate schedule end
			}
			
			// TODO
			if (match(path, method, "/workspaces/*/schedules/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_SCH where workspace = '" + workspace + "'";
						resultSet = statement.executeQuery(sql);
						try {
							ZResult ret = new ZResult();
							ret.content = ZConnector.ConvertToJsonString(schedule_remove_plan_field(resultSet).get(0));
							ret.statusCode = 200;
							return ret;
						} catch (Exception ex) {
							Map<String, Object> m2 = new HashMap<String, Object>();
							m2.put("error", ex + "");
							ZResult ret = new ZResult();
							ret.content = ZConnector.ConvertToJsonString(m2);
							ret.statusCode = 500;
							return ret;
						}
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/schedules/*, patch")) {
				// TODO
				

				JsonElement e = null;
				
				try {
					e = new Gson().fromJson (new String(bodyRaw), JsonElement.class);
				} catch (Exception ex) {
					throw new Exception("Unable to parse request body as a JSON.");
				}

				// TODO, validate [daily, weekly, monthly]
				JsonElement daily = ZAPI.getElement(e, "daily");
				JsonElement weekly = ZAPI.getElement(e, "weekly");
				JsonElement monthly = ZAPI.getElement(e, "monthly");
				
				String plan = null;
				if (daily == null && weekly == null && monthly == null) {
					
				} else {
					ValidateSchedule.validate_daily_weekly_monthly(daily, weekly, monthly);
					plan = ValidateSchedule.plan2;
				}
				
				
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);

				String description = getFromJsonBodyAsString(bodyRaw, "description");
				String source = getFromJsonBodyAsString(bodyRaw, "source");
				String target = getFromJsonBodyAsString(bodyRaw, "target");
				String config = getFromJsonBodyAsString(bodyRaw, "config");
				String pgp = getFromJsonBodyAsString(bodyRaw, "pgp");
				String validFrom = getFromJsonBodyAsString(bodyRaw, "validFrom");
				String validUntil = getFromJsonBodyAsString(bodyRaw, "validUntil");
				
				
				HashMap<String, Object> m1 = new HashMap<String, Object>(); 
				if (description != null) { ValidateSchedule.validate_schedule_description(description); m1.put("description", description); }
				if (source != null) { ValidateSchedule.validate_schedule_source(source); m1.put("source", source); }
				if (target != null) { ValidateSchedule.validate_schedule_target(target); m1.put("target", target); }
				if (config != null) { ValidateSchedule.validate_schedule_config(config); m1.put("config", config); }
				if (pgp != null) { ValidateSchedule.validate_schedule_pgp(pgp); m1.put("pgp", pgp); }
				if (validFrom != null) { ValidateSchedule.validate_schedule_validFrom(validFrom); m1.put("validFrom", validFrom); }
				if (validUntil != null) { ValidateSchedule.validate_schedule_validUntil(validUntil); m1.put("validUntil", validUntil); }
				if (plan != null) { m1.put("plan", plan); }
				
				Set<String> ks = m1.keySet();
				for (String key : ks) {
					Object value = m1.get(key);
					System.out.println("key["+key+"] -> value["+value+"]");
				}
				
				
				
				
				
				
				
				
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
						String l = "";
						Set<String> keys = m1.keySet();
						int count = keys.size();
						int i = 0;
						for (String key : keys) {
							String fieldName = key.toLowerCase();
							if (fieldName.compareToIgnoreCase("name") == 0) {
								System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
								i++;
								continue;
							}
							Object fieldValue = m1.get(key);
							if (fieldValue == null) {
								i++;
								continue;
							}
							l += fieldName + " = " + "'" +fieldValue + "'";
							if (i == count - 1) {
							} else {
								l += ", ";
							}
							i++;
						}
						String dsffdfds = count > 0 ? ", " : "";
						String sql = "UPDATE " + Constant.SCHEMA + ".T_SCH SET " + l + dsffdfds + "modified = '" +now + "' WHERE name = '" + name + "' and workspace = '" + workspace + "'";
						int updated = statement.executeUpdate(sql);
						if (updated == 0) {
							ZResult ret = new ZResult();
							HashMap<String, Object> m1 = new HashMap<String, Object>();
							m1.put("error", "T_SCH \"" +name + "\" does not exist for [workspace = \"" + workspace + "\" and source = \"" + source + "\" and target = \"" + target + "\" and pgp = \"" + pgp + "\" and config = \"" + config + "\"].");
							ret.content = ZConnector.ConvertToJsonString(m1);
							ret.statusCode = 404;
							return ret;
						}
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("updated", updated);
						ret.content = ZConnector.ConvertToJsonString(m1);
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			// TODO
			if (match(path, method, "/workspaces/*/schedules/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "delete from " + Constant.SCHEMA + ".T_SCH where name = '" + name + "' and workspace = '" + workspace + "'";
						int deleted = statement.executeUpdate(sql);
						if (deleted == 0) {
							ZResult ret = new ZResult();
							HashMap<String, Object> m1 = new HashMap<String, Object>();
							m1.put("error", "T_SCH \"" +name + "\" does not exist for [workspace = \"" + workspace + "\"].");
							ret.content = ZConnector.ConvertToJsonString(m1);
							ret.statusCode = 404;
							return ret;
						}
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("deleted", deleted);
						ret.content = ZConnector.ConvertToJsonString(m1);
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			// TODO
			if (match(path, method, "/workspaces/*/sites, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return ZConnector.T_SIT.list(workspace);
			}
			
			if (match(path, method, "/workspaces/*/sites/*/tasks, get")) {
				
				String workspace = getFromPathParamsAsString(path, 1);
				
				String site = getFromPathParamsAsString(path, 3);
				
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_TSK where workspace = '" + workspace + "' and (source = '"+site+"' or target = '"+site+"')";
						resultSet = statement.executeQuery(sql);
						ArrayList<Object> ls1 = new ArrayList<Object>();
						while (resultSet.next()) {
							Map<String, Object> m1 = new HashMap<String, Object>();
							m1.put("workspace", resultSet.getString("workspace"));
							m1.put("source", resultSet.getString("source"));
							m1.put("target", resultSet.getString("target"));
							m1.put("pgp", resultSet.getString("pgp"));
							m1.put("config", resultSet.getString("config"));
							m1.put("id", resultSet.getInt("id"));
							m1.put("description", resultSet.getString("description"));
							m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
							m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
							m1.put("status", resultSet.getString("status"));
							ls1.add(m1);
						}
						Map<String, Object> m2 = new HashMap<String, Object>();
						m2.put("list", ls1);
						ZResult ret = new ZResult();
						ret.content = ZConnector.ConvertToJsonString(m2);
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/sites/*/tasks/*, get")) {
				
				String workspace = getFromPathParamsAsString(path, 1);
				
				String site = getFromPathParamsAsString(path, 3);
				
				String id = getFromPathParamsAsString(path, 5);
				
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_TSK where id = '" + id + "' and workspace = '" + workspace + "' and (source = '"+site+"' or target = '"+site+"')";
						resultSet = statement.executeQuery(sql);
						ArrayList<Object> ls1 = new ArrayList<Object>();
						while (resultSet.next()) {
							Map<String, Object> m1 = new HashMap<String, Object>();
							m1.put("workspace", resultSet.getString("workspace"));
							m1.put("source", resultSet.getString("source"));
							m1.put("target", resultSet.getString("target"));
							m1.put("pgp", resultSet.getString("pgp"));
							m1.put("config", resultSet.getString("config"));
							m1.put("id", resultSet.getInt("id"));
							m1.put("description", resultSet.getString("description"));
							m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
							m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
							m1.put("status", resultSet.getString("status"));
							ls1.add(m1);
						}
						
						if (ls1.size() == 0) {
							ZResult ret = new ZResult();
							HashMap<String, Object> m1 = new HashMap<String, Object>();
							m1.put("error", "T_TSK \"" +id + "\" does not exist.");
							ret.content = ZConnector.ConvertToJsonString(m1);
							ret.statusCode = 404;
							return ret;
						}
						
						ZResult ret = new ZResult();
						ret.content = ZConnector.ConvertToJsonString(ls1.get(0));
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/sites/*/tasks/*/logs, get")) {
				
				String workspace = getFromPathParamsAsString(path, 1);
				
				String id = getFromPathParamsAsString(path, 5);
				
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_LOG where workspace = '" + workspace + "' and task = '" + id + "'";
						resultSet = statement.executeQuery(sql);
						ArrayList<Object> ls1 = new ArrayList<Object>();
						while (resultSet.next()) {
							Map<String, Object> m1 = new HashMap<String, Object>();
							m1.put("workspace", resultSet.getString("workspace"));
							m1.put("task", resultSet.getInt("task"));
							m1.put("id", resultSet.getInt("id"));
							m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
							m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
							m1.put("logtype", resultSet.getString("logtype"));
							m1.put("title", resultSet.getString("title"));
							m1.put("body", resultSet.getString("body"));
							ls1.add(m1);
						}
						Map<String, Object> m2 = new HashMap<String, Object>();
						m2.put("list", ls1);
						ZResult ret = new ZResult();
						ret.content = ZConnector.ConvertToJsonString(m2);
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/sites, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromJsonBodyAsString(bodyRaw, "name");
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				String host = getFromJsonBodyAsString(bodyRaw, "host");
				int port = getFromJsonBodyAsInteger(bodyRaw, "port", -1);
				String protocol = getFromJsonBodyAsString(bodyRaw, "protocol");
				String rootfolder = getFromJsonBodyAsString(bodyRaw, "rootFolder");
				String username = getFromJsonBodyAsString(bodyRaw, "username");
				String password = getFromJsonBodyAsString(bodyRaw, "password");
				String keypath = getFromJsonBodyAsString(bodyRaw, "keyPath");
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
						Map<String, Object> mh = new HashMap<String, Object>();
						if (workspace != null) { mh.put("workspace", workspace); }
						if (name != null) { mh.put("name", name); }
						if (description != null) { mh.put("description", description); }
						if (host != null) { mh.put("host", host); }
						if (protocol != null) { mh.put("protocol", protocol); }
						if (rootfolder != null) { mh.put("rootfolder", rootfolder); }
						if (username != null) { mh.put("username", username); }
						if (password != null) { mh.put("password", password); }
						if (keypath != null) { mh.put("keypath", keypath); }
						mh.put("port", port);
						Set<String> keys = mh.keySet();
						String skeys = "";
						String svalues = "";
						for (String key : keys) {
							skeys += key;
							svalues += "'" + mh.get(key) + "'";
							skeys += ", ";
							svalues += ", ";
						}
						String sql = "insert into " + Constant.SCHEMA + ".T_SIT ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning name";
						resultSet = statement.executeQuery(sql);
						String rName = "";
						while (resultSet.next()) {
							String name = resultSet.getString("name");
							rName += name;
						}
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("name", rName);
						ret.content = ZConnector.ConvertToJsonString(m1);
						ret.statusCode = 201;
						return ret;
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/sites/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String site = getFromPathParamsAsString(path, 3);
				return ZConnector.T_SIT.get(workspace, site);
			}

			if (match(path, method, "/workspaces/*/sites/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String site = getFromPathParamsAsString(path, 3);
				
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				String host = getFromJsonBodyAsString(bodyRaw, "host");
				int port = getFromJsonBodyAsInteger(bodyRaw, "port", -1);
				String protocol = getFromJsonBodyAsString(bodyRaw, "protocol");
				String rootfolder = getFromJsonBodyAsString(bodyRaw, "rootFolder");
				String username = getFromJsonBodyAsString(bodyRaw, "username");
				String password = getFromJsonBodyAsString(bodyRaw, "password");
				String keypath = getFromJsonBodyAsString(bodyRaw, "keyPath");
				
				HashMap<String, Object> map = new HashMap<String , Object>();
				
				if( description != null ) { map.put("description", description); }
				if( host != null ) { map.put("host", host); }
				if( port > 0 ) { map.put("port", port); }
				if( protocol != null ) { map.put("protocol", protocol); }
				if( rootfolder != null ) { map.put("rootfolder", rootfolder); }
				if( username != null ) { map.put("username", username); }
				if( password != null ) { map.put("password", password); }
				if( keypath != null ) { map.put("keypath", keypath); }
				
				return ZConnector.T_SIT.update(workspace, site, map);
			}
			
			if (match(path, method, "/workspaces/*/sites/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String site = getFromPathParamsAsString(path, 3);
				return ZConnector.T_SIT.delete(workspace, site);
			}
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			if (match(path, method, "/workspaces/*/pgps, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return ZConnector.T_PGP.list(workspace);
			}
			
			if (match(path, method, "/workspaces/*/pgps, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromJsonBodyAsString(bodyRaw, "name");
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				String direction = getFromJsonBodyAsString(bodyRaw, "direction");
				String keyPath = getFromJsonBodyAsString(bodyRaw, "keyPath");
				String keyPassword = getFromJsonBodyAsString(bodyRaw, "keyPassword");
				String fnRenameTo = getFromJsonBodyAsString(bodyRaw, "fnRenameTo");
				return ZConnector.T_PGP.create(workspace, name, description, direction, keyPath, keyPassword, fnRenameTo);
			}
			
			if (match(path, method, "/workspaces/*/pgps/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String pgp = getFromPathParamsAsString(path, 3);
				return ZConnector.T_PGP.get(workspace, pgp);
			}

			if (match(path, method, "/workspaces/*/pgps/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String pgp = getFromPathParamsAsString(path, 3);

				String description = getFromJsonBodyAsString(bodyRaw, "description");
				String direction = getFromJsonBodyAsString(bodyRaw, "direction");
				String keyPath = getFromJsonBodyAsString(bodyRaw, "keyPath");
				String keyPassword = getFromJsonBodyAsString(bodyRaw, "keyPassword");
				String fnRenameTo = getFromJsonBodyAsString(bodyRaw, "fnRenameTo");
				
				HashMap<String, Object> map = new HashMap<String , Object>();
				
				if( description != null ) { map.put("description", description); }
				if( direction != null ) { map.put("direction", direction); }
				if( keyPath != null ) { map.put("keyPath", keyPath); }
				if( keyPassword != null ) { map.put("keyPassword", keyPassword); }
				if( fnRenameTo != null ) { map.put("fnRenameTo", fnRenameTo); }
				
				return ZConnector.T_PGP.update(workspace, pgp, map);
			}
			
			if (match(path, method, "/workspaces/*/pgps/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String pgp = getFromPathParamsAsString(path, 3);
				return ZConnector.T_PGP.delete(workspace, pgp);
			}
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			if (match(path, method, "/workspaces/*/configs, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return ZConnector.T_CFG.list(workspace);
			}
			
			if (match(path, method, "/workspaces/*/configs, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromJsonBodyAsString(bodyRaw, "name");
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				int replace = getFromJsonBodyAsBoolean(bodyRaw, "replace", true) ? 1 : 0;
				int deleteaftersuccess = getFromJsonBodyAsBoolean(bodyRaw, "deleteAfterSuccess", true) ? 1 : 0;
				int retrycount = getFromJsonBodyAsInteger(bodyRaw, "retryCount", 0);
				int retryintervalms = getFromJsonBodyAsInteger(bodyRaw, "retryIntervalMs", 2000);
				String fnisfilenametomove = getFromJsonBodyAsString(bodyRaw, "fnIsFileNameToMove");
				String fnrenameto = getFromJsonBodyAsString(bodyRaw, "fnRenameTo");
				String archivefolder = getFromJsonBodyAsString(bodyRaw, "archiveFolder");
				String fnarcrenameto = getFromJsonBodyAsString(bodyRaw, "fnArcRenameTo");
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
						Map<String, Object> mh = new HashMap<String, Object>();
						if (workspace != null) { mh.put("workspace", workspace); }
						if (name != null) { mh.put("name", name); }
						if (description != null) { mh.put("description", description); }
						if (fnisfilenametomove != null) { mh.put("fnisfilenametomove", fnisfilenametomove); }
						if (fnrenameto != null) { mh.put("fnrenameto", fnrenameto); }
						if (archivefolder != null) { mh.put("archivefolder", archivefolder); }
						if (fnarcrenameto != null) { mh.put("fnarcrenameto", fnarcrenameto); }
						mh.put("replace", replace);
						mh.put("deleteaftersuccess", deleteaftersuccess);
						mh.put("retrycount", retrycount);
						mh.put("retryintervalms", retryintervalms);
						Set<String> keys = mh.keySet();
						String skeys = "";
						String svalues = "";
						for (String key : keys) {
							skeys += key;
							svalues += "'" + mh.get(key) + "'";
							skeys += ", ";
							svalues += ", ";
						}
						String sql = "insert into " + Constant.SCHEMA + ".T_CFG ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning name";
						System.out.println(sql);
						resultSet = statement.executeQuery(sql);
						String rName = "";
						while (resultSet.next()) {
							String name = resultSet.getString("name");
							rName += name;
						}
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("name", rName);
						ret.content = ZConnector.ConvertToJsonString(m1);
						ret.statusCode = 201;
						return ret;
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/configs/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return ZConnector.T_CFG.get(workspace, name);
			}

			if (match(path, method, "/workspaces/*/configs/*, patch")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);

				String description = getFromJsonBodyAsString(bodyRaw, "description");
				int replace = getFromJsonBodyAsBoolean(bodyRaw, "replace", true) ? 1 : 0;
				int deleteaftersuccess = getFromJsonBodyAsBoolean(bodyRaw, "deleteAfterSuccess", true) ? 1 : 0;
				int retrycount = getFromJsonBodyAsInteger(bodyRaw, "retryCount", 0);
				int retryintervalms = getFromJsonBodyAsInteger(bodyRaw, "retryIntervalMs", 2000);
				String fnisfilenametomove = getFromJsonBodyAsString(bodyRaw, "fnIsFileNameToMove");
				String fnrenameto = getFromJsonBodyAsString(bodyRaw, "fnRenameTo");
				String archivefolder = getFromJsonBodyAsString(bodyRaw, "archiveFolder");
				String fnarcrenameto = getFromJsonBodyAsString(bodyRaw, "fnArcRenameTo");
				
				HashMap<String, Object> map = new HashMap<String , Object>();
				
				if( description != null ) { map.put("description", description); }
				if( true ) { map.put("replace", replace); }
				if( true ) { map.put("deleteaftersuccess", deleteaftersuccess); }
				if( true ) { map.put("retrycount", retrycount); }
				if( true ) { map.put("retryintervalms", retryintervalms); }
				if( fnisfilenametomove != null ) { map.put("fnisfilenametomove", fnisfilenametomove); }
				if( fnrenameto != null ) { map.put("fnrenameto", fnrenameto); }
				if( archivefolder != null ) { map.put("archivefolder", archivefolder); }
				if( fnarcrenameto != null ) { map.put("fnarcrenameto", fnarcrenameto); }
				
				return ZConnector.T_CFG.update(workspace, name, map);
			}
			
			if (match(path, method, "/workspaces/*/configs/*, delete")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String name = getFromPathParamsAsString(path, 3);
				return ZConnector.T_CFG.delete(workspace, name);
			}
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			if (match(path, method, "/workspaces/*/tasks, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_TSK where workspace = '" + workspace + "'";
						resultSet = statement.executeQuery(sql);
						ArrayList<Object> ls1 = new ArrayList<Object>();
						while (resultSet.next()) {
							Map<String, Object> m1 = new HashMap<String, Object>();
							m1.put("workspace", resultSet.getString("workspace"));
							m1.put("source", resultSet.getString("source"));
							m1.put("target", resultSet.getString("target"));
							m1.put("pgp", resultSet.getString("pgp"));
							m1.put("config", resultSet.getString("config"));
							m1.put("id", resultSet.getInt("id"));
							m1.put("description", resultSet.getString("description"));
							m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
							m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
							m1.put("status", resultSet.getString("status"));
							ls1.add(m1);
						}
						Map<String, Object> m2 = new HashMap<String, Object>();
						m2.put("list", ls1);
						ZResult ret = new ZResult();
						ret.content = ZConnector.ConvertToJsonString(m2);
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/tasks, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				String source = getFromJsonBodyAsString(bodyRaw, "source");
				String target = getFromJsonBodyAsString(bodyRaw, "target");
				String pgp = getFromJsonBodyAsString(bodyRaw, "pgp");
				String config = getFromJsonBodyAsString(bodyRaw, "config");
				return ZConnector.T_TSK.create(workspace, source, target, pgp, config, description, "pending");
			}
			
			if (match(path, method, "/workspaces/*/tasks/*/status, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String taskId = getFromPathParamsAsString(path, 3);
				return process("/workspaces/" + workspace + "/tasks/" + taskId, "patch", query, header, bodyRaw);
			}
			
			if (match(path, method, "/workspaces/*/tasks/*, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String id = getFromPathParamsAsString(path, 3);
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_TSK where id = '" + id + "' and workspace = '" + workspace + "'";
						resultSet = statement.executeQuery(sql);
						ArrayList<Object> ls1 = new ArrayList<Object>();
						while (resultSet.next()) {
							Map<String, Object> m1 = new HashMap<String, Object>();
							m1.put("workspace", resultSet.getString("workspace"));
							m1.put("source", resultSet.getString("source"));
							m1.put("target", resultSet.getString("target"));
							m1.put("pgp", resultSet.getString("pgp"));
							m1.put("config", resultSet.getString("config"));
							m1.put("id", resultSet.getInt("id"));
							m1.put("description", resultSet.getString("description"));
							m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
							m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
							m1.put("status", resultSet.getString("status"));
							ls1.add(m1);
						}
						
						if (ls1.size() == 0) {
							ZResult ret = new ZResult();
							HashMap<String, Object> m1 = new HashMap<String, Object>();
							m1.put("error", "T_TSK \"" +id + "\" does not exist.");
							ret.content = ZConnector.ConvertToJsonString(m1);
							ret.statusCode = 404;
							return ret;
						}
						
						ZResult ret = new ZResult();
						ret.content = ZConnector.ConvertToJsonString(ls1.get(0));
						ret.statusCode = 200;
						return ret;
					}
				});
			}

			if (match(path, method, "/workspaces/*/tasks/*, patch")) {
				
				String workspace = getFromPathParamsAsString(path, 1);
				int id = getFromPathParamsAsInteger(path, 3, 0);
				
				String description = getFromJsonBodyAsString(bodyRaw, "description");
				String source = getFromJsonBodyAsString(bodyRaw, "source");
				String target = getFromJsonBodyAsString(bodyRaw, "target");
				String pgp = getFromJsonBodyAsString(bodyRaw, "pgp");
				String config = getFromJsonBodyAsString(bodyRaw, "config");
				String status = getFromJsonBodyAsString(bodyRaw, "status");
				
				HashMap<String, Object> map = new HashMap<String , Object>();

				if( description != null ) { map.put("description", description); }
				if( source != null ) { map.put("source", source); }
				if( target != null ) { map.put("target", target); }
				if( pgp != null ) { map.put("pgp", pgp); }
				if( config != null ) { map.put("config", config); }
				if( status != null ) { map.put("status", status); }
				
				//System.out.println(map.size());

				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
						String l = "";
						Set<String> keys = map.keySet();
						int count = keys.size();
						int i = 0;
						for (String key : keys) {
							String fieldName = key.toLowerCase();
							if (fieldName.compareToIgnoreCase("id") == 0) {
								System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
								i++;
								continue;
							}
							Object fieldValue = map.get(key);
							if (fieldValue == null) {
								i++;
								continue;
							}
							l += fieldName + " = " + "'" +fieldValue + "'";
							if (i == count - 1) {
							} else {
								l += ", ";
							}
							i++;
						}
						String dsffdfds = count > 0 ? ", " : "";
						String sql = "UPDATE " + Constant.SCHEMA + ".T_TSK SET " + l + dsffdfds + "modified = '" +now + "' WHERE id = '" + id + "' and workspace = '" + workspace + "'";
						System.out.println(sql);
						int updated = statement.executeUpdate(sql);
						if (updated == 0) {
							ZResult ret = new ZResult();
							HashMap<String, Object> m1 = new HashMap<String, Object>();
							m1.put("error", "T_TSK \"" +id + "\" does not exist for [workspace = \"" + workspace + "\" and source = \"" + source + "\" and target = \"" + target + "\" and pgp = \"" + pgp + "\" and config = \"" + config + "\"].");
							ret.content = ZConnector.ConvertToJsonString(m1);
							ret.statusCode = 404;
							return ret;
						}
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("updated", updated);
						ret.content = ZConnector.ConvertToJsonString(m1);
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/tasks/*/logs, get")) {
				
				String workspace = getFromPathParamsAsString(path, 1);
				
				String task = getFromPathParamsAsString(path, 3);
				
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_LOG where workspace = '" + workspace + "' and task = '" + task + "'";
						resultSet = statement.executeQuery(sql);
						ArrayList<Object> ls1 = new ArrayList<Object>();
						while (resultSet.next()) {
							Map<String, Object> m1 = new HashMap<String, Object>();
							m1.put("workspace", resultSet.getString("workspace"));
							m1.put("task", resultSet.getInt("task"));
							m1.put("id", resultSet.getInt("id"));
							m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
							m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
							m1.put("logtype", resultSet.getString("logtype"));
							m1.put("title", resultSet.getString("title"));
							m1.put("body", resultSet.getString("body"));
							ls1.add(m1);
						}
						Map<String, Object> m2 = new HashMap<String, Object>();
						m2.put("list", ls1);
						ZResult ret = new ZResult();
						ret.content = ZConnector.ConvertToJsonString(m2);
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			if (match(path, method, "/workspaces/*/logs, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						String sql = "select * from " + Constant.SCHEMA + ".T_LOG where workspace = '" + workspace + "'";
						resultSet = statement.executeQuery(sql);
						ArrayList<Object> ls1 = new ArrayList<Object>();
						while (resultSet.next()) {
							Map<String, Object> m1 = new HashMap<String, Object>();
							m1.put("workspace", resultSet.getString("workspace"));
							m1.put("task", resultSet.getInt("task"));
							m1.put("id", resultSet.getInt("id"));
							m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
							m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
							m1.put("logtype", resultSet.getString("logtype"));
							m1.put("title", resultSet.getString("title"));
							m1.put("body", resultSet.getString("body"));
							ls1.add(m1);
						}
						Map<String, Object> m2 = new HashMap<String, Object>();
						m2.put("list", ls1);
						ZResult ret = new ZResult();
						ret.content = ZConnector.ConvertToJsonString(m2);
						ret.statusCode = 200;
						return ret;
					}
				});
			}
			
			if (match(path, method, "/workspaces/*/logs, post")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String logtype = getFromJsonBodyAsString(bodyRaw, "logType");
				String title = getFromJsonBodyAsString(bodyRaw, "title");
				String body = getFromJsonBodyAsString(bodyRaw, "body");
				long task = getFromJsonBodyAsLong(bodyRaw, "task", 0);
				return ZObject.execute(new ZObject() {
					@Override
					public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
						java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
						Map<String, Object> mh = new HashMap<String, Object>();
						if (workspace != null) { mh.put("workspace", workspace); }
						if (logtype != null) { mh.put("logtype", logtype); }
						if (title != null) { mh.put("title", title); }
						if (body != null) { mh.put("body", body); }
						if ( true ) { mh.put("task", task); }
						Set<String> keys = mh.keySet();
						String skeys = "";
						String svalues = "";
						for (String key : keys) {
							skeys += key;
							svalues += "'" + mh.get(key) + "'";
							skeys += ", ";
							svalues += ", ";
						}
						String sql = "insert into " + Constant.SCHEMA + ".T_LOG ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning id";
						resultSet = statement.executeQuery(sql);
						String rName = "";
						while (resultSet.next()) {
							String id = resultSet.getString("id");
							rName += id;
						}
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("id", Long.parseLong(rName));
						ret.content = ZConnector.ConvertToJsonString(m1);
						ret.statusCode = 201;
						return ret;
					}
				});
			}
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			if (match(path, method, "/workspaces/*/sites/*/status, get")) {
				String workspace = getFromPathParamsAsString(path, 1);
				String siteName = getFromPathParamsAsString(path, 3);
				return ZWorker.readSiteStatus(workspace, siteName);
			}
			
			// ------------------------------------------------------------------------------------------------------------------------ //
			
			// ------------------------------------------------------------------------------------------------------------------------ //
		} catch (Exception ex) {
			return ZResult.ERROR_500(ex);
		}
		return ZResult.ERROR_501(method, path);
	}
}