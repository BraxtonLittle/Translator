public class Program2{
	public static void main(String[] args){
		primary(args);
	}
	public static int printHelper(int i){
		int x = 0;
		while(x<i){
			System.out.println("*");
			x = x+1;
		}
		return 0;
	}
	public static int getSum(int a, int b, int m){
		int total = 0;
		int i = 0;
		while(i<m){
			if((i%a==0) || (i%b==0)){
				total = total + i;
			}
			i = i+1;
		}
		return total;
	}
	public static void primary(String[] args){
		int a = Integer.parseInt( "6");
		int b = Integer.parseInt( "10");
		int m = Integer.parseInt( "1450");
		int i = a;
		while(i<=b){
			int x = printHelper(i);
			System.out.println("");
			i = i +1;
		}
		int sum = getSum(a,b,m);
		System.out.println(sum);
	}
}