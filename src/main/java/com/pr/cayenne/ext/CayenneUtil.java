package com.pr.cayenne.ext;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.commons.lang.StringUtils;

/**
 * Cayenee的工具类
 * 
 * @author Bailey
 */
public abstract class CayenneUtil {

	private static ServerRuntime serverRuntime;
	private static ObjectContext context;

	protected static String mapFileName = "cayenne-project.xml";

	protected CayenneUtil() {
	}

	/**
	 * 设置cayenne主配置文件, 默认"cayenne-project.xml"
	 */
	public static void setMapFileName(String mapFileName) {
		CayenneUtil.mapFileName = mapFileName;
	}

	public static ServerRuntime getRuntime() {
		if (serverRuntime == null) {
			if (StringUtils.isBlank(mapFileName)){
				throw new RuntimeException("未配置CayenneMapFile");
			}
			serverRuntime = new ServerRuntime(mapFileName);
		}
		return serverRuntime;
	}

	public static ObjectContext getContext() {
		if (context == null) {
			context = getRuntime().getContext();
		}
		return context;
	}
}
