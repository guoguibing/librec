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

package net.librec.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Guo Guibing
 */
public class Systems {

    private static final Log LOG = LogFactory.getLog(Systems.class);

    private static String desktopPath = null;

    public final static String FILE_SEPARATOR = System.getProperty("file.separator");
    public final static String USER_NAME = System.getProperty("user.name");
    public final static String USER_DIRECTORY = System.getProperty("user.home");
    public final static String WORKING_DIRECTORY = System.getProperty("user.dir");
    public final static String OPERATING_SYSTEM = System.getProperty("os.name");

    public enum OS {
        Windows, Linux, Mac
    }

    private static OS os = null;

    /**
     * @return path to the desktop with a file separator in the end
     */
    public static String getDesktop() {
        if (desktopPath == null)
            desktopPath = USER_DIRECTORY + FILE_SEPARATOR + "Desktop" + FILE_SEPARATOR;
        return desktopPath;
    }

    /**
     * Get IP of the System.
     *
     * @return IP of the System
     */
    public static String getIP() {
        InetAddress ip = null;
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ip.getHostName() + "@" + ip.getHostAddress();
    }

    /**
     * Get OS type of the System.
     *
     * @return OS type of the System
     */
    public static OS getOs() {
        if (os == null) {
            for (OS m : OS.values()) {
                if (OPERATING_SYSTEM.toLowerCase().contains(m.name().toLowerCase())) {
                    os = m;
                    break;
                }
            }
        }
        return os;
    }

    /**
     * Pause the system.
     */
    public static void pause() {
        try {
            LOG.debug("System paused, press [enter] to continue ...");
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture screen the with the name screenshot.png
     *
     * @throws Exception if error occurs
     */
    public static void captureScreen() throws Exception {
        captureScreen("screenshot.png");
    }

    /**
     * Capture screen with the input string as file name
     *
     * @param fileName  a given file name
     * @throws Exception if error occurs
     */
    public static void captureScreen(String fileName) throws Exception {

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle screenRectangle = new Rectangle(screenSize);
        Robot robot = new Robot();
        BufferedImage image = robot.createScreenCapture(screenRectangle);

        File file = new File(fileName);
        ImageIO.write(image, "png", file);

        LOG.debug("A screenshot is captured to: " + file.getPath());
    }

}
