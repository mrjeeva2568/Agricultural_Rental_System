public class Owner extends Person {
    public Owner(int id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public String getRole() { return "OWNER"; }
}
