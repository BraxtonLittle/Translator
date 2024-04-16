import java.io.File;
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
 * I think we'll need to keep track of initialized variables and their
 * types so that when they get passed as parameters in functions we can
 * translate them to Java easily.
 * 
 * One of our additional features can check if const variables get re-initialized,
 * and if a let/var variable is undefined/used outside of its scope. i.e. it gets defined within
 * a conditional block and is used outside of the block, thus it would be undefined.
 * 
 * We might be able to do a type checking system if we're already storing types in a symbol table,
 * and prohibit duplicate functions with the same name
 * 
 * Lastly we can add a feature that explicity shows the parsing process
 * 
 */

public class Translator {
	
	/*
	 * Saves scanner reading from text tile, creates an output
	 * Arraylist that will later be written to a file, and calls
	 * translate helper function.
	 */
	public static void main(String[] args) {
		Scanner sc = readFileFromCommandLine(args);
		Scanner subScanner = readFileFromCommandLine(args);
		List<String> output = new ArrayList<String>();
		if(sc!=null) {
			Map<String, List> symbolTable = new HashMap<>();
			translate(0, sc, subScanner, symbolTable, output);
			sc.close();
		}
		// Once we've read through the entire file, write output
		writeOutput(output);
	}
	
	public static void translateDeclarationStmt(String line, Integer tabCount, List<String> output) {
		// Remove scope variable
		line = line.substring(line.indexOf(" ")+1);
		line = "\t".repeat(tabCount) + line;
		output.add(line);
	}
	
	public static void translateInitializeStmt(String line, List<String> output) {
		// Initialize statements don't need to be translated
		output.add(line);
	}
	
	public static void translatePrintStmt(String line, List<String> output) {
		line = line.replace("speak", "System.out.print");
		output.add(line);
	}
	
	public static void translateReturnStmt(String line, List<String> output) {
		// Return statements don't need to be translated
		output.add(line);
		
	}
	
	/*
	 * This function uses the return statement to determine a function's
	 * return type, which will be used when translating the function declaration
	 * line. This checks for which primitive operators are present in the return
	 * statement in the necessary order (i.e. putting arithmetic before comparison
	 * would be wrong because "return (3+4)<9" would be considered arithmetic when
	 * its actually a comparison statement. If we're not returning a primitive
	 * or string, we're returning a variable of sorts so use the return type of that
	 * variable stored in the lookup table.
	 */
	public static String getReturnType(Integer tabCount, String inputLine, Map<String, List> symbolTable) {
		String variableToSearch = inputLine.substring(inputLine.indexOf("return"));
		variableToSearch = variableToSearch.strip();
		
		// If we're returning a function or variable, we want to lookup only the name in our
		// symbol table without the parenthesis
		Pattern pattern = Pattern.compile(".*(.*);");
	    Matcher matcher = pattern.matcher(variableToSearch);
	    if(matcher.find()) {
	    	variableToSearch = variableToSearch.substring(variableToSearch.indexOf("("));
	    }
		variableToSearch+=tabCount;
		System.out.println("Searching for variable: " + variableToSearch);
		
		
		// Not returning a function, check primitive values
		String[] comparisonOps = {">", "<", ">=", "<=", "=="};
		String[] arithmeticOps = {"+", "-", "/", "%"};
		for(String comparisonOp : comparisonOps) {
			if(inputLine.contains(comparisonOp)) {
				return "boolean";
			}
		}
		
		for(String arithmeticOp : arithmeticOps) {
			if(inputLine.contains(arithmeticOp)) {
				return "Integer";
			}
		}
		
		if(inputLine.contains("\"")) {
			return "String";
		}
		
		// If no return type found, print error and return void
		System.out.println("ERROR: A return type could not be found for: " + inputLine);
		return "void";
	}
	
	/*
	 * This function uses a subScanner to "jump ahead" in the file and determine
	 * the function's return type first so that it can correctly translate
	 * the function declaration line before translating the body normally by calling
	 * the translate() function with an increased tabCount value to indicate scope.
	 * 
	 */
	public static void translateFunction(Integer tabCount, Scanner scanner, Scanner subScanner, Map<String, List> symbolTable, String line, List<String> output) {
		
		// Shows an error if a function exists with same name and return type, mostly because
		// this isn't valid in Java but also because it'll cause a bug in our implementation below
		String functionName = line.substring(line.indexOf(" "), line.indexOf("("));
		functionName = functionName.strip();
		if(symbolTable.containsKey(functionName)) {
			// Check if return type is the same, same function names are allowed but
			// their return type has to be different. This has to be done with lineCount
			// instead of checking the subScanner's line against the main scanner
			System.out.println("ERROR: Duplicate function name " + functionName + " detected");
			return;
		}
		
		// Use the subScanner to retrieve the return statement line
		String foundLine = "";
    	Matcher matcher;
		while(subScanner.hasNextLine()) {
			String inputLine = subScanner.nextLine();
			int readTabCount = countTabs(inputLine);
			// If tab counts mismatch, we've broken out of scope without finding
			// a return statement
			if(readTabCount!=tabCount) {
				break;
			}
			String regex = "\t".repeat(readTabCount) + "return .*;";
			Pattern pattern = Pattern.compile(regex);
			matcher = pattern.matcher(inputLine);
			if(matcher.find()) {
				foundLine = inputLine;
				break;
			}
		}
		
		// Reset the subScanner for future use in nested bodies
		subScanner.reset();
		
		// Interpret the return type from the return statement line, or void
		// if no return statement was found in this scope
		String returnType;
		if(foundLine.length()>0) {
			System.out.println("Found line: " + foundLine);
			returnType = getReturnType(tabCount, foundLine.strip(), symbolTable);
		}
		else {
			// No return line, function type is void
			returnType = "void";
		}
		
		output.add("public static " + returnType + " " + functionName + "\n{");
		
		translate(tabCount+1, scanner, subScanner, symbolTable, output);
		
		//Add a closing bracket to signify the end of the function
		output.add("}");
	}
	
