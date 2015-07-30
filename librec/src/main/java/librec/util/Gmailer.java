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

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

public class Gmailer extends EMailer {

	public Gmailer() {
		configSSL();
		defaultInstance();
	}

	public void configTLS() {
		props.put("mail.smtp.starttls.enable", "true");

		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.smtp.host", "smtp.gmail.com");
		props.setProperty("mail.smtp.port", "587");
		props.setProperty("mail.smtp.auth", "true");
	}

	public void configSSL() {
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
	}

	private void defaultInstance() {
		props.setProperty("mail.debug", "false");

		final String userName = "happycodingprojects@gmail.com";
		final String password = "dailycoding@ntu";
		props.setProperty("mail.smtp.user", userName);
		props.setProperty("mail.smtp.password", password);

		props.setProperty("mail.from", userName);
		props.setProperty("mail.to", "gguo1@e.ntu.edu.sg");

		props.setProperty("mail.subject", "Program Notifier from Gmail");
		props.setProperty("mail.text", "Program was finished @" + Dates.now());

		Session.getDefaultInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, password);
			}
		});
	}

	public static void main(String[] args) throws Exception {
		new Gmailer().send("Your program has been finished");
	}
}
