/*
 * Braille Utils (C) 2010-2011 Daisy Consortium 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.daisy.dotify.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.daisy.braille.utils.api.validator.ValidatorFactory;
import org.daisy.braille.utils.pef.PEFFileSplitter;
import org.daisy.streamline.cli.AbstractUI;
import org.daisy.streamline.cli.Argument;
import org.daisy.streamline.cli.ExitCode;
import org.daisy.streamline.cli.OptionalArgument;

/**
 * Provides a UI for splitting a PEF-file. Not for public use. 
 * This class is a package class. Use DotifyCLI
 * @author Joel Håkansson
 */
class SplitPEF extends AbstractUI {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SplitPEF ui = new SplitPEF();
		if (args.length!=2) {
			System.out.println("Expected two arguments.");
			System.out.println();
			ui.displayHelp(System.out);
			ExitCode.MISSING_ARGUMENT.exitSystem();
		}
		File input = new File(args[0]);
		File output = new File(args[1]);
		PEFFileSplitter splitter = new PEFFileSplitter(ValidatorFactory.newInstance());
		splitter.split(input, output);
	}

	@Override
	public String getName() {
		return DotifyCLI.SPLIT;
	}
	
	@Override
	public String getDescription() {
		return "Splits a PEF file into several files, one file per volume. The purpose is to facilitating the " +
				"use of PEF-files with braille editors that do not support multi volume files.";
	}

	@Override
	public List<Argument> getRequiredArguments() {
		ArrayList<Argument> ret = new ArrayList<Argument>();
		ret.add(new Argument("input_file", "Path to the input PEF-file"));
		ret.add(new Argument("output_directory", "Path to the output folder"));
		return ret;
	}

	@Override
	public List<OptionalArgument> getOptionalArguments() {
		return null;
	}

}
