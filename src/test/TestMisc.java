package test;

public class TestMisc {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String s = "<lsdflsdf>phoenix_api_GetIt(xxx)</dfdfds>";
		System.out.println(s.replaceAll("phoenix_api_GetIt\\s*\\(","phoenix_good_GetIt("));
	}

}
