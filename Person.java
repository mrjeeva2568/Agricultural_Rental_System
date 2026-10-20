// Abstraction + inheritance: Farmer and Owner share these fields
public abstract class Person {
    protected final int id;
    protected final String name;
    protected final String email;

    public Person(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public int getId()       { return id; }
    public String getName()  { return name; }
    public String getEmail() { return email; }

    public abstract String getRole();
}
