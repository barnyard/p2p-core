//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli.commands;

import java.io.PrintStream;

public class ExitCommand extends CommandBase {
	public ExitCommand() {
	}

	public void execute(PrintStream outputStream) {
		outputStream.println("Koala says: 'Fair dinkum mate'\n");
		outputStream.println("                  ");
		outputStream.println("           *    ");
		outputStream.println("          /_\\   ");
		outputStream.println("        {~._.~} ");
		outputStream.println("         ( Y )  ");
		outputStream.println("        ()~*~() ");
		outputStream.println("        (_)-(_) ");
		outputStream.println("                ");

		// big bear waving.
		/*
		 * + "                                   ,8888b\n" +
		 * "                       ________    P __ 8b\n" +
		 * "                 __d888888888888888 8\"\"8 b\n" +
		 * "               d88P\"\"\"\".      \"\"\"\"\"888 9 P\n" +
		 * "            w88P'        '._         \"88b\n" +
		 * "          d8P           .-(_)-.w.       \"8b.\n" +
		 * "        d8P          w /   `,  \'8        \"88  ,88.\n" +
		 * "      _d8P         .8P(     | ,             \"8 8  8888b\n" +
		 * "  _d888            \"'   /-___-'`             88  8    8b\n" +
		 * " d8P                                          888      8b\n" +
		 * " 8P d8                       .                88        8:\n" +
		 * " 8b \"8                        :               88        8:\n" +
		 * "  8bw                         :              :8P8      8P\n" +
		 * "    \"8b                       :             ,8P  888888P\n" +
		 * "     \"8b                      ;           ,88P      d|\n" +
		 * "      88b                               ,88P       dP\n" +
		 * "     dP\"88b                          ,888P 88b    dP\n" +
		 * "    dP   \"88b____________________www88P      88bdP'\n" +
		 * "   dP       \"88888888888888888888P''           88b\n" +
		 * " d88n        d8                    .            \"8b\n" +
		 * "dP          d8                      .             8b\n" +
		 * "88         8b.                       :             8b\n" +
		 * "88           88                       :            \"8;\n" +
		 * " 8b       8b 88                       :             88\n" +
		 * "  8b       88P                        :             88\n" +
		 * "   \"888888P'                          .             d8\n" +
		 * "       8b                            :             d8'\n" +
		 * "       ,8b                                      _d8P\n" +
		 * "       8P888b                               __d88P\n" +
		 * "       88 \"8888b                        __d88P\"8b\n" +
		 * "       88      \"888888______________wd888P\"      8b\n" +
		 * "      8P            \"\"8888888888888P\"\"            88\n" +
		 * "     8P                                            88\n" +
		 * "    8P                     w_                      89\n" +
		 * "   88                       \"b  w88                89\n" +
		 * "   8b                         8b\"                  8P\n" +
		 * "   \"8b                         8b                 dP\n" +
		 * "    \"88b                       8P               .dP\n" +
		 * "      \"8888b________________d888w_           ..dP'\n" +
		 * "         \"\"\"\"888888888888888P    \"888888888888P\n")
		 */

		// party koala
	}

	public String getDescription() {
		return "Exits this tool";
	}

	public String getKeyword() {
		return "exit";
	}

}
