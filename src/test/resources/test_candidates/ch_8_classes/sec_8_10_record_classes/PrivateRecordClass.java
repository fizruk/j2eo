public class PrivateRecordClass {
	public static void main(String[] args) {
		Person p = new Person("Mike", "Innopolis");
		System.out.println(p.name());
		System.out.println(p.address());
		System.out.println("passed");
	}
}
private record Person (String name, String address) {}
