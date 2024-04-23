public class output{
	public static void main(String[] args){
		primary(args);
	}
<<<<<<< HEAD
<<<<<<< HEAD
	public static void primary(){
		boolean bval1 = true;
		boolean bval2 = false;
		boolean bval3 = bval1 || bval2;
		System.out.println("bval3 = " + bval3);
		boolean bval4 = bval3 && bval2;
		System.out.println("bval4 = " + bval4);
		boolean bval5 =!bval4; 
		System.out.println("bval5 = " + bval5);
=======
	public static int primary(String x, int y, boolean z){
=======
	public static int primary(String[] args){
>>>>>>> 4edc0d3 (Added support for command line args)
		return args[0];
>>>>>>> 000a9dd (Added command line support)
	}
}