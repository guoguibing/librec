// Copyright (C) 2014 Guibing Guo
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

package librec.main;

import happy.coding.io.Strings;
import happy.coding.system.Systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A demo created for the UMAP'15 demo session, could be useful for other users.
 * 
 * @author Guo Guibing
 *
 */
public class Demo {

	public static void main(String[] args) {
		try {
			new Demo().execute(args);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void execute(String[] args) throws Exception {
		LibRec librec = new LibRec();

		String configFile = "librec.conf";
		List<String> candOptions = new ArrayList<>();
		candOptions.add("Section 1: Baselines");
		candOptions.add("10: Global Average.");
 
		int option = 0;
		Scanner reader = new Scanner(System.in);
		do {
			println(Strings.toSection(candOptions));
			print("Please choose your command id: ");
			option = reader.nextInt();

			// clear console
			Systems.clearConsole();
			println("Your choice is " + option);
			
			// run algorithm

		} while (option != 0);
		reader.close();

		println("Thanks for trying out LibRec! See you again!");
	}

	private void println(String msg) {
		System.out.println(msg);
	}

	private void print(String msg) {
		System.out.print(msg);
	}

}
