package br.art.cinex.enviacortesia.domain.dto;

 

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A DTO for the {@link br.com.interglobal.cinexapi.domain.Cadastropromocao} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CadastropromocaoDTO implements Serializable {

    private Long id;

    private String nome;

    private String fone;

    private String cpf;

    private ZonedDateTime dataCadastro;

    @NotNull
    private Boolean cupomEnviado;

    @NotNull
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getFone() {
        return fone;
    }

    public void setFone(String fone) {
        this.fone = fone;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public ZonedDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(ZonedDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Boolean getCupomEnviado() {
        return cupomEnviado;
    }

    public void setCupomEnviado(Boolean cupomEnviado) {
        this.cupomEnviado = cupomEnviado;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CadastropromocaoDTO)) {
            return false;
        }

        CadastropromocaoDTO cadastropromocaoDTO = (CadastropromocaoDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, cadastropromocaoDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CadastropromocaoDTO{" +
            "id=" + getId() +
            ", nome='" + getNome() + "'" +
            ", fone='" + getFone() + "'" +
            ", cpf='" + getCpf() + "'" +
            ", dataCadastro='" + getDataCadastro() + "'" +
            ", cupomEnviado='" + getCupomEnviado() + "'" +
            ", email='" + getEmail() + "'" +
            "}";
    }
}
