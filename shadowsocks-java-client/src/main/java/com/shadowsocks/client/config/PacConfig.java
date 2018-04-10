package com.shadowsocks.client.config;

import java.util.ArrayList;
import java.util.List;

public class PacConfig {

	// PAC优先直连模式下，使用代理的域名
	public static List<String> proxyDomains = new ArrayList<>();

	// PAC优先代理模式下，使用直连的域名
	public static List<String> directDomains = new ArrayList<>();

}