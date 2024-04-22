import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * General project notes:
 * 
 * 
 * One of our additional features can check if const variables get re-initialized,
 * and if a let/var variable is undefined/used outside of its scope. i.e. it gets defined within
 * a conditional block and is used outside of the block, thus it would be undefined.
 * 
 * TODO: need to translate let and var and const keywords into String, Integer and boolean.
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
			Map<String, String> symbolTable = new HashMap<>();
			try {
				translate(0, sc, symbolTable, output, 1);
				// Once we've read through the entire file, write output
				writeOutput(output,args);
			} catch (ParseException e) {
				System.out.println(e.getMessage());
			}
			sc.close();
		}
	}

	public static void translateDeclarationStmt(String line, Integer tabCount, List<String> output,
			Map<String, String> symbolTable) throws ParseException {
		System.out.println("Translating declaration statement...");
		String varName = line.substring(line.indexOf(" ")).replaceAll(";", "");
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
		if (symbolTable.containsKey(varName)) {
			throw new ParseException("ERROR: A variable with that name already exists in this scope", 0);
		}

		// If declaring and initializing, store type in symbol table, otherwise if just
		// declaring variable then store its type as null in symbol table
		String returnType;
		if (line.contains("=")) {
			String returnedValue = line.substring(line.indexOf("=") + 1).replaceAll(";", "");
			returnType = getReturnType(tabCount, returnedValue, symbolTable);
			symbolTable.put(varName, returnType);
		} else {
			symbolTable.put(varName, "null");
			returnType = "";
		}

		// Remove scope variable before writing to output
		// line = line.substring(line.indexOf(" ")+1);
		// line = "\t".repeat(tabCount) + line;
		String transLine = ("\t".repeat(tabCount)) +returnType + " " + varName + " " + rightSide;
		output.add(transLine);
		System.out.println("declaration...DONE!");
	}

	public static void translateInitializeStmt(String line, Integer tabCount, List<String> output,
			Map<String, String> symbolTable) throws ParseException {
		System.out.println("Parsing variable initalization...");
		// System.out.println("Symbol table: " + symbolTable);
		// Check if initialized variable has been declared
		String varName = line.substring(0, line.indexOf("=")).strip();
		// TODO: iteratively check for other variables defined in larger scope by
		// iterating down to tabCount 0
		if (!symbolTable.containsKey(varName + tabCount)) {
			throw new ParseException(
					"ERROR: Variable " + varName + " has not been declared. Use the const|let|var keywords", 0);
		}
		// Wait to append tabCount to end of variable name in case we need to print its
		// name in above case for ParseException
		varName += tabCount;
		// Check type assignment and make sure we're not reassigning a variable to a
		// different type, unless we're assigning
		// a null variable to have an actual value
		String returnedValue = line.substring(line.indexOf("=") + 1).replaceAll(";", "");
		String newlyInitializedType = getReturnType(tabCount, returnedValue, symbolTable);
		String originallyInitializedType = symbolTable.get(varName);
		if (originallyInitializedType != "null" && originallyInitializedType != newlyInitializedType) {
			throw new ParseException("ERROR: You cannot assign " + varName + " to be of type " + newlyInitializedType
					+ " because it was declared as a " + symbolTable.get(varName), 0);
		} else {
			symbolTable.put(varName, newlyInitializedType);
		}
		System.out.println("intitalization...DONE!");
		output.add(line);
	}

	public static void translatePrintStmt(String line, List<String> output) {
		System.out.println("Translating speak statement...");
		line = line.replace("speak", "System.out.print");
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
	public static String getReturnType(Integer tabCount, String inputLine, Map<String, String> symbolTable)
			throws ParseException {
		String variableToSearch = inputLine.strip();
		// If we're returning a function, we want to lookup only the name in our
		// symbol table without the parenthesis
		Pattern pattern = Pattern.compile("[a-zA-Z]*\\(.*\\)");
		Matcher matcher = pattern.matcher(variableToSearch);
		if (matcher.find()) {
			variableToSearch = variableToSearch.substring(0, variableToSearch.indexOf("(")).strip();
			if (symbolTable.containsKey(variableToSearch)) {
				return symbolTable.get(variableToSearch);
			} else {
				// Function definition wasn't stored in symbol table, meaning function
				// hasn't been defined yet
				throw new ParseException(
						"ERROR: The function " + variableToSearch + " has not been defined before being used", 0);
			}
		}

		// Not returning a function, check primitive values

		// Check if the value is an integer. "-?" searches for an optional dash to
		// handle
		// negative integers, and "\d+" searches for one or more digits
		pattern = Pattern.compile("-?[0-9]+");
		matcher = pattern.matcher(variableToSearch);
		if (matcher.find()) {
			return "int";
		}

		if (inputLine.contains("on") || inputLine.contains("off")) {
			return "boolean";
		}

		String[] comparisonOps = { ">", "<", ">=", "<=", "==" };
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

		if (inputLine.contains("\"")) {
			return "String";
		}

		// If no return type found, print error
		throw new ParseException("ERROR: A return type could not be found for: " + inputLine, 0);
	}

	/*
	 * This function uses a subScanner to "jump ahead" in the file and determine the
	 * function's return type first so that it can correctly translate the function
	 * declaration line before translating the body normally by calling the
	 * translate() function with an increased tabCount value to indicate scope.
	 * 
	 */
	public static int translateFunction(Integer tabCount, Scanner scanner, Map<String, String> symbolTable, String line,
			List<String> output, int lineCount) throws ParseException {
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
		System.out.println("funciton head...DONE!");	
		functionName += tabCount;
		System.out.println("Retriving return statment...");	
		// Use the subScanner to retrieve the return statement line
		String foundLine = "";
		Matcher matcher;
		int subscannerLineCount = 1;
		Scanner subScanner;
		try {
			subScanner = new Scanner(new File("input.txt"));
		} catch (FileNotFoundException e) {
			throw new ParseException("Unable to open input.txt for using sub-scanner", 0);
		}
		while (subScanner.hasNextLine()) {
			String inputLine = subScanner.nextLine();
			if (subscannerLineCount >= currentLineCount + 1) {
				int readTabCount = countTabs(inputLine);
				// If tab counts mismatch, we've broken out of scope without finding
				// a return statement
				if (readTabCount - 1 != tabCount) {
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

		// Interpret the return type from the return statement line, or void
		// if no return statement was found in this scope
		String returnType;
		if (foundLine.length() > 0) {
			foundLine = foundLine.strip().substring(foundLine.indexOf(" ")).replaceAll(";", "");
			returnType = getReturnType(tabCount, foundLine, symbolTable);
		} else {
			// No return line, function type is void
			returnType = "void";
		}
		symbolTable.put(functionName, returnType);
		String funNameWithParams = line.substring(line.indexOf(" ") + 1, line.length() - 1);
		output.add("public static " + returnType + " " + funNameWithParams + "{");
		System.out.println("return statement...DONE!");	
		currentLineCount++;
		lineCount++;
		Scanner sub;
		System.out.println("Translatng function body....");
		try {
			File input = new File("input.txt");
			sub = new Scanner(input);
			for (int i = 1; i < currentLineCount; i++) {
				sub.nextLine();
			}
			currentLineCount = translate(tabCount + 1, sub, symbolTable, output, currentLineCount);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), 0);
		}
		int dif = currentLineCount - lineCount;
		while (dif > 0) {
			scanner.nextLine();
			dif--;
		}
		System.out.println("function body...DONE!");	

		// TODO: Remove variables from symbol table defined in local function scope

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
			Map<String, String> symbolTable, List<String> output, int lineCount) throws ParseException {
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
			File input = new File("input.txt");
			Scanner sub = new Scanner(input);
			for (int i = 1; i < currentLineCount; i++) {
				sub.nextLine();
			}
			currentLineCount = translate(tabCount + 1, sub, symbolTable, output, currentLineCount);
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
	public static int translateLoops(Integer tabCount, Scanner scanner, String line, Map<String, String> symbolTable,
			List<String> output, int lineCount) throws ParseException {
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
			File input = new File("input.txt");
			Scanner sub = new Scanner(input);
			for (int i = 1; i < currentLineCount; i++) {
				sub.nextLine();
			}
			currentLineCount = translate(tabCount + 1, sub, symbolTable, output, currentLineCount);
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
	public static int translate(Integer tabCount, Scanner scanner, Map<String, String> symbolTable, List<String> output,
			int lineCount) throws ParseException {
		String x = "";
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
						currentLineCount);
			} else {
				// Conditional statement
				pattern = Pattern.compile("(if|elif|else)(.*):");
				matcher = pattern.matcher(inputLine);
				if (matcher.find()) {
					currentLineCount = translateConditionalStmt(tabCount, scanner, inputLine, symbolTable, output,
							currentLineCount);
				} else {
					// Loops fn
					pattern = Pattern.compile("while(.*):");
					matcher = pattern.matcher(inputLine);
					if (matcher.find()) {
						currentLineCount = translateConditionalStmt(tabCount, scanner, inputLine, symbolTable, output,
								currentLineCount);
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
								translateDeclarationStmt(inputLine, tabCount, output, symbolTable);
								currentLineCount += 1;
							} else {
								// Variable initialization
								pattern = Pattern.compile(".*=.*;");
								matcher = pattern.matcher(inputLine);
								if (matcher.find()) {
									translateInitializeStmt(inputLine, tabCount, output, symbolTable);
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
	public static void writeOutput(List<String> output,String[] args) {
		// For now, we'll just print the output to check if its
		// correct then worry about actually writing to a file later
		cleanOutput(output);
		try {
		      FileWriter myWriter = new FileWriter(args[1]);
		      String heading = args[1];
		      heading = heading.replaceAll(".java", "");
		      myWriter.write("public class " + heading + "{" + "\n");
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
	 * Takes our output and changes key words to match
	 * our language grammar.
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