package be.ap.edu.mobileexamen;

/**
 * Created by Media Markt on 19/11/2015.
 */
public class Bibliotheek {
    private String naam;
    private String point_lat;
    private String point_lng;
    private String grondopp;

    public Bibliotheek(String naam, String point_lat, String point_lng, String grondopp) {
        this.naam = naam;
        this.point_lat = point_lat;
        this.point_lng = point_lng;
        this.grondopp = grondopp;
    }

    public String getNaam() {
        return naam;
    }

    public String getPoint_lat() { return point_lat; }

    public String getPoint_lng() { return point_lng; }

    public String getGrondOpp() {
        return grondopp;
    }

}
