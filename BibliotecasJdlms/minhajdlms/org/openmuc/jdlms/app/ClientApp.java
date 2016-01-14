/*
 * Copyright 2012-15 Fraunhofer ISE
 *
 * This file is part of jDLMS.
 * For more information visit http://www.openmuc.org
 *
 * jDLMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jDLMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jDLMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jdlms.app;

import static java.lang.System.exit;
import static org.openmuc.jdlms.app.ClientApp.Action.PRINT_HELP;
import static org.openmuc.jdlms.app.ClientApp.Action.QUIT;
import static org.openmuc.jdlms.app.ClientApp.Action.READ;
import static org.openmuc.jdlms.app.ClientApp.Action.SCAN;
import static org.openmuc.jdlms.app.ClientApp.Action.WRITE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class ClientApp {
	
	private static final String LINES = "------------------------------------------------------";

	private static ConsoleApp consoleApp;

	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (consoleApp != null) {
					consoleApp.close();
				}
			}
		});
	}

	public static void main(String[] args) throws Throwable {

		ConsoleLineParser clientLineParser;
		try {
			clientLineParser = new ConsoleLineParser(args);
			consoleApp = clientLineParser.createConsoleApp();
			processActions();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			// ConsoleLineParser.printUsage();
			System.exit(3);
		}

	}

	private static void processActions() {
		printHelp();

		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		try {

			String actionStr;
			while (true) {
				System.out.println("\n** Enter actionkey: ");

				try {
					actionStr = inputReader.readLine();
				} catch (IOException e) {
					System.err.printf("%s. Application is being shut down.\n", e.getMessage());
					exit(2);
					return;
				}

				Action action;
				try {
					action = Action.actionFor(actionStr);
				} catch (IllegalArgumentException e) {
					System.err.println("Illegal actionkey.\n");

					printHelp();
					continue;
				}

				switch (action) {
				case READ:
					consoleApp.processRead();
					break;

				case WRITE:
					consoleApp.processWrite();
					break;

				case SCAN:
					consoleApp.processScan();
					break;

				case QUIT:
					System.out.println("** Closing connection.");
					return;

				case PRINT_HELP:
				default:
					printHelp();
					break;
				}

			}

		} catch (IOException e) {
			System.err.println("Connection closed for the following reason: " + e.getMessage());
			e.printStackTrace(); // TODO: remove this.
			return;
		} finally {
			consoleApp.close();
		}
	}

	private static void printHelp() {
		String message = " %s - to %s";
		System.out.flush();
		System.out.println();
		System.out.println(LINES);
		System.out.printf(message, QUIT.key(), "quit the connection\n");
		System.out.println(LINES);

		System.out.printf(message, READ.key(), "read\n");
		System.out.printf(message, WRITE.key(), "write\n");
		System.out.printf(message, SCAN.key(), "scan\n");

		System.out.println(LINES);
		System.out.printf(message, PRINT_HELP.key(), "for help\n");
		System.out.println(LINES);

	}

	enum Action {
		READ("1"),
		WRITE("2"),
		SCAN("3"),

		PRINT_HELP("?"),
		QUIT("q");

		private final String key;

		private Action(String key) {
			this.key = key;
		}

		public String key() {
			return this.key;
		}

		public static Action actionFor(String key) throws IllegalArgumentException {
			Action[] actions = Action.values();

			for (Action action : actions) {
				if (action.key.equals(key)) {
					return action;
				}
			}
			throw new IllegalArgumentException("Wrong key!");
		}
	}

	/**
	 * Don't let anyone instanciate this class.
	 */
	private ClientApp() {
	}
}
