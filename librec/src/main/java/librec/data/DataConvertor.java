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

package librec.data;

import happy.coding.io.FileIO;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A data convertor class to convert a data file from one source format to a target format (i.e., our supporting format)
 * of {@code UserId ItemId Rating}, separted by " \t,"<br>
 * 
 * @author guoguibing
 * 
 */
public class DataConvertor {

	// pathes to source, target files
	private String sourcePath, targetPath;

	/**
	 * @param sourcePath
	 *            path to the source file
	 * @param targetPath
	 *            path to the target file
	 */
	public DataConvertor(String sourcePath, String targetPath) throws Exception {
		this.sourcePath = sourcePath;
		this.targetPath = targetPath;

		// clean the target file
		FileIO.deleteFile(targetPath);
	}

	/**
	 * Convert a data file separated by {@code sep} to a data file separated by {@code toSep}
	 * 
	 * @param sep
	 *            separator of the source file
	 * @param toSep
	 *            separtor of the target file
	 */
	public void cvtSeparator(String sep, String toSep) throws Exception {
		BufferedReader br = FileIO.getReader(sourcePath);

		String line = null;
		List<String> lines = new ArrayList<>();
		while ((line = br.readLine()) != null) {
			String newline = line.replaceAll(sep, toSep);

			lines.add(newline);

			if (lines.size() >= 1000) {
				FileIO.writeList(targetPath, lines, true);
				lines.clear();
			}
		}

		if (lines.size() > 0)
			FileIO.writeList(targetPath, lines, true);

		br.close();
	}

	/**
	 * <p>
	 * Source File Format:<br>
	 * First Line: {@code UserID Sep #Ratings}<br>
	 * Other Lines: {@code ItemID Sep2 Rating}<br>
	 * </p>
	 * 
	 * @param sep1
	 *            the separtor of the first line
	 * @param sep2
	 *            the separtor of the other lines
	 * 
	 */
	public void cvtFirstLines(String sep1, String sep2) throws Exception {
		BufferedReader br = FileIO.getReader(sourcePath);

		String line = null, userId = null;
		List<String> lines = new ArrayList<>();
		while ((line = br.readLine()) != null) {

			String[] vals = line.split(sep1);

			if (vals.length > 1) {
				// first line
				userId = line.split(sep1)[0];
			} else if ((vals = line.split(sep2)).length > 1) {
				// other lines: for the train data set
				String newLine = userId + " " + vals[0] + " " + vals[1];
				lines.add(newLine);
			} else {
				// other lines: for the test data set
				String newLine = userId + " " + vals[0];
				lines.add(newLine);
			}

			if (lines.size() >= 1000) {
				FileIO.writeList(targetPath, lines, true);
				lines.clear();
			}
		}

		if (lines.size() > 0)
			FileIO.writeList(targetPath, lines, true);

		br.close();
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

}
