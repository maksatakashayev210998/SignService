package kz.npck.jws.model;

public class SignResponse {

    private String jws;
    private String kid;
    private String x5tS256;

    public SignResponse() {
    }

    public SignResponse(String jws, String kid, String x5tS256) {
        this.jws = jws;
        this.kid = kid;
        this.x5tS256 = x5tS256;
    }

    public String getJws() {
        return jws;
    }

    public void setJws(String jws) {
        this.jws = jws;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getX5tS256() {
        return x5tS256;
    }

    public void setX5tS256(String x5tS256) {
        this.x5tS256 = x5tS256;
    }
}

