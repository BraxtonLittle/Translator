import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 * General project notes:
 * 
 * Check for in programs: Wrong types, no variable declared, re-initializing
 * 
 * 
 */

public class Translator {

	/*
	 * Saves scanner reading from text tile, creates an output Arraylist that will
	 * later be written to a file, and calls translate helper function.
	 */
	public static void main(String[] args) {
		Scanner sc = readFileFromCommandLine(args);
		List<String> output = new ArrayList<String>();
		if (sc != null) {
			Map<String, String[]> symbolTable = new HashMap<>();
			processCmdLineArgs(args, symbolTable);
			try {
				translate(0, sc, symbolTable, output, 1,args);
				replaceCmdLineArgs(output,args);
				// Once we've read through the entire file, write output
				writeOutput(output, args);
			} catch (ParseException e) {
				System.out.println(e.getMessage());
			}
			sc.close();
		}
	}
	
	public static void replaceCmdLineArgs(List<String> output,String[] args) {
		for(int i = 0; i<output.size(); i++) {
			String line = output.get(i);
			if(line.contains("$arg")) {
				String modifiedLine = line.substring(line.indexOf("$arg")+4, line.indexOf("$arg")+5);
				int argIndex = Integer.parseInt(modifiedLine.replace(";", "").strip());
				String tmp =  '"' + args[argIndex] + '"';
				String formattedLine = line.replace("$arg" + argIndex, tmp);
				output.set(i, formattedLine);
			}
		}
	}
	
	public static void processCmdLineArgs(String[] args, Map<String, String[]> table) {
		if(args.length>2) {
			Pattern pattern;
			Matcher matcher;
			for(int i = 2; i<args.length; i++) {
				String argType = "String";
				pattern = Pattern.compile("-?[0-9]+");
				matcher = pattern.matcher(args[i]);
				if (matcher.find()) {
					argType = "int";
					String key = "$arg" + (i-2);
					
				}
				String[] mappedContents = {argType, "const"};
				String key = "$arg" + (i-2);
				table.put(key, mappedContents);
			}
		}
	}

	public static void translateDeclarationStmt(String line, Integer tabCount, List<String> output,
			Map<String, String[]> symbolTable,String[] args) throws ParseException {
		System.out.println("Translating declaration statement...");
		String varName = line.substring(line.indexOf(" ")).replaceAll(";", "");
		String scope = line.substring(0, line.indexOf(" "));
		if (varName.contains("=")) {
			// Get everything up to equals sign if initializing variable to get variable
			// name
			varName = varName.substring(0, varName.indexOf("="));
		} else {
			// Remove semicolon if just declaring variable to get variable name
			varName.replaceAll(";", "");
		}
		varName = varName.strip();
		String rightSide = line.substring(line.indexOf("="));

		// If re-declaring a variable in the same scope, throw error
		if (symbolTable.containsKey(varName + tabCount)) {
			throw new ParseException("ERROR: A variable with that name already exists in this scope", 0);
		}

		// If declaring and initializing, store type in symbol table, otherwise if just
		// declaring variable then store its type as null in symbol table
		String returnType;
		if (line.contains("=")) {
			String returnedValue = line.substring(line.indexOf("=") + 1).replaceAll(";", "");
			returnType = getReturnType(tabCount, returnedValue, symbolTable,args);
			String[] mapContents = { returnType, scope };
			symbolTable.put(varName + tabCount, mapContents);
		} else {
			String[] mapContents = { "null", scope };
			symbolTable.put(varName + tabCount, mapContents);
			returnType = "";
		}

		// Remove scope variable before writing to output
		// line = line.substring(line.indexOf(" ")+1);
		// line = "\t".repeat(tabCount) + line;
		if(returnType.equals("int") && rightSide.contains("$arg")) {
			String newRight = rightSide.substring(1,rightSide.length()-1);
			newRight = "= Integer.parseInt(" + newRight + ");";
			rightSide = newRight;
		}
		String transLine = ("\t".repeat(tabCount)) + returnType + " " + varName + " " + rightSide;
		output.add(transLine);
		System.out.println("declaration...DONE!");
	}

