import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
 * We need to figure out how to handle nesting for the bodies of functions
 * and conditional statements. Since our language is indentation-based, I'm
 * thinking our translate function can have a numerical flag indicating
 * the number of tab characters we're on, representing scope in a way. Then
 * when we reach a line that's smaller then our current tab count (i.e we
 * WERE inside of a function so each line started with 1 tab, but now we've
 * broken out of that so each line only starts with 0 tabs) we break out of
 * the while loop. I've added the tabCount param just to have it there but
 * I don't have any logic for it yet.
 */

public class Translator {
	
	/*
	 * Saves scanner reading from text tile, creates an output
	 * Arraylist that will later be written to a file, and calls
	 * translate helper function.
	 */
	public static void main(String[] args) {
		Scanner sc = readFileFromCommandLine(args);
		List<String> output = new ArrayList<String>();
		
		if(sc!=null) {
			translate(0, sc, output);
		}
	}
	
	public static void translateDeclarationStmt(String line, List<String> output) {
		// Remove scope variable
		line = line.substring(line.indexOf(" ")+1);
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
	
	public static void translateFunction(Integer tabCount, Scanner scanner, String line, List<String> output) {
		// Translate line parameter first and add it to output, then handle
		// nested function body by calling translate with additional tabCount
		System.out.println("------------------");
		//translate(tabCount+1, scanner, output);
		System.out.println("------------------");
	}
	
	public static void translateConditionalStmt(Integer tabCount, Scanner scanner, String line, List<String> output) {
		// Translate line parameter first and add it to output, then handle
		// nested function body by calling translate with additional tabCount 
		//translate(tabCount+1, scanner, output);
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
	public static void translate(Integer tabCount, Scanner scanner, List<String> output)
	{
		while(scanner.hasNextLine()) {
			String inputLine = scanner.nextLine();
			
			int readTabCount = countTabs(inputLine);
			if(tabCount!=readTabCount) {
				break;
			}
			inputLine = inputLine.replaceAll("\t", "");
			
			// Function declaration
			Pattern pattern = Pattern.compile("fun .*(.*):");
		    Matcher matcher = pattern.matcher(inputLine);
		    if(matcher.find()) {
		    	translateFunction(tabCount, scanner, inputLine, output);
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
				    		translateDeclarationStmt(inputLine, output);
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
		// Once we've read through the entire file, write output
		writeOutput(output);
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