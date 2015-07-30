// Copyright (C) 2014-2015 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;

public class URLReader
{
	public static String read(String url) throws Exception
	{
		URL link = new URL(url);
		StringBuilder sb = new StringBuilder();

		BufferedReader br = new BufferedReader(new InputStreamReader(link.openStream()));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			sb.append(line);
			sb.append("\r\n");
		}

		br.close();

		return sb.toString();
	}

	public static String read(String url, String proxyHost, int proxyPort) throws Exception
	{
		SocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
		Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);

		URL link = new URL(url);
		URLConnection conn = link.openConnection(proxy);
		conn.setConnectTimeout(10 * 1000);

		StringBuilder sb = new StringBuilder();

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			sb.append(line);
			sb.append("\r\n");
		}

		br.close();

		return sb.toString();
	}
	
	public static String read(String url, Proxy proxy) throws Exception
	{
		URL link = new URL(url);
		URLConnection conn = link.openConnection(proxy);
		conn.setConnectTimeout(10 * 1000);
		
		StringBuilder sb = new StringBuilder();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			sb.append(line);
			sb.append("\r\n");
		}
		
		br.close();
		
		return sb.toString();
	}

}