	public static void translateInitializeStmt(String line, Integer tabCount, List<String> output,
			Map<String, String[]> symbolTable, String[] args) throws ParseException {
		System.out.println("Parsing variable initalization...");
		// Check if initialized variable has been declared
		String varName = line.substring(0, line.indexOf("=")).strip();
		int variableTabCount = tabCount;
		boolean foundMatchingVariable = false;
		while (variableTabCount >= 0) {
			if (symbolTable.containsKey(varName + variableTabCount)) {
				foundMatchingVariable = true;
				break;
			}
			variableTabCount--;
		}
		if (!foundMatchingVariable) {
			throw new ParseException(
					"ERROR: Variable " + varName + " has not been declared. Use the const|let|var keywords", 0);
		}

		// Check type assignment and make sure we're not reassigning a variable to a
		// different type, unless we're assigning
		// a null variable to have an actual value
		String returnedValue = line.substring(line.indexOf("=") + 1).replaceAll(";", "");
		String newlyInitializedType = getReturnType(tabCount, returnedValue, symbolTable,args);
		String originallyInitializedType = symbolTable.get(varName + variableTabCount)[0];
		String originalScope = symbolTable.get(varName + variableTabCount)[1];
		originalScope = originalScope.replaceAll("\t", "");
		if (originalScope.equals("const")) {
			throw new ParseException("ERROR: You cannot re-assign the const variable " + varName + " to a new value!",
					0);
		}
		if (originallyInitializedType != "null" && originallyInitializedType != newlyInitializedType) {
			throw new ParseException("ERROR: You cannot assign " + varName + " to be of type " + newlyInitializedType
					+ " because it was declared as a " + symbolTable.get(varName + variableTabCount), 0);
		} else {
			String[] mapContents = { newlyInitializedType, originalScope };
			symbolTable.put(varName + variableTabCount, mapContents);
		}
		System.out.println("intitalization...DONE!");
		output.add(line);
	}

	public static void translatePrintStmt(String line, List<String> output) {
		System.out.println("Translating speak statement...");
		line = line.replace("speak", "System.out.println");
		output.add(line);
		System.out.println("speak...DONE!");
	}

	public static void translateReturnStmt(String line, List<String> output) {
		// Return statements don't need to be translated
		output.add(line);
	}

	/*
	 * This function uses the return statement to determine a function's return
	 * type, which will be used when translating the function declaration line. This
	 * checks for which primitive operators are present in the return statement in
	 * the necessary order (i.e. putting arithmetic before comparison would be wrong
	 * because "return (3+4)<9" would be considered arithmetic when its actually a
	 * comparison statement. Similarly, the expression "0"+5 needs to be recognized
	 * as a String instead of int, which is why we have String recognition before
	 * arithmetic operators. If we're not returning a primitive or string, we're
	 * returning a variable of sorts so use the return type of that variable stored
	 * in the lookup table.
	 */
	public static String getReturnType(Integer tabCount, String inputLine, Map<String, String[]> symbolTable, String[] args)
			throws ParseException {
		String variableToSearch = inputLine.strip();
		// If we're returning a function, we want to lookup only the name in our
		// symbol table without the parenthesis
		Pattern pattern = Pattern.compile("[a-zA-Z]*\\(.*\\)");
		Matcher matcher = pattern.matcher(variableToSearch);
		if (matcher.find()) {
			variableToSearch = variableToSearch.substring(0, variableToSearch.indexOf("(")).strip();
			variableToSearch = variableToSearch + "0";
			if (symbolTable.containsKey(variableToSearch)) {
				return symbolTable.get(variableToSearch)[0];
			} else {
				// Function definition wasn't stored in symbol table, meaning function
				// hasn't been defined yet
				throw new ParseException(
						"ERROR: The function " + variableToSearch + " has not been defined before being used", 0);
			}
		}
		if(variableToSearch.contains("$arg")) {
			int i = variableToSearch.length()-1;
			int j = Integer.parseInt(variableToSearch.substring(i));
			pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
			matcher = pattern.matcher(args[j]);
			if(matcher.find()) {
				return "int";
			}
			else {
				return "String";
			}
			
			
		}

		if (inputLine.contains("on") || inputLine.contains("off")) {
			return "boolean";
		}

		String[] comparisonOps = { ">", "<", ">=", "<=", "==", "OR", "NOT", "AND" };
		String[] arithmeticOps = { "+", "-", "/", "%" };
		for (String comparisonOp : comparisonOps) {
			if (inputLine.contains(comparisonOp)) {
				return "boolean";
			}
		}

		for (String arithmeticOp : arithmeticOps) {
			if (inputLine.contains(arithmeticOp)) {
				return "int";
			}
		}

		// Check if the value is an integer. "-?" searches for an optional dash to
		// handle
		// negative integers, and "\d+" searches for one or more digits
		pattern = Pattern.compile("-?[0-9]+");
		matcher = pattern.matcher(variableToSearch);
		if (matcher.find()) {
			return "int";
		}

		if (inputLine.contains("\"")) {
			return "String";
		}

		if (symbolTable.containsKey(variableToSearch + tabCount)) {
			return symbolTable.get(variableToSearch + tabCount)[0];
		}

		// If no return type found, print error
		throw new ParseException("ERROR: A return type could not be found for: " + inputLine, 0);
	}

