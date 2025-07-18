package br.art.cinex.enviacortesia.domain;
import java.time.Instant;

import lombok.Data;

@Data
public class User {

	private Integer id;
	private String username;
    private String login;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private boolean activated = false;
    private String langKey;
    private String imageUrl;
    private String activationKey;
    private String resetKey;
    private Instant resetDate = null;
}