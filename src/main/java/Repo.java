import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Repo {
    private String name;

    public Repo() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
