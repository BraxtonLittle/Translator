public class output{
	public static void main(String[] args){
		primary();
	}
	public static int countTo(int val){
		int i = 0;
		while(i < val){
			i = i+1;
		}
		return i;
	}
	public static void primary(){
		int check = countTo(7);
		if(check == 7){
			System.out.println("values are the same, hooray!");
		}
		else{
			System.out.println("cities will burn");
		}
	}
}