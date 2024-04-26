public class Program1{
	public static void main(String[] args){
		primary(args);
	}
	public static int gcd(int num1, int num2){
		if(num2==0){
			return num1;
		}
		return gcd(num2, num1%num2);
	}
	public static int lcm(int num1, int num2){
		int myGCD = gcd(num1, num2);
		int product = num1*num2;
		return product/myGCD;
	}
	public static void primary(String[] args){
		int var1 = Integer.parseInt( "12");
		int var2 = Integer.parseInt( "20");
		int difference = 0;
		if(var1 > var2){
			System.out.println("MAX: " + var1);
			difference = var1 - var2;
		}
		else{
			System.out.println("MAX: " + var2);
			difference = var2 - var1;
		}
		System.out.println("SUM: " + (var1+var2));
		System.out.println("DIFFERENCE: " + difference);
		System.out.println("PRODUCT: " + (var1 * var2));
		System.out.println("GCD: " + gcd(var1, var2));
		System.out.println("LCM: " + lcm(var1, var2));
	}
}