	public static void translateConditionalStmt(Integer tabCount, Scanner scanner, String line, List<String> output) {
		// Translate line parameter first and add it to output, then handle
		// nested function body by calling translate with additional tabCount 
		// translate(tabCount+1, scanner, output);
	}
	
	public static int countTabs(String inputLine) {
		for(int i = 0; i<inputLine.length(); i++) {
			if(inputLine.charAt(i)!='\t') {
				return i;
			}
		}
		System.out.println("Error reading tab count");
		return -1;
	}
	
	/*
	 * Parses through each line of the input file and uses regular
	 * expressions to call the matching helper function for translation.
	 * Multi-line statements such as if statements and functions will
	 * call scanner.nextLine in their own helper methods and return here
	 * to continue parsing through the rest of the input file afterwards.
	 */
	public static void translate(Integer tabCount, Scanner scanner, Scanner subScanner, Map<String, List> symbolTable, List<String> output)
	{
		while(scanner.hasNextLine()) {
			String inputLine = scanner.nextLine();
			
			int readTabCount = countTabs(inputLine);
			if(tabCount!=readTabCount) {
				break;
			}
			//inputLine = inputLine.replaceAll("\t", "");
			
			// Function declaration
			Pattern pattern = Pattern.compile("fun .*(.*):");
		    Matcher matcher = pattern.matcher(inputLine);
		    if(matcher.find()) {
		    	translateFunction(tabCount, scanner, subScanner, symbolTable, inputLine, output);
		    }
		    else {
		    	// Conditional statement
		    	pattern = Pattern.compile("if(.*):");
		    	matcher = pattern.matcher(inputLine);
		    	if(matcher.find()) {
		    		translateConditionalStmt(tabCount, scanner, inputLine, output);
		    	}
		    	else {
		    		// Print statement
			    	pattern = Pattern.compile("speak(.*);");
			    	matcher = pattern.matcher(inputLine);
			    	if(matcher.find()) {
			    		translatePrintStmt(inputLine, output);
			    	}
			    	else {
			    		// Variable declaration
				    	pattern = Pattern.compile("(let|const|var) .*;");
				    	matcher = pattern.matcher(inputLine);
				    	if(matcher.find()) {
				    		translateDeclarationStmt(inputLine, tabCount, output);
				    	}
				    	else {
				    		// Variable initialization
					    	pattern = Pattern.compile(".*=.*;");
					    	matcher = pattern.matcher(inputLine);
					    	if(matcher.find()) {
					    		translateInitializeStmt(inputLine, output);
					    	}
					    	else {
					    		// Return statement
						    	pattern = Pattern.compile("return .*;");
						    	matcher = pattern.matcher(inputLine);
						    	if(matcher.find()) {
						    		translateReturnStmt(inputLine, output);
						    	}
						    	else {
						    		System.out.println("Unrecognized statement: " + inputLine);
						    	}
					    	}
					    	
				    	}
			    	}
		    	}
		    }
		}
	}
	
	/*
	 * Writes the contents of the output ArrayList, which contains
	 * translated lines of Java code, to a Java file that can then
	 * be compiled and ran.
	 */
	public static void writeOutput(List<String> output) {
		// For now, we'll just print the output to check if its
		// correct then worry about actually writing to a file later
		for(String translatedLine : output) {
			System.out.println(translatedLine);
		}
	}
	
	/*
	 * Checks that a filename was passed in the command line, tries
	 * to open the file and create a scanner object to read it, then
	 * returns the scanner if successful. If unable to do either of
	 * these, return null and print the error that occurred.
	 */
	public static Scanner readFileFromCommandLine(String[] args) {
		if(args.length>0) {
			try {
				File input = new File("input.txt");
				Scanner sc = new Scanner(input);
				return sc;
			}
			catch(Exception e) {
				System.out.println("Failed to read file: " + e);
			}
		}
		else {
			System.out.println("Input file not provided in command line args");
		}
		return null;
	}
}