package com.shadowsocks.client.utils;

import com.shadowsocks.client.config.Server;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 网络工具
 * <p>
 * Internet util
 *
 * @since v0.0.2
 */
public class NetUtils {

  private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);

  /**
   * 检测服务器是否可用
   * <p>
   * test server is available
   *
   * @param server 服务器实例
   */
  public static boolean isConnected(Server server) {
    boolean isAvailable = true;
    TelnetClient client = new TelnetClient();
    try {
      client.setDefaultTimeout(3000);
      client.connect(server.getHost(), server.getPort());
      client.disconnect();
    } catch (Exception e) {
      logger.warn("remote server: " + server.toString() + " connected failed");
      isAvailable = false;
    }
    server.setAvailable(isAvailable);
    return isAvailable;
  }

  /**
   * 获取服务PING指令所消耗的时间
   * <p>
   * To get the average time of ping operation
   *
   * @param server 服务器实例
   * @return ping time
   */
  public static Double avgPingTime(Server server) {
    boolean isAvailable = server.isAvailable();
    if (!isAvailable) {
      return 999999.9;
    }

    ArrayList<Double> timeList = new ArrayList<>();
    try {
      Runtime r = Runtime.getRuntime();
      Process p = r.exec("ping " + server.getHost());
      BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String inputLine;
      int cnt = 0;
      while ((inputLine = in.readLine()) != null) {
        int startIndex = inputLine.indexOf("time=");
        int endIndex = inputLine.indexOf(" ms");
        if (startIndex < 0 || endIndex < 0) {
          continue;
        }
        String time = inputLine.substring(startIndex + "time=".length(), endIndex);
        timeList.add(Double.valueOf(time));
        if (++cnt > 8) {
          break;
        }
      }
      in.close();
      Thread.sleep(100);
    } catch (Exception e) {
      logger.warn("remote server: " + server.toString() + " telnet failed");
      return 999999.9;
    }

    Double avg = timeList.parallelStream().collect(Collectors.averagingDouble(p -> p));
    server.setPingTime(avg);
    return avg;
  }

  public static void main(String[] args) {
    String inputLine = "64 bytes from 107.175.23.142: icmp_seq=4 ttl=52 time=288.452 ms";
    int startIndex = inputLine.indexOf("time=");
    int endIndex = inputLine.indexOf(" ms");
    String time = inputLine.substring(startIndex + "time=".length(), endIndex);
    System.out.println(time);
  }

}
