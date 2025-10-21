package Model;

public class User {
    private int id;
    private String name;
    private String gender;
    private String email;
    private String contact;
    private String address;
    private String avatarPath; // nếu có ảnh đại diện

    public User(int id, String name, String gender, String email, String contact, String address, String avatarPath) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.email = email;
        this.contact = contact;
        this.address = address;
        this.avatarPath = avatarPath;
    }

    // Getter & Setter
    public int getId() { return id; }

    public String getName() { return name; }

    public String getGender() { return gender; }

    public String getEmail() { return email; }

    public String getContact() { return contact; }

    public String getAddress() { return address; }

    public String getAvatarPath() { return avatarPath; }
    public String getDisplayId() {
        return "2023" + id;
    }
}