	public static void addParamsToTable(String params, Map<String, String[]> symbolTable) {
		if (params.equals(""))
			return;
		String[] paramList = params.split(",");
		for(String param : paramList) {
			if(param.strip().length()>0) {
				System.out.println(param);
				param = param.strip();
				String[] paramContents = param.split(" ");
				String[] mappedContents = {paramContents[0], "const"};
				symbolTable.put(paramContents[1]+1, mappedContents);
			}
		}
	}
	
	public static String insertCmdLineArgs(String line) {
		String firstHalf = line.substring(0, line.indexOf("("));
		String secondHalf = line.substring(line.indexOf("(")+1);
		if(secondHalf.charAt(0)==')') {
			return firstHalf + "(String[] args" + secondHalf;
		}
		else {
			return firstHalf + "(String[] args, " + secondHalf;
		}
		
	}

	/*
	 * This function uses a subScanner to "jump ahead" in the file and determine the
	 * function's return type first so that it can correctly translate the function
	 * declaration line before translating the body normally by calling the
	 * translate() function with an increased tabCount value to indicate scope.
	 * 
	 */
	public static int translateFunction(Integer tabCount, Scanner scanner, Map<String, String[]> symbolTable,
			String line, List<String> output, int lineCount, String[] args) throws ParseException {
		int currentLineCount = lineCount;
		System.out.println("Parsing function head...");
		// Shows an error if a function exists with same name and return type, mostly
		// because
		// this isn't valid in Java but also because it'll cause a bug in our
		// implementation below
		String functionName = line.substring(line.indexOf(" "), line.indexOf("("));
		functionName = functionName.strip();
		if (symbolTable.containsKey(functionName + tabCount)) {
			// Check if return type is the same, same function names are allowed but
			// their return type has to be different. This has to be done with lineCount
			// instead of checking the subScanner's line against the main scanner
			throw new ParseException("ERROR: Duplicate function name " + functionName + " detected", 0);
		}
		String paramList = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
		addParamsToTable(paramList, symbolTable);
		System.out.println("function head...DONE!");
		functionName += tabCount;
		
		// Use the subScanner to retrieve the return statement line
		String foundLine = "";
		Matcher matcher;
		int subscannerLineCount = 1;
		Scanner subScanner;
		try {
			subScanner = new Scanner(new File(args[0]));
		} catch (FileNotFoundException e) {
			throw new ParseException("Unable to open file for using sub-scanner", 0);
		}
		while (subScanner.hasNextLine()) {
			String inputLine = subScanner.nextLine();
			if (subscannerLineCount >= currentLineCount + 1) {
				int readTabCount = countTabs(inputLine);
				// If tab counts mismatch, we've broken out of scope without finding
				// a return statement
				if (readTabCount - 1 < tabCount) {
					break;
				}
				String regex = "\t".repeat(readTabCount) + "return .*;";
				Pattern pattern = Pattern.compile(regex);
				matcher = pattern.matcher(inputLine);
				if (matcher.find()) {
					foundLine = inputLine;
					break;
				}
			}
			subscannerLineCount += 1;
		}

		currentLineCount++;
		lineCount++;
		Scanner sub;
		System.out.println("Translating function body....");
		// Add dummy function header to output to be translated after getting return
		// type
		output.add(functionName + "()");
		try {
			File input = new File(args[0]);
			sub = new Scanner(input);
			for (int i = 1; i < currentLineCount; i++) {
				sub.nextLine();
			}
			currentLineCount = translate(tabCount + 1, sub, symbolTable, output, currentLineCount, args);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), 0);
		}
		int dif = currentLineCount - lineCount;
		while (dif > 0) {
			scanner.nextLine();
			dif--;
		}
		System.out.println("function body...DONE!");
		// Interpret the return type from the return statement line, or void
		// if no return statement was found in this scope
		System.out.println("Retrieving return statment...");
		String returnType;
		if (foundLine.length() > 0) {
			foundLine = foundLine.strip().substring(foundLine.indexOf(" ")).replaceAll(";", "");
			returnType = getReturnType(tabCount + 1, foundLine, symbolTable,args);
		} else {
			// No return line, function type is void
			returnType = "void";
		}
		String[] mapContents = { returnType, "func" };
		symbolTable.put(functionName, mapContents);
		String funNameWithParams = line.substring(line.indexOf(" ") + 1, line.length() - 1);
		if(functionName.equals("primary0")) {
			funNameWithParams = insertCmdLineArgs(funNameWithParams);
		}
		for(int i = 0; i<output.size(); i++) {
			String outputLine = output.get(i);
			if (outputLine.equals(functionName + "()")) {
				output.set(i, "public static " + returnType + " " + funNameWithParams + "{");
			}
		}
		System.out.println("return statement...DONE!");

		Iterator<Map.Entry<String, String[]>> itr = symbolTable.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<String, String[]> entry = itr.next();
			if (entry.getKey().charAt(entry.getKey().length() - 1) == '1') {
				itr.remove();
			}
		}

		// Add a closing bracket to signify the end of the function, and add 2 to the
		// updated line
		// count to account for the opening and closing brackets
		output.add("}");
		return currentLineCount;
	}

	/*
	 * Translates if statements into proper java grammar.
	 */
	public static int translateConditionalStmt(Integer tabCount, Scanner scanner, String line,
			Map<String, String[]> symbolTable, List<String> output, int lineCount, String[] args)
			throws ParseException {
		int currentLineCount = lineCount;
		// Translate line parameter first and add it to output, then handle
		// nested function body by calling translate with additional tabCount
		// translate(tabCount+1, scanner, output);
		System.out.println("Parsing conditional head...");
		line = line.replace(":", "{");
		if (line.contains("elif")) {
			line = line.replace("elif", "else if");
		}
		currentLineCount++;
		lineCount++;
		output.add(line);
		System.out.println("conditional head....DONE!");
		System.out.println("Parsing conditional body...");
		try {
			File input = new File(args[0]);
			Scanner sub = new Scanner(input);
			for (int i = 1; i < currentLineCount; i++) {
				sub.nextLine();
			}
			currentLineCount = translate(tabCount + 1, sub, symbolTable, output, currentLineCount, args);
		} catch (Exception e) {
			System.out.println("Failed to read file: " + e);
		}
		System.out.println("conditional body...DONE!");
		int dif = currentLineCount - lineCount;
		while (dif > 0) {
			scanner.nextLine();
			dif--;
		}

		output.add("\t".repeat(tabCount) + "}");
		// increment currentLineCount with each line of the body of conditional and
		// return it
		// so we have an accurate line count for the translate function
		return currentLineCount;
	}

	/*
	 * Translates loops into proper java grammar
	 */
	public static int translateLoops(Integer tabCount, Scanner scanner, String line, Map<String, String[]> symbolTable,
			List<String> output, int lineCount, String[] args) throws ParseException {
		int currentLineCount = lineCount;
		// Translate line parameter first and add it to output, then handle
		// nested function body by calling translate with additional tabCount
		// translate(tabCount+1, scanner, output);
		System.out.println("Parsing loop head...");
		line = line.replace(":", "{");
		currentLineCount++;
		lineCount++;
		output.add(line);
		System.out.println("loop head...DONE!");
		System.out.println("Parsing loop body...");
		try {
			File input = new File(args[0]);
			Scanner sub = new Scanner(input);
			for (int i = 1; i < currentLineCount; i++) {
				sub.nextLine();
			}
			currentLineCount = translate(tabCount + 1, sub, symbolTable, output, currentLineCount, args);
		} catch (Exception e) {
			System.out.println("Failed to read file: " + e);
		}
		int dif = currentLineCount - lineCount;
		while (dif > 0) {
			scanner.nextLine();
			dif--;
		}
		System.out.println("loop body...DONE!");
		output.add("\t".repeat(tabCount) + "}");
		// increment currentLineCount with each line of the body of conditional and
		// return it
		// so we have an accurate line count for the translate function
		return currentLineCount;
	}

	public static int countTabs(String inputLine) {
		for (int i = 0; i < inputLine.length(); i++) {
			if (inputLine.charAt(i) != '\t') {
				return i;
			}
		}

		// Line of only tabs, basically blank space so just return 0 for tab count
		return 0;
	}

	/*
	 * Parses through each line of the input file and uses regular expressions to
	 * call the matching helper function for translation. Multi-line statements such
	 * as if statements and functions will call scanner.nextLine in their own helper
	 * methods and return here to continue parsing through the rest of the input
	 * file afterwards.
	 */
	public static int translate(Integer tabCount, Scanner scanner, Map<String, String[]> symbolTable, List<String> output,
			int lineCount, String[] args) throws ParseException {
		int currentLineCount = lineCount;
		while (scanner.hasNextLine()) {
			String inputLine = scanner.nextLine();
			
			int readTabCount = countTabs(inputLine);
			if (tabCount != readTabCount) {
				break;
			}
			// inputLine = inputLine.replaceAll("\t", "");

			// Function declaration
			Pattern pattern = Pattern.compile("fun .*(.*):");
			Matcher matcher = pattern.matcher(inputLine);
			if (matcher.find()) {
				currentLineCount = translateFunction(tabCount, scanner, symbolTable, inputLine, output,
						currentLineCount, args);
			} else {
				// Conditional statement
				pattern = Pattern.compile("(if|elif|else)(.*):");
				matcher = pattern.matcher(inputLine);
				if (matcher.find()) {
					currentLineCount = translateConditionalStmt(tabCount, scanner, inputLine, symbolTable, output,
							currentLineCount, args);
				} else {
					// Loops 
					pattern = Pattern.compile("while(.*):");
					matcher = pattern.matcher(inputLine);
					if (matcher.find()) {
						currentLineCount = translateLoops(tabCount, scanner, inputLine, symbolTable, output,
								currentLineCount, args);
					} else {
						// Print statement
						pattern = Pattern.compile("speak(.*);");
						matcher = pattern.matcher(inputLine);
						if (matcher.find()) {
							translatePrintStmt(inputLine, output);
							currentLineCount += 1;
						} else {
							// Variable declaration
							pattern = Pattern.compile("(let|const|var) .*;");
							matcher = pattern.matcher(inputLine);
							if (matcher.find()) {
								translateDeclarationStmt(inputLine, tabCount, output, symbolTable,args);
								currentLineCount += 1;
							} else {
								// Variable initialization
								pattern = Pattern.compile(".*=.*;");
								matcher = pattern.matcher(inputLine);
								if (matcher.find()) {
									translateInitializeStmt(inputLine, tabCount, output, symbolTable,args);
									currentLineCount += 1;
								} else {
									// Return statement
									pattern = Pattern.compile("return .*;");
									matcher = pattern.matcher(inputLine);
									if (matcher.find()) {
										translateReturnStmt(inputLine, output);
										currentLineCount += 1;
									} else {
										// Whitespace or blank lines
										pattern = Pattern.compile("[ \\t]*");
										matcher = pattern.matcher(inputLine);
										if (matcher.find()) {
											currentLineCount += 1;
										} else {
											throw new ParseException("Uncrecognized statement: " + inputLine, 0);
										}
									}
								}

							}
						}
					}
				}
			}
		}
		return currentLineCount;
	}

	/*
	 * Writes the contents of the output ArrayList, which contains translated lines
	 * of Java code, to a Java file that can then be compiled and ran.
	 */
	public static void writeOutput(List<String> output, String[] args) {
		// For now, we'll just print the output to check if its
		// correct then worry about actually writing to a file later
		cleanOutput(output);
		try {
		      FileWriter myWriter = new FileWriter(args[1]);
		      String heading = args[1];
		      heading = heading.replaceAll(".java", "");
		      myWriter.write("public class " + heading + "{\n");
		      myWriter.write("\tpublic static void main(String[] args)" + "{\n");
		      myWriter.write("\t\tprimary(args);\n\t}\n");
		      for (String translatedLine : output) {
		    	  myWriter.write("\t" + translatedLine + "\n");
				}
		      myWriter.write("}");
		      myWriter.close();
		      System.out.println("Successfully wrote to the file.");
		    } catch (IOException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
	}

	/*
	 * Takes our output and changes key words to match our language grammar.
	 */
	public static void cleanOutput(List<String> output) {
		System.out.println("Changing keywords...");
		int i = 0;
		while (i < output.size()) {
			String temp;
			if (output.get(i).contains("AND")) {
				temp = output.get(i).replace(" AND ", " && ");
				output.set(i, temp);
			}
			if (output.get(i).contains("OR")) {
				temp = output.get(i).replace(" OR ", " || ");
				output.set(i, temp);
			}
			if (output.get(i).contains("NOT")) {
				temp = output.get(i).replace(" NOT ", "!");
				output.set(i, temp);
			}
			if (output.get(i).contains(" on;")) {
				temp = output.get(i).replace(" on;", " true;");
				output.set(i, temp);
			}
			if (output.get(i).contains(" off;")) {
				temp = output.get(i).replace(" off;", " false;");
				output.set(i, temp);
			}
			if (output.get(i).contains(" on ")) {
				temp = output.get(i).replace(" on ", " true ");
				output.set(i, temp);
			}
			if (output.get(i).contains(" off ")) {
				temp = output.get(i).replace(" off ", "false ");
				output.set(i, temp);
			}
			if (output.get(i).contains("on,")) {
				temp = output.get(i).replace("on,", "true,");
				output.set(i, temp);
			}
			if (output.get(i).contains("off,")) {
				temp = output.get(i).replace("off,", "false,");
				output.set(i, temp);
			}
			if (output.get(i).contains("(on)") || output.get(i).contains(" on)") || output.get(i).contains("(on ")) {
				temp = output.get(i).replace("on", "true");
				output.set(i, temp);
			}
			if (output.get(i).contains("(off)") || output.get(i).contains(" off)") || output.get(i).contains("(off ")) {
				temp = output.get(i).replace("off", "false");
				output.set(i, temp);
			}
			i++;

		}
		System.out.println("Changing keywords...DONE!");
	}

	/*
	 * Checks that a filename was passed in the command line, tries to open the file
	 * and create a scanner object to read it, then returns the scanner if
	 * successful. If unable to do either of these, return null and print the error
	 * that occurred.
	 */
	public static Scanner readFileFromCommandLine(String[] args) {
		if (args.length > 0) {
			try {
				File input = new File(args[0]);
				Scanner sc = new Scanner(input);
				return sc;
			} catch (Exception e) {
				System.out.println("Failed to read file: " + e);
			}
		} else {
			System.out.println("Input file not provided in command line args");
		}
		return null;
	}
}