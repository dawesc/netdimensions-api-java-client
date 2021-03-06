package com.netdimensions.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;

public final class SystemClient {
	private final String baseUrl;
	private final Credentials credentials;

	public SystemClient(final String baseUrl, final String adminUserId, final String key) {
		this.baseUrl = baseUrl;
		this.credentials = Credentials.basic(adminUserId, key);
	}

	public static void main(final String[] args) throws IOException {
		final String baseUrl = args[0];
		final String adminUserId = args[1];
		final String key = args[2];
		final String userId = args[3];
		final String password = args[4];
		final String familyName = args[5];
		final String givenName = args[6];
		final Status result = new SystemClient(baseUrl, adminUserId, key).send(SystemRequest.createUser(userId, UserField.password(password),
				UserField.familyName(familyName), UserField.givenName(givenName), UserField.status(UserStatus.ACTIVE)));
		switch (result) {
		case SUCCEEDED:
			System.out.println("Successfully created user");
			break;
		case FAILED:
			System.out.println("Couldn't create user");
			break;
		case UNKNOWN:
			System.out.println("Couldn't determine whether user was created (Talent Suite version is 10.1 or earlier");
			break;
		}
	}

	public final Status send(final SystemRequest req) throws IOException {
		final URLConnection con = new URL(baseUrl + "contentHandler/usersCsv").openConnection();
		con.setDoOutput(true);
		con.setRequestProperty("Authorization", credentials.toString());
		con.setRequestProperty("Content-Type", MediaType.CSV_UTF_8.toString());
		try (final OutputStream out = con.getOutputStream()) {
			out.write(req.user.toCsvString(req.action).getBytes(Charsets.UTF_8));
			out.flush();
		}
		try (final InputStream in = con.getInputStream()) {
			final List<String> lines = CharStreams.readLines(new InputStreamReader(in, Charsets.UTF_8));
			return lines.isEmpty() ? Status.UNKNOWN : lines.get(lines.size() - 1).startsWith("\"") ? Status.FAILED : Status.SUCCEEDED;
		}
	}

	/**
	 * @deprecated Use {@link #send(SystemRequest)} with {@link SystemRequest#updateUser(String, String)} instead.
	 */
	@Deprecated
	public final void updateUser(final String userId, final String password) throws IOException {
		send(SystemRequest.updateUser(new UserRecord(userId, UserField.password(password))));
	}
}
