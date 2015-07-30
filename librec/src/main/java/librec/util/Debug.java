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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;

/**
 * Define constants or methods often used during the debug process
 * 
 * @author guoguibing
 *
 */
public class Debug
{
	public final static boolean	ON		= true;
	public final static boolean	OFF		= false;

	public final static String	dirPath	= Systems.getDesktop();

	/**
	 * Calculate the amount of memory used by calling obj's method
	 * 
	 * @param obj
	 *            the instance of caller
	 * @param method
	 *            the method to be called
	 * @param args
	 *            the arguments to be passed for this method
	 * 
	 * @return How much memory is used by calling obj's method (in KByte)
	 * @throws Exception
	 * 
	 */
	public static double memory(Object obj, Method method, Object... args) throws Exception
	{
		double mem = 0.0;
		Runtime runtime = Runtime.getRuntime();
		double start, end;

		start = runtime.freeMemory();
		method.invoke(obj, args);
		end = runtime.freeMemory();

		mem = end - start;
		mem /= 1000.0;

		return mem;
	}

	public static void pipeErrors()
	{
		pipeErrors(dirPath + "errors.txt");
	}

	/**
	 * Redirect system errors into a file
	 * 
	 */
	public static void pipeErrors(String filePath)
	{
		try
		{
			System.setErr(new PrintStream(new File(filePath)));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void stopHere()
	{
		try
		{
			System.in.read();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void pipeConsoles()
	{
		pipeConsoles(dirPath + "console.txt");
	}

	/**
	 * Redirect system outputs into a file
	 * 
	 */
	public static void pipeConsoles(String filePath)
	{
		try
		{
			System.setOut(new PrintStream(new File(filePath)));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
