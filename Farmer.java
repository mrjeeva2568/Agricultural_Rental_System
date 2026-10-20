public class Farmer extends Person {
    private final String village;

    public Farmer(int id, String name, String email, String village) {
        super(id, name, email);
        this.village = village;
    }

    public String getVillage() { return village; }

    @Override
    public String getRole() { return "FARMER"; }
}
