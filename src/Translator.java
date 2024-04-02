import java.io.File;
import java.util.Scanner;

public class Translator {
	public static void main(String[] args) {
		Scanner sc = readFileFromCommandLine(args);
		if(sc!=null) {
			while(sc.hasNextLine()) {
				System.out.println(sc.nextLine());
			}
		}
	}
	
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